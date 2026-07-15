package com.tails.place.dto;

// 개인화 추천 결과 한 건. place는 기존 PlaceResponse를 재사용하고 score(0~1, 코사인 유사도)만 추가
public record PlaceRecommendationResponse(PlaceResponse place, double score) {
}
