package com.tails.report;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.Comment;
import com.tails.comment.CommentRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.notification.event.ReportResolvedEvent;
import com.tails.report.dto.AdminReportResponse;
import com.tails.report.dto.ReportCreateRequest;
import com.tails.report.dto.ReportResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;

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
        return reportRepository.findByStatus(status, pageable).map(this::toAdminResponse);
    }

    @Transactional
    public void resolve(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        report.markAsResolved();
        eventPublisher.publishEvent(new ReportResolvedEvent(report.getReporter().getId(), report.getId()));
    }

    // 신고 대상의 현재 내용을 미리보기로 붙여줌. 이미 삭제/탈퇴된 대상이면 안내 문구로 대체
    private AdminReportResponse toAdminResponse(Report report) {
        TargetPreview preview = switch (report.getTargetType()) {
            case BOARD -> boardRepository.findById(report.getTargetId())
                    .map(board -> new TargetPreview(board.getTitle(), null))
                    .orElse(new TargetPreview("삭제된 게시글입니다.", null));
            case COMMENT -> commentRepository.findById(report.getTargetId())
                    .map(comment -> new TargetPreview(comment.getContent(), comment.getBoard().getId()))
                    .orElse(new TargetPreview("삭제된 댓글입니다.", null));
            case MEMBER -> memberRepository.findById(report.getTargetId())
                    .map(member -> new TargetPreview(member.getNickname() + " (" + member.getEmail() + ")", null))
                    .orElse(new TargetPreview("탈퇴한 회원입니다.", null));
        };
        return AdminReportResponse.from(report, preview.text(), preview.boardId());
    }

    private record TargetPreview(String text, Long boardId) {
    }

    // 신고 기록 자체를 삭제(처리 완료 후 큐 정리용). 신고 대상 게시글/댓글 삭제와는 무관
    @Transactional
    public void delete(Long reportId) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));
        reportRepository.delete(report);
    }

    // 자기 신고 방지: MEMBER 신고는 대상이 본인, BOARD/COMMENT 신고는 그 글/댓글 작성자가 본인인 경우
    private void requireTargetExistsAndNotSelf(Long memberId, ReportTargetType targetType, Long targetId) {
        switch (targetType) {
            case BOARD -> {
                Board board = boardRepository.findById(targetId)
                        .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
                if (!board.isVisibleTo(memberId)) {
                    throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
                }
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
