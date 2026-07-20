package com.tails.place.dto;

import com.tails.place.Place;
import lombok.Getter;

// 평점순 정렬(GET /api/places/rankings/rating) 전용 응답 DTO.
// 평균 별점은 랭킹 화면에서만 필요한 값이라 공용 PlaceResponse에 넣지 않고 전용 타입으로 분리
@Getter
public class PlaceRatingResponse {

    private final Long placeId;
    private final String placeName;
    private final String address;
    private final String imageUrl;
    private final double averageRating;

    private PlaceRatingResponse(Place place, double averageRating) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress();
        this.imageUrl = place.getImageUrl();
        this.averageRating = averageRating;
    }

    public static PlaceRatingResponse of(Place place, double averageRating) {
        return new PlaceRatingResponse(place, averageRating);
    }
}
