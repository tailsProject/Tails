package com.tails.traveldetail;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.traveldetail.dto.OptimizedRouteResponse;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.traveldetail.dto.TravelDetailReorderRequest;
import com.tails.traveldetail.dto.TravelDetailResponse;
import com.tails.traveldetail.dto.TravelDetailUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// TravelDetail API 컨트롤러. Travel에 소속된 리소스라 중첩 경로 사용
@RestController
@RequestMapping("/api/travels/{travelId}/details")
@RequiredArgsConstructor
@Tag(name = "TravelDetail", description = "여행 일정 상세(방문 장소) 추가 / 조회 / 수정 / 삭제 API")
public class TravelDetailController {

    private final TravelDetailService travelDetailService;

    // 여행 일정에 방문 장소 추가
    @PostMapping
    @Operation(summary = "여행 일정에 방문 장소 추가", description = "travelId 여행 일정에 새 방문 장소를 추가합니다. sequence는 서버가 자동으로 계산합니다.")
    public ApiResponse<TravelDetailResponse> addTravelDetail(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TravelDetailCreateRequest request) {
        return ApiResponse.success(travelDetailService.addTravelDetail(travelId, userDetails.getMemberId(), request));
    }

    // 세부 일정 목록 조회. date가 있으면 그 날짜만, 없으면 전체
    @GetMapping
    @Operation(summary = "여행 일정 상세 목록 조회", description = "date 쿼리 파라미터가 있으면 그 날짜 일정만, 없으면 전체 일정을 날짜순/순서순으로 조회합니다.")
    public ApiResponse<List<TravelDetailResponse>> getTravelDetails(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) LocalDate date) {
        Long memberId = userDetails.getMemberId();
        if (date != null) {
            return ApiResponse.success(travelDetailService.getTravelDetailsByDate(travelId, memberId, date));
        }
        return ApiResponse.success(travelDetailService.getTravelDetails(travelId, memberId));
    }

    // 세부 일정 수정 (visitTime/memo만)
    @PutMapping("/{detailId}")
    @Operation(summary = "세부 일정 수정", description = "travelId 여행 일정의 detailId 세부 일정의 visitTime/memo를 수정합니다.")
    public ApiResponse<TravelDetailResponse> updateTravelDetail(
            @PathVariable Long travelId,
            @PathVariable Long detailId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TravelDetailUpdateRequest request) {
        return ApiResponse.success(
                travelDetailService.updateTravelDetail(travelId, detailId, userDetails.getMemberId(), request));
    }

    // 세부 일정 삭제
    @DeleteMapping("/{detailId}")
    @Operation(summary = "세부 일정 삭제", description = "travelId 여행 일정의 detailId 세부 일정을 삭제합니다.")
    public ApiResponse<Void> deleteTravelDetail(
            @PathVariable Long travelId,
            @PathVariable Long detailId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelDetailService.deleteTravelDetail(travelId, detailId, userDetails.getMemberId());
        return ApiResponse.success();
    }

    // 하루 일정 순서 재정렬. detailIds는 그 날짜의 세부 일정 전체와 정확히 일치해야 함
    @PatchMapping("/order")
    @Operation(summary = "하루 일정 순서 재정렬", description = "요청받은 detailIds 순서 그대로 그 날짜의 sequence를 다시 부여합니다. detailIds는 해당 날짜의 세부 일정 전체와 정확히 일치해야 합니다.")
    public ApiResponse<List<TravelDetailResponse>> reorderDetails(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TravelDetailReorderRequest request) {
        return ApiResponse.success(
                travelDetailService.reorderDetails(travelId, userDetails.getMemberId(), request));
    }

    // 하루 일정 경로 최적화 추천. DB 미반영 — 마음에 들면 재정렬 API를 다시 호출해서 반영
    @GetMapping("/optimize-route")
    @Operation(summary = "여행 경로 최적화 추천", description = "지정한 날짜의 방문 장소들을 최근접 이웃 방식으로 정렬한 추천 순서와 총 이동 거리를 반환합니다. DB에는 반영되지 않으며, 반영하려면 순서 재정렬 API를 다시 호출해야 합니다.")
    public ApiResponse<OptimizedRouteResponse> suggestOptimizedRoute(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam LocalDate date) {
        return ApiResponse.success(
                travelDetailService.suggestOptimizedRoute(travelId, userDetails.getMemberId(), date));
    }
}
