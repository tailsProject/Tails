package com.tails.report.dto;

import com.tails.report.Report;
import com.tails.report.ReportStatus;
import com.tails.report.ReportTargetType;
import java.time.LocalDateTime;

public record ReportResponse(
        Long reportId,
        ReportTargetType targetType,
        Long targetId,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt
) {
    public static ReportResponse from(Report report) {
        return new ReportResponse(
                report.getId(),
                report.getTargetType(),
                report.getTargetId(),
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
