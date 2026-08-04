package com.tails.travel.dto;

import com.tails.traveldetail.TravelDetail;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;

// 공유 링크용 세부 일정 응답. TravelDetailResponse를 그대로 쓰지 않는 이유: memo는 개인 메모라
// 로그인 없이 누구나 보는 공유 응답에 노출되면 안 됨
@Getter
public class SharedTravelDetailResponse {

    private final Long detailId;
    private final Long placeId;
    private final String placeName;
    private final String placeImageUrl;
    private final Double placeLatitude;
    private final Double placeLongitude;
    private final LocalDate travelDate;
    private final LocalTime visitTime;
    private final Integer sequence;

    private SharedTravelDetailResponse(Long detailId, Long placeId, String placeName, String placeImageUrl,
            Double placeLatitude, Double placeLongitude, LocalDate travelDate, LocalTime visitTime,
            Integer sequence) {
        this.detailId = detailId;
        this.placeId = placeId;
        this.placeName = placeName;
        this.placeImageUrl = placeImageUrl;
        this.placeLatitude = placeLatitude;
        this.placeLongitude = placeLongitude;
        this.travelDate = travelDate;
        this.visitTime = visitTime;
        this.sequence = sequence;
    }

    public static SharedTravelDetailResponse from(TravelDetail travelDetail) {
        var place = travelDetail.getPlace();
        return new SharedTravelDetailResponse(
                travelDetail.getDetailId(),
                place != null ? place.getPlaceId() : null,
                place != null ? place.getPlaceName() : null,
                place != null ? place.getImageUrl() : null,
                place != null ? place.getLatitude() : null,
                place != null ? place.getLongitude() : null,
                travelDetail.getTravelDate(),
                travelDetail.getVisitTime(),
                travelDetail.getSequence()
        );
    }
}
