package com.tails.place;

import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import static org.assertj.core.api.Assertions.assertThat;

// PlaceRepository.searchPlaces()의 region 조건이 실제 DB에서 SUBSTRING_INDEX로 정상 동작하는지 확인
class PlaceRegionSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private PlaceRepository placeRepository;
    @Autowired
    private PlaceService placeService;

    private Place save(String externalId, String name, String address) {
        Place place = Place.builder()
                .externalPlaceId(externalId)
                .placeName(name)
                .address(address)
                .latitude(37.5)
                .longitude(127.0)
                .build();
        return placeRepository.save(place);
    }

    @Test
    void 지역명이_주소_첫_토큰에_포함되면_검색된다() {
        save("region-1", "서울 장소", "서울특별시 강남구 테헤란로 123");
        save("region-2", "부산 장소", "부산광역시 해운대구 우동 456");

        var result = placeService.searchPlaces(null, null, null, "서울", null, null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void 다른_지역_주소_중간에_같은_문자열이_있어도_섞이지_않는다() {
        // "강남" 검색 시, 주소 첫 토큰(시/도)에 "강남"이 없는 곳은 매칭되면 안 됨(과거 전체 address LIKE 방식의 오탐 방지)
        save("region-3", "강남 아닌 장소", "부산광역시 강남아파트 앞");

        var result = placeService.searchPlaces(null, null, null, "강남", null, null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(0);
    }

    @Test
    void 지역_줄임말_별칭도_검색된다() {
        save("region-4", "충북 장소", "충청북도 청주시 상당구 123");

        var result = placeService.searchPlaces(null, null, null, "충북", null, null, null, PageRequest.of(0, 20));

        assertThat(result.getTotalElements()).isEqualTo(1);
    }
}
