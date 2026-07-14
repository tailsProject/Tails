package com.tails.place;

import com.tails.common.response.ApiResponse;
import com.tails.place.dto.PlaceResponse;
import com.tails.place.dto.PlaceSearchResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// Place(장소) HTTP API. 조회/필터링 로직은 {@link PlaceService}에 위임
// 응답은 {@code ApiResponse}로 감싸 API 전체 응답 형식을 통일
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
@Tag(name = "Place", description = "장소 조회 / 검색 / 카테고리 필터 API")
public class PlaceController {

    private final PlaceService placeService;

    // 장소 상세 조회 (GET /api/places/{placeId})
    @GetMapping("/{placeId}")
    @Operation(summary = "장소 상세 조회", description = "placeId로 장소 하나의 상세 정보를 조회합니다.")
    public ApiResponse<PlaceResponse> getPlaceDetail(@PathVariable Long placeId) {
        return ApiResponse.success(placeService.getPlaceDetail(placeId));
    }

    // 장소 목록 페이징 조회 (기본값: 20개씩, placeId 오름차순)
    @GetMapping
    @Operation(summary = "장소 목록 조회", description = "장소 목록을 페이지 단위로 조회합니다. 기본값: 20개씩, placeId 오름차순.")
    public ApiResponse<Page<PlaceResponse>> getPlaces(
            @PageableDefault(size = 20, sort = "placeId") Pageable pageable) {
        return ApiResponse.success(placeService.getPlaces(pageable));
    }

    // 통합 검색 — 키워드/카테고리/지역/반경(좌표)을 자유롭게 조합. 조건은 최소 1개 필요하고
    // 좌표 검색은 lat/lng/radius를 모두 보내야 하며 결과가 가까운 순으로 정렬됨
    @GetMapping("/search")
    @Operation(summary = "장소 통합 검색",
            description = "키워드/카테고리(cat1·cat2)/지역(region)/반경(lat·lng·radius[m]) 조건을 조합해 장소를 검색합니다. "
                    + "조건은 최소 1개 필요하고, 좌표 검색은 lat·lng·radius를 모두 보내야 하며 결과가 가까운 순으로 정렬됩니다.")
    public ApiResponse<List<PlaceSearchResponse>> searchPlaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String cat1,
            @RequestParam(required = false) String cat2,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double radius) {
        return ApiResponse.success(placeService.searchPlaces(keyword, cat1, cat2, region, lat, lng, radius));
    }

    // 카테고리 필터 (cat1 필수, cat2 선택)
    @GetMapping("/category")
    @Operation(summary = "카테고리 필터", description = "cat1(필수), cat2(선택) 카테고리 코드로 장소를 필터링합니다.")
    public ApiResponse<List<PlaceResponse>> getPlacesByCategory(
            @RequestParam(required = true) String cat1,
            @RequestParam(required = false) String cat2) {
        return ApiResponse.success(placeService.getPlacesByCategory(cat1, cat2));
    }
}
