package com.tails.review.dto;

import com.tails.review.Review;

import java.time.LocalDateTime;

// 내가 작성한 리뷰 목록의 각 항목 응답. 작성자는 본인이라 자명하므로 대신 어느 장소의 리뷰인지를 담는다
public record MyReviewResponse(
        Long reviewId,
        Long placeId,
        String placeName,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MyReviewResponse from(Review review) {
        return new MyReviewResponse(
                review.getReviewId(),
                review.getPlace().getPlaceId(),
                review.getPlace().getPlaceName(),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
