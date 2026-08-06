package com.tails.traveldetail.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

// 방문 장소 추가 요청 DTO. sequence는 서버가 자동 계산하므로 요청에서 받지 않음
public record TravelDetailCreateRequest(
        @NotNull(message = "방문할 장소를 선택해주세요.")
        Long placeId,

        @NotNull(message = "방문 날짜를 입력해주세요.")
        LocalDate travelDate,

        LocalTime visitTime,

        String memo
) {
}
