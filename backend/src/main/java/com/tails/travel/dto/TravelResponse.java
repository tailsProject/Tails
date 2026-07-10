package com.tails.travel.dto;

import com.tails.travel.Travel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

// Travel 엔티티 응답 DTO
@Getter
public class TravelResponse {

    private final Long travelId;
    private final Long memberId;
    private final String title;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TravelResponse(Long travelId, Long memberId, String title, LocalDate startDate,
            LocalDate endDate, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.travelId = travelId;
        this.memberId = memberId;
        this.title = title;
        this.startDate = startDate;
        this.endDate = endDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Travel 엔티티 → TravelResponse 변환
    public static TravelResponse from(Travel travel) {
        return new TravelResponse(
                travel.getTravelId(),
                travel.getMember().getId(),
                travel.getTitle(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getCreatedAt(),
                travel.getUpdatedAt()
        );
    }
}
