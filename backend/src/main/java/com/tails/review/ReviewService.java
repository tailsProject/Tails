package com.tails.review;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.place.Place;
import com.tails.place.PlaceRepository;
import com.tails.review.dto.ReviewCreateRequest;
import lombok.RequiredArgsConstructor;
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
}
