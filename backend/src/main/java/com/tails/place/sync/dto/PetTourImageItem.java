package com.tails.place.sync.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

// detailImage2 응답 항목 - 장소 하나에 딸린 추가 사진 목록(원본 이미지 URL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record PetTourImageItem(
        String originimgurl,
        String imgname,
        String serialnum) {
}
