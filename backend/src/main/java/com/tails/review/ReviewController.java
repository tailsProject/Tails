package com.tails.review;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.review.dto.MyReviewResponse;
import com.tails.review.dto.PlaceRatingSummaryResponse;
import com.tails.review.dto.RecentReviewResponse;
import com.tails.review.dto.ReviewCreateRequest;
import com.tails.review.dto.ReviewListResponse;
import com.tails.review.dto.ReviewUpdateRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 장소 리뷰 API. 로그인 필요
// "내가 작성한 리뷰"(/api/reviews/me)처럼 장소에 묶이지 않는 경로도 있어서
// 클래스 레벨 @RequestMapping 대신 메서드마다 전체 경로를 적음 (BoardBookmarkController와 동일)
@RestController
@RequiredArgsConstructor
@Tag(name = "Review", description = "장소 리뷰 및 별점 API")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/places/{placeId}/reviews")
    @Operation(summary = "리뷰 작성", description = "장소에 별점과 리뷰를 작성합니다. 한 장소에 리뷰는 회원당 하나만 작성할 수 있습니다. 로그인 필요.")
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long placeId,
                                     @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.create(userDetails.getMemberId(), placeId, request));
    }

    @GetMapping("/api/places/{placeId}/reviews")
    @Operation(summary = "장소 리뷰 목록 조회", description = "placeId 장소의 리뷰 목록과 평균 별점/리뷰 수를 조회합니다. 로그인 필요.")
    public ApiResponse<ReviewListResponse> getReviews(@PathVariable Long placeId,
                                                       @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(reviewService.getReviews(placeId, pageable));
    }

    @PatchMapping("/api/places/{placeId}/reviews/{reviewId}")
    @Operation(summary = "리뷰 수정", description = "reviewId 리뷰를 수정합니다. 작성자 본인만 가능.")
    public ApiResponse<Void> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long placeId,
                                     @PathVariable Long reviewId,
                                     @Valid @RequestBody ReviewUpdateRequest request) {
        reviewService.update(userDetails.getMemberId(), placeId, reviewId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/api/places/{placeId}/reviews/{reviewId}")
    @Operation(summary = "리뷰 삭제", description = "reviewId 리뷰를 삭제합니다. 작성자 본인만 가능.")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long placeId,
                                     @PathVariable Long reviewId) {
        reviewService.delete(userDetails.getMemberId(), placeId, reviewId);
        return ApiResponse.success();
    }

    @GetMapping("/api/reviews/recent")
    @Operation(summary = "최근 리뷰 미리보기", description = "전체 장소를 통틀어 최신순으로 리뷰 N건을 조회합니다. 메인페이지용, 로그인 불필요.")
    public ApiResponse<List<RecentReviewResponse>> getRecentReviews(@RequestParam(defaultValue = "4") int size) {
        return ApiResponse.success(reviewService.getRecentReviews(size));
    }

    @GetMapping("/api/reviews/rating-summary")
    @Operation(summary = "여러 장소 평균 별점/리뷰 수 일괄 조회",
            description = "placeIds에 해당하는 장소들의 평균 별점과 리뷰 수를 한 번에 조회합니다. "
                    + "메인페이지 인기 장소 카드용, 로그인 불필요. 리뷰가 없는 장소는 결과에서 빠집니다.")
    public ApiResponse<List<PlaceRatingSummaryResponse>> getRatingSummaries(@RequestParam List<Long> placeIds) {
        return ApiResponse.success(reviewService.getRatingSummaries(placeIds));
    }

    @GetMapping("/api/reviews/me")
    @Operation(summary = "내가 작성한 리뷰 목록 조회", description = "로그인한 회원이 작성한 리뷰 목록을 조회합니다.")
    public ApiResponse<Page<MyReviewResponse>> getMyReviews(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                             @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(reviewService.getMyReviews(userDetails.getMemberId(), pageable));
    }
}
