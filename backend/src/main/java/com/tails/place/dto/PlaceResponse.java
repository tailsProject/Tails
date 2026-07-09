package com.tails.place.dto;

import com.tails.place.Place;
import lombok.Getter;

// Place 엔티티를 API 응답 형태로 변환하는 DTO.
// Entity를 직접 노출하지 않기 위해 사용하며, 생성은 {@link #from(Place)}
@Getter
public class PlaceResponse {

    private final Long placeId;
    private final String placeName;
    private final String address;
    private final Double latitude;
    private final Double longitude;
    private final String phone;
    private final String petInfo;
    private final String imageUrl;
    private final String cat1;
    private final String cat2;
    private final String cat3;

    private PlaceResponse(Long placeId, String placeName, String address, Double latitude,
            Double longitude, String phone, String petInfo, String imageUrl, String cat1,
            String cat2, String cat3) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.petInfo = petInfo;
        this.imageUrl = imageUrl;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
    }

    // Place 엔티티를 PlaceResponse DTO로 변환
    public static PlaceResponse from(Place place) {
        return new PlaceResponse(
                place.getPlaceId(),
                place.getPlaceName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPhone(),
                place.getPetInfo(),
                place.getImageUrl(),
                place.getCat1(),
                place.getCat2(),
                place.getCat3()
        );
    }
}
