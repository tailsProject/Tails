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
    // TourAPI 세부분류(cat1)가 비어있는 일부 장소도 이 값(관광지/문화시설/행사공연축제/레포츠/숙박/쇼핑/음식점 등)은
    // 항상 있어서, 프론트에서 카테고리 배지를 표시할 때 cat1이 없을 때의 폴백으로 쓴다
    private final String contentTypeId;

    private PlaceResponse(Long placeId, String placeName, String address, Double latitude,
            Double longitude, String phone, String petInfo, String imageUrl,
            List<String> imageUrls, String cat1, String cat2, String cat3,
            String contentTypeId) {
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
        this.contentTypeId = contentTypeId;
    }

    // 목록/랭킹/추천 등에서 쓰는 기본 변환 - 장소별 추가 이미지 목록까지 조회하면 N+1이 되므로
    // imageUrls는 비워둔다(대표 이미지는 place.imageUrl로 그대로 노출됨)
    public static PlaceResponse from(Place place) {
        return from(place, List.of());
    }

    // 상세 조회 전용 - imageUrls는 place_image 테이블에서 별도 조회해 전달받는다
    // (Place 엔티티만으로는 알 수 없어서 파라미터로 받음, 단건 조회라 N+1 걱정 없음)
    public static PlaceResponse from(Place place, List<String> imageUrls) {
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
                place.getContentTypeId()
        );
    }
}
