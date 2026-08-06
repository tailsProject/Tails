package com.tails.board;

import com.tails.board.dto.LikeToggleResponse;
import com.tails.bookmark.BoardBookmarkRepository;
import com.tails.comment.CommentRepository;
import com.tails.common.util.FileStorage;
import com.tails.image.ImageRepository;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.notification.event.BoardLikedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// BoardService.toggleLike() 알림 발행 조건(본인 글 제외/취소 시 미발행/탈퇴 회원 제외) 단위테스트
@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock
    private BoardRepository boardRepository;
    @Mock
    private BoardLikeRepository boardLikeRepository;
    @Mock
    private BoardBookmarkRepository boardBookmarkRepository;
    @Mock
    private MemberRepository memberRepository;
    @Mock
    private ImageRepository imageRepository;
    @Mock
    private CommentRepository commentRepository;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private BoardService boardService;

    private Board newBoard(Long boardId, Member owner) {
        Board board = Board.builder().member(owner).title("t").content("c").status(BoardStatus.PUBLISHED).build();
        ReflectionTestUtils.setField(board, "id", boardId);
        return board;
    }

    private Member newMember(Long memberId) {
        Member member = Member.builder().email(memberId + "@test.com").password("x").nickname("n" + memberId).build();
        ReflectionTestUtils.setField(member, "id", memberId);
        return member;
    }

    private void setUpService() {
        boardService = new BoardService(boardRepository, boardLikeRepository, boardBookmarkRepository,
                memberRepository, imageRepository, commentRepository, fileStorage, eventPublisher);
    }

    @Test
    void 다른_사람_글에_처음_좋아요를_누르면_알림이벤트가_발행된다() {
        setUpService();
        Member owner = newMember(1L);
        Board board = newBoard(10L, owner);
        when(boardRepository.findById(10L)).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByBoardIdAndMemberId(10L, 2L)).thenReturn(Optional.empty());
        when(memberRepository.getReferenceById(2L)).thenReturn(newMember(2L));

        LikeToggleResponse response = boardService.toggleLike(2L, 10L);

        assertThat(response.liked()).isTrue();
        verify(eventPublisher, times(1)).publishEvent(any(BoardLikedEvent.class));
    }

    @Test
    void 이미_누른_좋아요를_취소할_때는_알림이벤트가_발행되지_않는다() {
        setUpService();
        Member owner = newMember(1L);
        Board board = newBoard(10L, owner);
        Member liker = newMember(2L);
        BoardLike existing = BoardLike.builder().board(board).member(liker).build();
        when(boardRepository.findById(10L)).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByBoardIdAndMemberId(10L, 2L)).thenReturn(Optional.of(existing));

        LikeToggleResponse response = boardService.toggleLike(2L, 10L);

        assertThat(response.liked()).isFalse();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 본인_글에_본인이_좋아요를_눌러도_알림이벤트가_발행되지_않는다() {
        setUpService();
        Member owner = newMember(1L);
        Board board = newBoard(10L, owner);
        when(boardRepository.findById(10L)).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByBoardIdAndMemberId(10L, 1L)).thenReturn(Optional.empty());
        when(memberRepository.getReferenceById(1L)).thenReturn(owner);

        LikeToggleResponse response = boardService.toggleLike(1L, 10L);

        assertThat(response.liked()).isTrue();
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 작성자가_탈퇴한_글에_좋아요를_눌러도_알림이벤트가_발행되지_않는다() {
        setUpService();
        Board board = newBoard(10L, null);
        when(boardRepository.findById(10L)).thenReturn(Optional.of(board));
        when(boardLikeRepository.findByBoardIdAndMemberId(10L, 2L)).thenReturn(Optional.empty());
        when(memberRepository.getReferenceById(2L)).thenReturn(newMember(2L));

        LikeToggleResponse response = boardService.toggleLike(2L, 10L);

        assertThat(response.liked()).isTrue();
        verify(eventPublisher, never()).publishEvent(any());
    }
}
