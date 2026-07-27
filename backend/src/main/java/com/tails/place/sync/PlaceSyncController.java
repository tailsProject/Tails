package com.tails.place.sync;

import com.tails.common.response.ApiResponse;
import com.tails.place.sync.dto.CategoryCountSummaryResponse;
import com.tails.place.sync.dto.PlacePreviewItem;
import com.tails.place.sync.dto.SyncRunResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TourAPI 연동 확인/동기화용 엔드포인트. PlaceController(저장된 Place 조회)와 관심사가 달라서 경로를 /api/places/sync로 분리
@RestController
@RequestMapping("/api/places/sync")
@RequiredArgsConstructor
@Tag(name = "PlaceSync", description = "TourAPI(한국관광공사 반려동물 동반여행 서비스) 연동 - 확인/동기화 도구")
public class PlaceSyncController {

    private final PlaceSyncService placeSyncService;

    @GetMapping("/category-counts")
    @Operation(
            summary = "카테고리별 TourAPI 전체 건수 확인",
            description = "7개 contentTypeId(관광지/문화시설/행사공연축제/레포츠/숙박/쇼핑/음식점) 각각의 totalCount를 조회해서, "
                    + "카테고리별 건수 + 전체 합계 + 하루 1000건 처리 가정 시 예상 소요일수를 반환합니다. 지역 필터 없이 전국 대상입니다.")
    public ApiResponse<CategoryCountSummaryResponse> getCategoryCounts() {
        return ApiResponse.success(placeSyncService.getCategoryCounts());
    }

    // numOfRows 기본값 5: 항목마다 detailPetTour2가 추가로 호출되므로 확인용으로 작게 잡음
    @GetMapping("/preview")
    @Operation(
            summary = "카테고리 미리보기 (목록 + 상세 합침, 저장 안 함)",
            description = "contentTypeId 카테고리의 목록을 조회하고, 각 항목의 contentid로 detailPetTour2를 호출해서 "
                    + "상세정보를 합친 원본 데이터를 그대로 반환합니다. 이미 DB에 저장된 장소는 상세 조회를 생략하고 "
                    + "alreadyExists=true로만 표시합니다. Place 엔티티 변환/저장은 하지 않습니다.")
    public ApiResponse<List<PlacePreviewItem>> preview(
            @RequestParam(required = true) String contentTypeId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "5") int numOfRows) {
        PetTourContentType contentType = PetTourContentType.fromCode(contentTypeId);
        return ApiResponse.success(placeSyncService.previewSync(contentType, pageNo, numOfRows));
    }

    // 이미 저장된 항목은 건너뛰므로 재호출해도 중복 저장은 안 되지만, 실제로 DB에 사용
    @PostMapping("/run")
    @Operation(
            summary = "⚠️ 카테고리 동기화 실행 (실제 DB 저장)",
            description = "contentTypeId 카테고리의 목록을 조회하고, 아직 저장되지 않은 항목만 상세정보(detailPetTour2)까지 "
                    + "조회해서 Place 테이블에 실제로 저장합니다. 이미 external_place_id로 저장된 항목은 건너뜁니다. "
                    + "이 API는 조회 전용이 아니라 DB에 실제로 쓰기(INSERT)를 수행합니다.")
    public ApiResponse<SyncRunResponse> run(
            @RequestParam(required = true) String contentTypeId,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "100") int numOfRows) {
        PetTourContentType contentType = PetTourContentType.fromCode(contentTypeId);
        return ApiResponse.success(placeSyncService.syncCategory(contentType, pageNo, numOfRows));
    }
}
