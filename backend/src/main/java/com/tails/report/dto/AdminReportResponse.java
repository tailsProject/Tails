package com.tails.report.dto;

import com.tails.report.Report;
import com.tails.report.ReportStatus;
import com.tails.report.ReportTargetType;
import java.time.LocalDateTime;

// 관리자 신고 처리 큐 응답. ReportResponse와 달리 reporterId/reporterNickname, 신고 대상 미리보기를 포함
public record AdminReportResponse(
        Long reportId,
        Long reporterId,
        String reporterNickname,
        ReportTargetType targetType,
        Long targetId,
        Long targetBoardId,
        String targetPreview,
        String reason,
        ReportStatus status,
        LocalDateTime createdAt
) {
    public static AdminReportResponse from(Report report, String targetPreview, Long targetBoardId) {
        return new AdminReportResponse(
                report.getId(),
                report.getReporter().getId(),
                report.getReporter().getNickname(),
                report.getTargetType(),
                report.getTargetId(),
                targetBoardId,
                targetPreview,
                report.getReason(),
                report.getStatus(),
                report.getCreatedAt()
        );
    }
}
