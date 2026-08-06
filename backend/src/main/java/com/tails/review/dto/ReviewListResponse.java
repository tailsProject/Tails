package com.tails.review.dto;

import org.springframework.data.domain.Page;

// 장소 리뷰 목록(GET /api/places/{placeId}/reviews) 응답.
// averageRating/reviewCount는 "지금 이 페이지"가 아니라 이 장소의 리뷰 전체를 기준으로 계산한 값이다.
public record ReviewListResponse(
        Page<ReviewResponse> reviews,
        double averageRating,
        long reviewCount
) {
}
