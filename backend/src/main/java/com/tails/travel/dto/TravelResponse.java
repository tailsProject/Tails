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
    // 카드 썸네일용 대표 이미지, 1일차 첫 방문지의 장소 이미지, 없으면 null
    private final String thumbnailUrl;
    // 공유 중인 토큰, 없으면 비공개, 프론트가 이 값으로 공유 상태 판단
    private final String shareToken;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TravelResponse(Long travelId, Long memberId, String title, String description, LocalDate startDate,
            LocalDate endDate, List<PetResponse> pets, String thumbnailUrl, String shareToken,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.travelId = travelId;
        this.memberId = memberId;
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pets = pets;
        this.thumbnailUrl = thumbnailUrl;
        this.shareToken = shareToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // thumbnailUrl은 엔티티에 없는 파생 값이라 별도 조회 결과를 받아 조립
    public static TravelResponse from(Travel travel, String thumbnailUrl) {
        return new TravelResponse(
                travel.getTravelId(),
                travel.getMember().getId(),
                travel.getTitle(),
                travel.getDescription(),
                travel.getStartDate(),
                travel.getEndDate(),
                travel.getPets().stream().map(PetResponse::from).toList(),
                thumbnailUrl,
                travel.getShareToken(),
                travel.getCreatedAt(),
                travel.getUpdatedAt()
        );
    }
}
