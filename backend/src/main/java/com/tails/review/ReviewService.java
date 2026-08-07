package com.tails.review;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.image.Image;
import com.tails.image.ImageRepository;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.review.dto.MyReviewResponse;
import com.tails.review.dto.PlaceRatingSummaryResponse;
import com.tails.review.dto.RecentReviewResponse;
import com.tails.review.dto.ReviewCreateRequest;
import com.tails.review.dto.ReviewListResponse;
import com.tails.review.dto.ReviewResponse;
import com.tails.review.dto.ReviewUpdateRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 장소 리뷰 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final PlaceRepository placeRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final FileStorage fileStorage;

    // 리뷰 작성. 이미 이 장소에 리뷰를 작성한 회원이면 409(DUPLICATE_REVIEW)
    @Transactional
    public Long create(Long memberId, Long placeId, ReviewCreateRequest request) {
        Place place = placeRepository.findById(placeId)
                .orElseThrow(() -> new CustomException(ErrorCode.PLACE_NOT_FOUND));

        if (reviewRepository.existsByPlace_PlaceIdAndMember_Id(placeId, memberId)) {
            throw new CustomException(ErrorCode.DUPLICATE_REVIEW);
        }

        Review review = Review.builder()
                .member(memberRepository.getReferenceById(memberId))
                .place(place)
                .rating(request.rating())
                .content(request.content())
                .build();
        return reviewRepository.save(review).getReviewId();
    }

    // 장소 리뷰 목록 + 평균 별점/리뷰 수. 리뷰가 하나도 없으면 avg가 null이라 0.0으로 대체
    public ReviewListResponse getReviews(Long placeId, Pageable pageable) {
        if (!placeRepository.existsById(placeId)) {
            throw new CustomException(ErrorCode.PLACE_NOT_FOUND);
        }

        Page<ReviewResponse> reviews = reviewRepository.findByPlaceIdWithMember(placeId, pageable)
                .map(ReviewResponse::from);
        Double averageRating = reviewRepository.findAverageRatingByPlaceId(placeId);
        long reviewCount = reviewRepository.countByPlace_PlaceId(placeId);

        return new ReviewListResponse(reviews, averageRating != null ? averageRating : 0.0, reviewCount);
    }

    // 리뷰 수정 - 작성자 본인만 가능
    @Transactional
    public void update(Long memberId, Long placeId, Long reviewId, ReviewUpdateRequest request) {
        Review review = getReviewInPlaceOrThrow(placeId, reviewId);
        requireOwner(review, memberId);
        review.updateInfo(request.rating(), request.content());
    }

    // 리뷰 삭제 - 작성자 본인 또는 ADMIN 가능 (신고된 리뷰 강제 삭제 용도)
    @Transactional
    public void delete(Long memberId, Long placeId, Long reviewId) {
        Review review = getReviewInPlaceOrThrow(placeId, reviewId);
        requireOwnerOrAdmin(review, memberId);

        // DB에서 삭제되지 않는 파일은 직접 삭제
        for (Image image : imageRepository.findByReview_ReviewIdOrderByCreatedAtAsc(reviewId)) {
            fileStorage.deleteAfterCommit(image.getStoredFileName());
        }

        reviewRepository.delete(review);
    }

    // 메인페이지 "최근 리뷰" 미리보기
    public List<RecentReviewResponse> getRecentReviews(int size) {
        return reviewRepository.findRecentWithMemberAndPlace(PageRequest.of(0, size)).stream()
                .map(RecentReviewResponse::from)
                .toList();
    }

    // 내가 작성한 리뷰 목록
    public Page<MyReviewResponse> getMyReviews(Long memberId, Pageable pageable) {
        return reviewRepository.findByMemberIdWithPlace(memberId, pageable)
                .map(MyReviewResponse::from);
    }

    // 메인페이지 "인기 장소" 카드용 - 여러 장소의 평균 별점/리뷰 수를 한 번에 조회.
    // 리뷰가 하나도 없는 장소는 결과에서 빠지므로(그룹 자체가 없음), 응답 목록에 없으면 리뷰 없음으로 보면 됨
    public List<PlaceRatingSummaryResponse> getRatingSummaries(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return List.of();
        }
        return reviewRepository.findRatingSummariesByPlaceIds(placeIds).stream()
                .map(row -> new PlaceRatingSummaryResponse((Long) row[0], (Double) row[1], (Long) row[2]))
                .toList();
    }

    // 작성자 본인인지 확인. 탈퇴한 회원(member == null)의 리뷰는 정당한 소유자가 없으므로 누구든 거부
    private void requireOwner(Review review, Long memberId) {
        if (review.getMember() == null || !review.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }
    }

    // 삭제는 작성자 본인 또는 ADMIN/MANAGER가 할 수 있음 (신고된 리뷰 강제 삭제 용도)
    private void requireOwnerOrAdmin(Review review, Long memberId) {
        if (review.getMember() != null && review.getMember().getId().equals(memberId)) {
            return;
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_REVIEW_OWNER));
        if (!member.getRole().isStaff()) {
            throw new CustomException(ErrorCode.NOT_REVIEW_OWNER);
        }
    }

    // reviewId로 조회하고, 그 리뷰가 URL의 placeId 소속이 맞는지도 함께 확인 (다른 장소의 reviewId를 잘못/악의적으로 넘긴 경우 방지)
    private Review getReviewInPlaceOrThrow(Long placeId, Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getPlace().getPlaceId().equals(placeId)) {
            throw new CustomException(ErrorCode.REVIEW_NOT_FOUND);
        }
        return review;
    }
}
