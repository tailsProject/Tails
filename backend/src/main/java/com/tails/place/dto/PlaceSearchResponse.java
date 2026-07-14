package com.tails.place.dto;

import com.tails.place.Place;
import lombok.Getter;

// 통합 검색(GET /api/places/search) 전용 응답 DTO
// PlaceResponse와 거의 같지만, 검색 기준 좌표에서의 거리(distanceMeters)는 "이번 검색 요청"에서만
// 의미가 있는 값이라 별도 타입으로 분리
@Getter
public class PlaceSearchResponse {

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
    private final Double distanceMeters;

    private PlaceSearchResponse(Place place, Double distanceMeters) {
        this.placeId = place.getPlaceId();
        this.placeName = place.getPlaceName();
        this.address = place.getAddress();
        this.latitude = place.getLatitude();
        this.longitude = place.getLongitude();
        this.phone = place.getPhone();
        this.petInfo = place.getPetInfo();
        this.imageUrl = place.getImageUrl();
        this.cat1 = place.getCat1();
        this.cat2 = place.getCat2();
        this.cat3 = place.getCat3();
        this.distanceMeters = distanceMeters;
    }

    // 좌표 검색 결과 변환 — 계산된 거리를 함께 담음
    public static PlaceSearchResponse of(Place place, double distanceMeters) {
        return new PlaceSearchResponse(place, distanceMeters);
    }

    // 좌표 없는 검색(키워드/카테고리/지역) 결과 변환 — 거리는 null
    public static PlaceSearchResponse from(Place place) {
        return new PlaceSearchResponse(place, null);
    }
}
