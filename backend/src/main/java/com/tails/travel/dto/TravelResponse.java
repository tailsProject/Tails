package com.tails.travel.dto;

import com.tails.pet.dto.PetResponse;
import com.tails.travel.Travel;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;

// Travel 엔티티 응답 DTO
@Getter
public class TravelResponse {

    private final Long travelId;
    private final Long memberId;
    private final String title;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<PetResponse> pets;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TravelResponse(Long travelId, Long memberId, String title, String description, LocalDate startDate,
            LocalDate endDate, List<PetResponse> pets, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.travelId = travelId;
        this.memberId = memberId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pets = pets;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Travel 엔티티 → TravelResponse 변환
    public static TravelResponse from(Travel travel) {
        return new TravelResponse(
                travel.getTravelId(),
                travel.getMember().getId(),
                travel.getTitle(),
                travel.getDescription(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getPets().stream().map(PetResponse::from).toList(),
                travel.getCreatedAt(),
                travel.getUpdatedAt()
        );
    }
}
