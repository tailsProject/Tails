package com.tails.traveldetail.dto;

import java.util.List;

// 여행 경로 최적화(추천 순서) 조회 응답. DB에는 반영되지 않는 "미리보기" — 반영하려면 이 응답의
// orderedDetails 순서 그대로 detailIds를 뽑아 재정렬 API(PATCH .../order)를 다시 호출해야 함
public record OptimizedRouteResponse(
        List<TravelDetailResponse> orderedDetails,
        double totalDistanceMeters
) {
}
