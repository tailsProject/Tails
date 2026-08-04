package com.tails.travel;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.travel.dto.ShareTokenResponse;
import com.tails.travel.dto.SharedTravelResponse;
import com.tails.travel.dto.TravelCreateRequest;
import com.tails.travel.dto.TravelResponse;
import com.tails.travel.dto.TravelUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    // 내 여행 일정 목록 페이지 단위 조회, 기본 시작일 내림차순 10개
    @GetMapping
    @Operation(summary = "내 여행 일정 목록 조회", description = "로그인한 회원이 만든 여행 일정 목록을 페이지 단위로 조회합니다.")
    public ApiResponse<Page<TravelResponse>> getMyTravels(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(travelService.getMyTravels(userDetails.getMemberId(), pageable));
    }

    // 여행 일정 상세 조회
    @GetMapping("/{travelId}")
    @Operation(summary = "여행 일정 상세 조회", description = "travelId로 여행 일정 상세 정보를 조회합니다. 본인 소유가 아니면 접근이 거부됩니다.")
    public ApiResponse<TravelResponse> getTravelDetail(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(travelService.getTravelDetail(travelId, userDetails.getMemberId()));
    }

    // 여행 일정 수정
    @PutMapping("/{travelId}")
    @Operation(summary = "여행 일정 수정", description = "travelId 여행 일정을 수정합니다. 본인 소유가 아니면 접근이 거부됩니다.")
    public ApiResponse<TravelResponse> updateTravel(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TravelUpdateRequest request) {
        return ApiResponse.success(travelService.updateTravel(travelId, userDetails.getMemberId(), request));
    }

    // 여행 일정 삭제
    @DeleteMapping("/{travelId}")
    @Operation(summary = "여행 일정 삭제", description = "travelId 여행 일정을 삭제합니다. 본인 소유가 아니면 접근이 거부됩니다.")
    public ApiResponse<Void> deleteTravel(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelService.deleteTravel(travelId, userDetails.getMemberId());
        return ApiResponse.success();
    }

    // 여행 일정 공유 링크 발급, 이미 공유 중이면 기존 링크 그대로 반환, 본인 소유만 가능
    @PostMapping("/{travelId}/share")
    @Operation(summary = "여행 일정 공유 링크 발급", description = "travelId 여행 일정의 공유 링크를 발급합니다. 이미 공유 중이면 기존 링크를 그대로 반환합니다.")
    public ApiResponse<ShareTokenResponse> shareTravel(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(travelService.shareTravel(travelId, userDetails.getMemberId()));
    }

    // 여행 일정 공유 중단, 본인 소유만 가능
    @DeleteMapping("/{travelId}/share")
    @Operation(summary = "여행 일정 공유 중단", description = "travelId 여행 일정의 공유를 중단합니다. 이후 기존 공유 링크는 사용할 수 없습니다.")
    public ApiResponse<Void> unshareTravel(
            @PathVariable Long travelId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        travelService.unshareTravel(travelId, userDetails.getMemberId());
        return ApiResponse.success();
    }

    // 공유 링크로 여행 일정 읽기 전용 조회, 로그인 불필요
    @GetMapping("/shared/{shareToken}")
    @Operation(summary = "공유된 여행 일정 조회", description = "공유 토큰으로 여행 일정을 읽기 전용 조회합니다. 로그인 불필요.")
    public ApiResponse<SharedTravelResponse> getSharedTravel(@PathVariable String shareToken) {
        return ApiResponse.success(travelService.getSharedTravel(shareToken));
    }
}
