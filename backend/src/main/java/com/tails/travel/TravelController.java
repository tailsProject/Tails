package com.tails.travel;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
