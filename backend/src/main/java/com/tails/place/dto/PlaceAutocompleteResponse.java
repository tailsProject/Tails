package com.tails.place.dto;

import com.tails.place.Place;
import lombok.Getter;

// 검색창 자동완성(GET /api/places/autocomplete) 전용 응답 DTO
@Getter
public class PlaceAutocompleteResponse {

    private final Long placeId;
    private final String placeName;
    private final String address;

    private PlaceAutocompleteResponse(Place place) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress();
    }

    public static PlaceAutocompleteResponse from(Place place) {
        return new PlaceAutocompleteResponse(place);
    }
}
