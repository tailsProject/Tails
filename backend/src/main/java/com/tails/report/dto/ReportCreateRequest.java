package com.tails.report.dto;

import com.tails.report.ReportTargetType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(

        @NotNull(message = "신고 대상 종류를 선택해주세요.")
        ReportTargetType targetType,

        @NotNull(message = "신고 대상을 선택해주세요.")
        Long targetId,

        @NotBlank(message = "신고 사유를 입력해주세요.")
        @Size(max = 500, message = "신고 사유는 500자 이하여야 합니다.")
        String reason
) {
}
