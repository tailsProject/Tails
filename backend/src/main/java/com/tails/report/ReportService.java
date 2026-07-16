package com.tails.report;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.Comment;
import com.tails.comment.CommentRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.report.dto.AdminReportResponse;
import com.tails.report.dto.ReportCreateRequest;
import com.tails.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 신고 관련 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, ReportCreateRequest request) {
        requireTargetExistsAndNotSelf(memberId, request.targetType(), request.targetId());

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(memberId, request.targetType(), request.targetId())) {
            throw new CustomException(ErrorCode.DUPLICATE_REPORT);
        }

        Report report = Report.builder()
                .reporter(memberRepository.getReferenceById(memberId))
                .targetType(request.targetType())
                .targetId(request.targetId())
                .reason(request.reason())
                .build();
        return reportRepository.save(report).getId();
    }

    public Page<ReportResponse> getMyReports(Long memberId, Pageable pageable) {
        return reportRepository.findByReporterId(memberId, pageable).map(ReportResponse::from);
    }

    // 관리자 신고 처리 큐 조회. 기본으로 미처리(PENDING) 건만 보여준다
    public Page<AdminReportResponse> getReportsByStatus(ReportStatus status, Pageable pageable) {
        return reportRepository.findByStatus(status, pageable).map(AdminReportResponse::from);
    }

    @Transactional
    public void resolve(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        report.markAsResolved();
    }

    // 자기 신고 방지: MEMBER 신고는 대상이 본인, BOARD/COMMENT 신고는 그 글/댓글 작성자가 본인인 경우
    private void requireTargetExistsAndNotSelf(Long memberId, ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case BOARD -> {
                Board board = boardRepository.findById(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
                if (board.getMember() != null && board.getMember().getId().equals(memberId)) {
                    throw new CustomException(ErrorCode.CANNOT_REPORT_SELF);
                }
            }
            case COMMENT -> {
                Comment comment = commentRepository.findById(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
                if (comment.getMember() != null && comment.getMember().getId().equals(memberId)) {
                    throw new CustomException(ErrorCode.CANNOT_REPORT_SELF);
                }
            }
            case MEMBER -> {
                if (targetId.equals(memberId)) {
                    throw new CustomException(ErrorCode.CANNOT_REPORT_SELF);
                }
                if (!memberRepository.existsById(targetId)) {
                    throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
                }
            }
        }
    }
}
