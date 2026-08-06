package com.tails.place.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// petTourSyncList2 응답 항목. mapx/mapy는 API가 문자열로 내려줘서 String으로 받고,
// Place 변환 시 Double.parseDouble로 바꿈
@JsonIgnoreProperties(ignoreUnknown = true)
public record PetTourListItem(
        String addr1,
        String addr2,
        String contentid,
        String contenttypeid,
        String createdtime,
        String firstimage,
        String firstimage2,
        String mapx,
        String mapy,
        String modifiedtime,
        String tel,
        String title,
        String lclsSystm1,
        String lclsSystm2,
        String lclsSystm3) {
}
