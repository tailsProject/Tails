package com.tails.travel.dto;

import com.tails.pet.dto.PetResponse;
import com.tails.travel.Travel;
import com.tails.traveldetail.TravelDetail;
import java.time.LocalDate;
import java.util.List;
import lombok.Getter;

// 공유 링크로 로그인 없이 조회하는 여행 일정 응답, 소유자 식별자는 제외한 공개용 정보만 포함
@Getter
public class SharedTravelResponse {

    private final String title;
    private final String description;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final List<PetResponse> pets;
    private final String thumbnailUrl;
    private final List<SharedTravelDetailResponse> details;

    private SharedTravelResponse(String title, String description, LocalDate startDate, LocalDate endDate,
            List<PetResponse> pets, String thumbnailUrl, List<SharedTravelDetailResponse> details) {
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
        this.pets = pets;
        this.thumbnailUrl = thumbnailUrl;
        this.details = details;
    }

    public static SharedTravelResponse of(Travel travel, List<TravelDetail> travelDetails, String thumbnailUrl) {
        List<SharedTravelDetailResponse> details = travelDetails.stream()
                .map(SharedTravelDetailResponse::from)
                .toList();
        List<PetResponse> pets = travel.getPets().stream().map(PetResponse::from).toList();
        return new SharedTravelResponse(
                travel.getTitle(), travel.getDescription(), travel.getStartDate(), travel.getEndDate(),
                pets, thumbnailUrl, details
        );
    }
}
