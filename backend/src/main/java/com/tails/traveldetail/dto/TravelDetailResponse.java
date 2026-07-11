package com.tails.traveldetail.dto;

import com.tails.traveldetail.TravelDetail;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.Getter;

// TravelDetail 엔티티 응답 DTO. placeName은 연관된 Place에서 미리 꺼내 채워줌
// (프론트가 장소명 표시를 위해 Place API를 또 호출하지 않아도 되도록)
@Getter
public class TravelDetailResponse {

    private final Long detailId;
    private final Long travelId;
    private final Long placeId;
    private final String placeName;
    private final LocalDate travelDate;
    private final LocalTime visitTime;
    private final String memo;
    private final Integer sequence;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private TravelDetailResponse(Long detailId, Long travelId, Long placeId, String placeName,
            LocalDate travelDate, LocalTime visitTime, String memo, Integer sequence,
            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.detailId = detailId;
        this.travelId = travelId;
        this.placeId = placeId;
        this.placeName = placeName;
        this.travelDate = travelDate;
        this.visitTime = visitTime;
        this.memo = memo;
        this.sequence = sequence;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // TravelDetail 엔티티 → TravelDetailResponse 변환
    // travel/place가 LAZY라서 트랜잭션(@Transactional) 안에서 호출해야 함
    public static TravelDetailResponse from(TravelDetail travelDetail) {
        return new TravelDetailResponse(
                travelDetail.getDetailId(),
                travelDetail.getTravel().getTravelId(),
                travelDetail.getPlace().getPlaceId(),
                travelDetail.getPlace().getPlaceName(),
                travelDetail.getTravelDate(),
                travelDetail.getVisitTime(),
                travelDetail.getMemo(),
                travelDetail.getSequence(),
                travelDetail.getCreatedAt(),
                travelDetail.getUpdatedAt()
        );
    }
}
