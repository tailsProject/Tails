package com.tails.traveldetail;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.traveldetail.dto.TravelDetailCreateRequest;
import com.tails.traveldetail.dto.TravelDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// TravelDetail API 컨트롤러. TravelDetail은 항상 특정 Travel에 소속되므로
// /api/travels/{travelId}/details 중첩 경로를 쓴다 — URL만 봐도 소속 관계가 드러남
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
}
