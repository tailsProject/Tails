package com.tails.travel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

// 여행 일정 생성(POST /api/travels) 요청 DTO
// @NotBlank/@NotNull/@Size는 컨트롤러의 @Valid가 실행 / endDate < startDate 검증은 TravelService에서 처리
public record TravelCreateRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
        String title,

        @Size(max = 255, message = "소개글은 255자 이하여야 합니다.")
        String description,

        @NotNull(message = "시작일을 입력해주세요.")
        LocalDate startDate,

        @NotNull(message = "종료일을 입력해주세요.")
        LocalDate endDate,

        // 함께 가는 반려동물, 미입력 시 빈 목록으로 처리
        List<Long> petIds
) {
}
