package com.tails.place.sync;

import com.tails.place.Place;
import com.tails.place.PlaceImage;
import com.tails.place.PlaceImageRepository;
import com.tails.place.PlaceRepository;
import com.tails.place.sync.dto.CategoryCountItem;
import com.tails.place.sync.dto.CategoryCountSummaryResponse;
import com.tails.place.sync.dto.PetTourDetailItem;
import com.tails.place.sync.dto.PetTourImageItem;
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

    // data.go.kr 활용신청 키의 실제 하루 호출 한도
    private static final int DAILY_PROCESS_CAPACITY = 1000;

    // 쇼핑(38) 카테고리 안에서 "여행지"라 보기 애매한 하위분류(SH04=개별 상점, SH07=편의점/마트)는
    // 상세/사진 API 호출 전에 걸러서 API 호출 자체를 아낀다. 반려동물 동반여행 서비스 취지상
    // 약국/개별 브랜드 매장/편의점보다는 대형 쇼핑몰·아울렛(SH02)·전통시장(SH06) 위주로 채운다
    private static final String SHOPPING_CONTENT_TYPE = "38";
    private static final List<String> EXCLUDED_SHOPPING_SUBCATEGORIES = List.of("SH04", "SH07");

    private final PetTourApiClient petTourApiClient;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;

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
    public List<PlacePreviewItem> previewSync(PetTourContentType contentType, String areaCode, int pageNo, int numOfRows) {
        List<PetTourListItem> listItems = petTourApiClient.fetchSyncList(contentType.getCode(), areaCode, pageNo, numOfRows);

        return listItems.stream()
                .filter(listItem -> !isExcludedShoppingSubcategory(listItem))
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
    public SyncRunResponse syncCategory(PetTourContentType contentType, String areaCode, int pageNo, int numOfRows) {
        List<PetTourListItem> listItems = petTourApiClient.fetchSyncList(contentType.getCode(), areaCode, pageNo, numOfRows);

        int newlySaved = 0;
        int skippedAlreadyExists = 0;
        int excludedBySubcategory = 0;
        int failed = 0;
        List<String> failedDetails = new ArrayList<>();

        for (PetTourListItem listItem : listItems) {
            try {
                if (isExcludedShoppingSubcategory(listItem)) {
                    excludedBySubcategory++;
                    continue;
                }

                var existing = placeRepository.findByExternalPlaceId(listItem.contentid());
                if (existing.isPresent()) {
                    skippedAlreadyExists++;
                    // 예전에(이 기능 추가 전) 이미 저장된 장소는 사진이 없을 수 있어 보강해준다.
                    // 사진 보강이 실패해도(예: 트래픽 초과) 이미 세어둔 skippedAlreadyExists를
                    // failed로 다시 잘못 세지 않도록 별도로 감싸서 흡수한다
                    trySyncImagesIfMissing(existing.get(), listItem.contentid());
                    continue;
                }

                PetTourDetailItem detail = petTourApiClient.fetchDetail(listItem.contentid());
                Place saved = placeRepository.save(toPlaceEntity(listItem, detail));
                newlySaved++;
                // 장소 저장 자체는 성공했으므로, 사진만 못 받아온 경우 newlySaved를 그대로 인정하고
                // 실패로 잘못 세지 않는다(사진은 다음 sync 때 syncImagesIfMissing이 다시 채워줌)
                trySyncImagesIfMissing(saved, listItem.contentid());
            } catch (Exception exception) {
                failed++;
                failedDetails.add(listItem.contentid() + ": " + exception.getMessage());
            }
        }

        return new SyncRunResponse(
                contentType.getCode(), pageNo, numOfRows, listItems.size(),
                newlySaved, skippedAlreadyExists, excludedBySubcategory, failed, failedDetails);
    }

    private boolean isExcludedShoppingSubcategory(PetTourListItem listItem) {
        return SHOPPING_CONTENT_TYPE.equals(listItem.contenttypeid())
                && EXCLUDED_SHOPPING_SUBCATEGORIES.contains(listItem.lclsSystm2());
    }

    // 사진 API 호출 실패(트래픽 초과 등)가 장소 저장 성공/스킵 집계에 섞이지 않도록 감싼다.
    // 실패해도 장소 자체는 이미 저장/확인된 상태이므로 그냥 넘어가고, 사진은 다음 sync 때 재시도된다
    private void trySyncImagesIfMissing(Place place, String contentId) {
        try {
            syncImagesIfMissing(place, contentId);
        } catch (Exception ignored) {
            // 사진만 못 받아온 것 - 장소 저장 자체의 성공 여부에는 영향 없음
        }
    }

    // 이미 이미지가 저장돼 있으면 다시 안 부름(불필요한 외부 API 호출 절약 + 중복 저장 방지)
    private void syncImagesIfMissing(Place place, String contentId) {
        if (placeImageRepository.existsByPlace_PlaceId(place.getPlaceId())) {
            return;
        }

        List<PetTourImageItem> images = petTourApiClient.fetchImages(contentId);
        int sequence = 0;
        for (PetTourImageItem image : images) {
            if (image.originimgurl() == null || image.originimgurl().isBlank()) {
                continue;
            }
            placeImageRepository.save(PlaceImage.builder()
                    .place(place)
                    .imageUrl(image.originimgurl())
                    .sequence(sequence++)
                    .build());
        }
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

    // 좌표를 모르는 항목은 mapx/mapy가 빈 문자열로 내려옴 - 그대로 parseDouble하면
    // NumberFormatException이라 빈 값은 null로 반환. 드물게 "0"이 오는 경우도 있는데,
    // (0, 0)은 대서양 한가운데라 국내 장소 좌표로는 있을 수 없는 값 - 이 값이 그대로 들어가면
    // 지도에서 전체 범위(bounds) 계산이 지구 반대편까지 걸쳐 깨지므로 마찬가지로 null 처리한다
    private Double parseCoordinate(String value) {
        if (value == null || value.isBlank() || "0".equals(value.trim())) {
            return null;
        }
        return Double.parseDouble(value);
    }

    // 값이 있는 필드만 "라벨: 값" 형태로 줄바꿈해서 이어붙임
    // detail이 null이면(반려동물 동반 상세정보 없는 항목) 빈 문자열 반환
    private String buildPetInfo(PetTourDetailItem detail) {
        if (detail == null) {
            return "";
        }

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
