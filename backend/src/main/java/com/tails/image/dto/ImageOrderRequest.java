package com.tails.image.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// 이미지 순서 변경 요청 - 원하는 순서 그대로 나열한 전체 imageId 목록. 첫 번째가 대표 이미지
public record ImageOrderRequest(

        @NotEmpty(message = "이미지 순서 목록이 비어 있습니다.")
        List<Long> imageIds
) {
}
