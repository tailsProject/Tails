package com.tails.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// 리뷰 작성 요청 데이터를 전달하고 입력값 검증을 수행하는 DTO
public record ReviewCreateRequest(

        // int가 아니라 Integer인 이유: int면 요청에 rating이 아예 없을 때 0으로 채워져
        // @NotNull에 안 걸리고 @Min(1)에 걸림 — "값을 안 보냈다"는 메시지를 주기 위해 Integer + @NotNull
        @NotNull(message = "별점을 입력해주세요.")
        @Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
        @Max(value = 5, message = "별점은 5점 이하여야 합니다.")
        Integer rating,

        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        @Size(max = 2000, message = "리뷰는 2000자 이하여야 합니다.")
        String content
) {
}
