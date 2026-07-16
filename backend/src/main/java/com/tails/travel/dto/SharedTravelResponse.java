package com.tails.travel.dto;

import com.tails.travel.Travel;
import com.tails.traveldetail.TravelDetail;
import com.tails.traveldetail.dto.TravelDetailResponse;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

// 공유 링크로 로그인 없이 조회하는 여행 일정 응답 DTO. TravelResponse와 달리 memberId(소유자 식별자)는
// 포함하지 않음 — 링크만 있으면 누구나 보는 공개 응답이라 소유자 정보는 최소한만 노출
@Getter
public class SharedTravelResponse {

    private final String title;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<TravelDetailResponse> details;

    private SharedTravelResponse(String title, LocalDate startDate, LocalDate endDate,
            List<TravelDetailResponse> details) {
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.details = details;
    }

    public static SharedTravelResponse of(Travel travel, List<TravelDetail> travelDetails) {
        List<TravelDetailResponse> details = travelDetails.stream()
                .map(TravelDetailResponse::from)
                .toList();
        return new SharedTravelResponse(travel.getTitle(), travel.getStartDate(), travel.getEndDate(), details);
    }
}
