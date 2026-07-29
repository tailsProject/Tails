package com.tails.place.dto;

import com.tails.place.Place;
import java.util.List;
import lombok.Getter;

// Place 엔티티를 API 응답 형태로 변환하는 DTO.
// Entity를 직접 노출하지 않기 위해 사용하며, 생성은 {@link #from(Place, List)}
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
    private final List<String> imageUrls;
    private final String cat1;
    private final String cat2;
    private final String cat3;
    private final boolean bookmarked;

    private PlaceResponse(Long placeId, String placeName, String address, Double latitude,
            Double longitude, String phone, String petInfo, String imageUrl, List<String> imageUrls,
            String cat1, String cat2, String cat3, boolean bookmarked) {
        this.placeId = placeId;
        this.placeName = placeName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.petInfo = petInfo;
        this.imageUrl = imageUrl;
        this.imageUrls = imageUrls;
        this.cat1 = cat1;
        this.cat2 = cat2;
        this.cat3 = cat3;
        this.bookmarked = bookmarked;
    }

    // 목록/랭킹/추천 등에서 쓰는 기본 변환 - 장소별 추가 이미지 목록까지 조회하면 N+1이 되므로
    // imageUrls는 비워둔다(대표 이미지는 place.imageUrl로 그대로 노출됨). bookmarked도 항상 false
    public static PlaceResponse from(Place place) {
        return from(place, List.of(), false);
    }

    // 상세 조회 전용 - imageUrls는 place_image 테이블에서 별도 조회해 전달받는다
    // (Place 엔티티만으로는 알 수 없어서 파라미터로 받음, 단건 조회라 N+1 걱정 없음)
    // bookmarked도 로그인 회원의 찜 여부를 Service에서 계산해 전달받는다
    public static PlaceResponse from(Place place, List<String> imageUrls, boolean bookmarked) {
        return new PlaceResponse(
                place.getPlaceId(),
                place.getPlaceName(),
                place.getAddress(),
                place.getLatitude(),
                place.getLongitude(),
                place.getPhone(),
                place.getPetInfo(),
                place.getImageUrl(),
                imageUrls,
                place.getCat1(),
                place.getCat2(),
                place.getCat3(),
                bookmarked
        );
    }
}
