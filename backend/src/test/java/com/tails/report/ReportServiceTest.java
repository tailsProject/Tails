package com.tails.report;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.board.BoardStatus;
import com.tails.comment.Comment;
import com.tails.comment.CommentRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.report.dto.ReportCreateRequest;
import com.tails.review.ReviewRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

// ReportService 단위테스트 - 자기 자신 신고 방지/중복 신고 방지
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReportService reportService;

    private Member newMember(Long id) {
        Member member = Member.builder().email(id + "@test.com").password("x").nickname("n" + id).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    @Test
    void 본인_게시글을_신고하면_CANNOT_REPORT_SELF() {
        Member owner = newMember(1L);
        Board board = Board.builder().member(owner).title("t").content("c").status(BoardStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(board, "id", 100L);
        when(boardRepository.findById(100L)).thenReturn(Optional.of(board));

        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.BOARD, 100L, "reason");

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_SELF);
    }

    @Test
    void 본인_댓글을_신고하면_CANNOT_REPORT_SELF() {
        Member owner = newMember(1L);
        Board board = Board.builder().member(owner).title("t").content("c").status(BoardStatus.PUBLISHED).build();
        Comment comment = Comment.builder().board(board).member(owner).content("c").build();
        ReflectionTestUtils.setField(comment, "id", 200L);
        when(commentRepository.findById(200L)).thenReturn(Optional.of(comment));

        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.COMMENT, 200L, "reason");

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_SELF);
    }

    @Test
    void 자기_자신을_회원으로_신고하면_CANNOT_REPORT_SELF() {
        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.MEMBER, 1L, "reason");

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.CANNOT_REPORT_SELF);
    }

    @Test
    void 같은_대상을_이미_신고했으면_DUPLICATE_REPORT() {
        Member other = newMember(2L);
        Board board = Board.builder().member(other).title("t").content("c").status(BoardStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(board, "id", 100L);
        when(boardRepository.findById(100L)).thenReturn(Optional.of(board));
        when(reportRepository.existsByReporterIdAndTargetTypeAndTargetId(1L, ReportTargetType.BOARD, 100L)).thenReturn(true);

        ReportCreateRequest request = new ReportCreateRequest(ReportTargetType.BOARD, 100L, "reason");

        assertThatThrownBy(() -> reportService.create(1L, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_REPORT);
    }
}
