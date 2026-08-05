package com.tails.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 리뷰 수정 요청. 검증 규칙은 ReviewCreateRequest와 동일 — 별점/내용을 항상 함께 받음(부분 수정 아님)
public record ReviewUpdateRequest(

        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        Integer rating,

        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        @Size(max = 2000, message = "리뷰는 2000자 이하여야 합니다.")
        String content
) {
}
