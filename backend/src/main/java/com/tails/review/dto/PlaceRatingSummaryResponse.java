package com.tails.review.dto;

// 여러 장소의 평균 별점/리뷰 수를 한 번에 조회할 때 쓰는 응답(메인페이지 "인기 장소" 카드용).
// 리뷰가 하나도 없는 장소는 배치 조회 결과에 아예 포함되지 않으므로, 이 목록에 없으면 리뷰 없음으로 간주한다.
public record PlaceRatingSummaryResponse(Long placeId, double averageRating, long reviewCount) {
}
