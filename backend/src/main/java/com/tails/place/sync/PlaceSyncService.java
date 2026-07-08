package com.tails.place.sync;

import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.place.sync.dto.CategoryCountItem;
import com.tails.place.sync.dto.CategoryCountSummaryResponse;
import com.tails.place.sync.dto.PetTourDetailItem;
import com.tails.place.sync.dto.PetTourListItem;
import com.tails.place.sync.dto.PlacePreviewItem;
import com.tails.place.sync.dto.SyncRunResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

// KorPetTourService2 연동 비즈니스 로직: 카테고리별 건수 확인 / 미리보기 / 실제 동기화(저장)
@Service
@RequiredArgsConstructor
public class PlaceSyncService {

    // petInfo 문자열을 만들 때 쓸 라벨-필드 순서. buildPetInfo에서 이 순서 그대로 이어붙임
    private static final List<PetInfoField> PET_INFO_FIELDS = List.of(
            new PetInfoField("동반유형", PetTourDetailItem::acmpyTypeCd),
            new PetInfoField("관련사고대비사항", PetTourDetailItem::relaAcdntRiskMtr),
            new PetInfoField("관련구비시설", PetTourDetailItem::relaPosesFclty),
            new PetInfoField("관련비치품목", PetTourDetailItem::relaFrnshPrdlst),
            new PetInfoField("기타동반정보", PetTourDetailItem::etcAcmpyInfo),
            new PetInfoField("관련구매품목", PetTourDetailItem::relaPurcPrdlst),
            new PetInfoField("동반가능동물", PetTourDetailItem::acmpyPsblCpam),
            new PetInfoField("관련렌탈품목", PetTourDetailItem::relaRntlPrdlst),
            new PetInfoField("동반시필요사항", PetTourDetailItem::acmpyNeedMtr));

    // 예상 소요일수 계산용 가정치(하루 처리 가능 건수) 실측치 아님 — 나중에 조정 가능
    private static final int DAILY_PROCESS_CAPACITY = 900;

    private final PetTourApiClient petTourApiClient;
    private final PlaceRepository placeRepository;

    public CategoryCountSummaryResponse getCategoryCounts() {
        List<CategoryCountItem> categories = Arrays.stream(PetTourContentType.values())
                .map(type -> new CategoryCountItem(
                        type.getCode(), type.getLabel(), petTourApiClient.fetchTotalCount(type.getCode())))
                .toList();

        long totalCount = categories.stream()
                .mapToLong(CategoryCountItem::totalCount)
                .sum();

        // 정수 올림 나눗셈: (a + b - 1) / b
        long estimatedDays = (totalCount + DAILY_PROCESS_CAPACITY - 1) / DAILY_PROCESS_CAPACITY;

        return new CategoryCountSummaryResponse(categories, totalCount, estimatedDays);
    }

    // 이미 저장된 장소는 detailPetTour2 호출을 생략(불필요한 외부 API 호출 절약)
    public List<PlacePreviewItem> previewSync(PetTourContentType contentType, int pageNo, int numOfRows) {
        List<PetTourListItem> listItems = petTourApiClient.fetchSyncList(contentType.getCode(), pageNo, numOfRows);

        return listItems.stream()
                .map(this::toPreviewItem)
                .toList();
    }

    private PlacePreviewItem toPreviewItem(PetTourListItem listItem) {
        boolean alreadyExists = placeRepository.findByExternalPlaceId(listItem.contentid()).isPresent();

        PetTourDetailItem detail = alreadyExists ? null : petTourApiClient.fetchDetail(listItem.contentid());

        return new PlacePreviewItem(listItem, detail, alreadyExists);
    }

    // 메서드 전체에 @Transactional을 걸지 않음 — 걸면 한 항목 실패가 전체 롤백으로
    // 번질 수 있어서, save()마다 개별 트랜잭션으로 커밋되게 두고 항목별 try-catch로 부분 실패를 흡수
    public SyncRunResponse syncCategory(PetTourContentType contentType, int pageNo, int numOfRows) {
        List<PetTourListItem> listItems = petTourApiClient.fetchSyncList(contentType.getCode(), pageNo, numOfRows);

        int newlySaved = 0;
        int skippedAlreadyExists = 0;
        int failed = 0;
        List<String> failedDetails = new ArrayList<>();

        for (PetTourListItem listItem : listItems) {
            try {
                if (placeRepository.findByExternalPlaceId(listItem.contentid()).isPresent()) {
                    skippedAlreadyExists++;
                    continue;
                }

                PetTourDetailItem detail = petTourApiClient.fetchDetail(listItem.contentid());
                placeRepository.save(toPlaceEntity(listItem, detail));
                newlySaved++;
            } catch (Exception exception) {
                failed++;
                failedDetails.add(listItem.contentid() + ": " + exception.getMessage());
            }
        }

        return new SyncRunResponse(
                contentType.getCode(), pageNo, numOfRows, listItems.size(),
                newlySaved, skippedAlreadyExists, failed, failedDetails);
    }

    private Place toPlaceEntity(PetTourListItem listItem, PetTourDetailItem detail) {
        String address = (listItem.addr2() != null && !listItem.addr2().isBlank())
                ? listItem.addr1() + " " + listItem.addr2()
                : listItem.addr1();

        String imageUrl = (listItem.firstimage() != null && !listItem.firstimage().isBlank())
                ? listItem.firstimage()
                : listItem.firstimage2();

        return Place.builder()
                .externalPlaceId(listItem.contentid())
                .contentTypeId(listItem.contenttypeid())
                .placeName(listItem.title())
                .cat1(listItem.lclsSystm1())
                .cat2(listItem.lclsSystm2())
                .cat3(listItem.lclsSystm3())
                .address(address)
                .latitude(parseCoordinate(listItem.mapy()))
                .longitude(parseCoordinate(listItem.mapx()))
                .phone(listItem.tel())
                .imageUrl(imageUrl)
                .petInfo(buildPetInfo(detail))
                .build();
    }

    // 좌표를 모르는 항목은 mapx/mapy가 빈 문자열로 내려옴
    // 그대로 parseDouble하면 NumberFormatException이라 빈 값은 null로 반환
    private Double parseCoordinate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Double.parseDouble(value);
    }

    // 값이 있는 필드만 "라벨: 값" 형태로 줄바꿈해서 이어붙임
    private String buildPetInfo(PetTourDetailItem detail) {
        StringBuilder petInfo = new StringBuilder();

        for (PetInfoField field : PET_INFO_FIELDS) {
            String value = field.valueExtractor().apply(detail);
            if (value != null && !value.isBlank()) {
                if (!petInfo.isEmpty()) {
                    petInfo.append("\n");
                }
                petInfo.append(field.label()).append(": ").append(value);
            }
        }

        return petInfo.toString();
    }

    private record PetInfoField(String label, Function<PetTourDetailItem, String> valueExtractor) {
    }
}
