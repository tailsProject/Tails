package com.tails.traveldetail.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

// 하루 일정 순서 재정렬 요청. detailIds는 "이 순서대로 방문" — 0번째가 그날의 1번째 방문(sequence=1)
// detailIds는 그 날짜에 실제로 존재하는 세부 일정 전체와 정확히 일치해야 함 (검증은 TravelDetailService.reorderDetails)
public record TravelDetailReorderRequest(
        @NotNull(message = "날짜를 입력해주세요.")
        LocalDate travelDate,

        @NotEmpty(message = "순서를 정할 세부 일정 목록을 입력해주세요.")
        List<Long> detailIds
) {
}
