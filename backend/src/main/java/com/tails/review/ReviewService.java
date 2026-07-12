package com.tails.review;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.review.dto.ReviewCreateRequest;
import com.tails.review.dto.ReviewListResponse;
import com.tails.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
}
