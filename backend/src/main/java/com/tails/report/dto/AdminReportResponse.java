package com.tails.report.dto;

import com.tails.report.Report;
import com.tails.report.ReportStatus;
import com.tails.report.ReportTargetType;
import java.time.LocalDateTime;

// 관리자 신고 처리 큐 응답. ReportResponse와 달리 reporterId/reporterNickname을 포함
public record AdminReportResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(Report report) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
