package com.tails.place.dto;

import com.tails.place.Place;
import lombok.Getter;

// 인기순 정렬(GET /api/places/rankings/popular) 전용 응답 DTO.
// PlaceRatingResponse와 같은 이유로 공용 PlaceResponse 대신 "찜 개수"만 얹은 전용 타입을 둠
@Getter
public class PlaceBookmarkCountResponse {

    private final Long placeId;
    private final String placeName;
    private final String address;
    private final String imageUrl;
    private final long bookmarkCount;

    private PlaceBookmarkCountResponse(Place place, long bookmarkCount) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress();
        this.imageUrl = place.getImageUrl();
        this.bookmarkCount = bookmarkCount;
    }

    public static PlaceBookmarkCountResponse of(Place place, long bookmarkCount) {
        return new PlaceBookmarkCountResponse(place, bookmarkCount);
    }
}
