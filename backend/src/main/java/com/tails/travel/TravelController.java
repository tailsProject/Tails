package com.tails.travel;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Travel API 컨트롤러
@RestController
@RequestMapping("/api/travels")
@RequiredArgsConstructor
@Tag(name = "Travel", description = "여행 일정 생성 / 조회 / 수정 / 삭제 API")
public class TravelController {

    private final TravelService travelService;

    // 여행 일정 생성
    @PostMapping
    @Operation(summary = "여행 일정 생성", description = "새 여행 일정을 만듭니다.")
    public ApiResponse<TravelResponse> createTravel(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TravelCreateRequest request) {
        return ApiResponse.success(travelService.createTravel(userDetails.getMemberId(), request));
    }

    // 내 여행 일정 목록 조회
    @GetMapping
    @Operation(summary = "내 여행 일정 목록 조회", description = "로그인한 회원이 만든 여행 일정 목록을 조회합니다.")
    public ApiResponse<List<TravelResponse>> getMyTravels(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(travelService.getMyTravels(userDetails.getMemberId()));
    }

    // 여행 일정 상세 조회
    @GetMapping("/{travelId}")
    @Operation(summary = "여행 일정 상세 조회", description = "travelId로 여행 일정 상세 정보를 조회합니다. 본인 소유가 아니면 접근이 거부됩니다.")
    public ApiResponse<TravelResponse> getTravelDetail(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(travelService.getTravelDetail(travelId, userDetails.getMemberId()));
    }
}
