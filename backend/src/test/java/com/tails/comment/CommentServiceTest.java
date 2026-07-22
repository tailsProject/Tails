package com.tails.comment;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.board.BoardStatus;
import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentUpdateRequest;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// CommentService 단위테스트 - 답글의 답글 승격/타 게시글 답글 차단/삭제된 댓글 수정 차단
@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private BoardRepository boardRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CommentService commentService;

    private Member newMember(Long id) {
        Member member = Member.builder().email(id + "@test.com").password("x").nickname("n" + id).build();
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Board newBoard(Long boardId) {
        Board board = Board.builder().member(null).title("t").content("c").status(BoardStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }

    private Comment newComment(Long commentId, Board board, Comment parent) {
        Comment comment = Comment.builder().board(board).member(newMember(1L)).parent(parent).content("c").build();
        ReflectionTestUtils.setField(comment, "id", commentId);
        return comment;
    }

    @Test
    void 답글에_답글을_달면_최상위_댓글이_부모로_설정된다() {
        Board board = newBoard(1L);
        Comment root = newComment(10L, board, null);
        Comment reply = newComment(11L, board, root);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board));
        when(commentRepository.findById(11L)).thenReturn(Optional.of(reply));
        when(memberRepository.getReferenceById(2L)).thenReturn(newMember(2L));
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        commentService.create(2L, 1L, new CommentCreateRequest("답글의 답글", 11L));

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentRepository).save(captor.capture());
        assertThat(captor.getValue().getParent()).isEqualTo(root);
    }

    @Test
    void 다른_게시글_소속_댓글에_답글을_달면_PARENT_COMMENT_BOARD_MISMATCH() {
        Board board1 = newBoard(1L);
        Board board2 = newBoard(2L);
        Comment otherBoardComment = newComment(20L, board2, null);
        when(boardRepository.findById(1L)).thenReturn(Optional.of(board1));
        when(commentRepository.findById(20L)).thenReturn(Optional.of(otherBoardComment));

        assertThatThrownBy(() -> commentService.create(2L, 1L, new CommentCreateRequest("c", 20L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PARENT_COMMENT_BOARD_MISMATCH);
    }

    @Test
    void 삭제된_댓글은_수정할_수_없다() {
        Board board = newBoard(1L);
        Member owner = newMember(1L);
        Comment comment = Comment.builder().board(board).member(owner).content("c").build();
        ReflectionTestUtils.setField(comment, "id", 10L);
        comment.softDelete();
        when(commentRepository.findById(10L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.update(1L, 1L, 10L, new CommentUpdateRequest("edited")))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.COMMENT_NOT_FOUND);
    }
}
