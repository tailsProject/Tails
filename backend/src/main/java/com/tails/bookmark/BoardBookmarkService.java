package com.tails.bookmark;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.board.dto.BoardResponse;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.notification.event.BoardBookmarkedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 게시글 북마크 등록 및 해제 비즈니스 로직 처리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardBookmarkService {

    private final BoardBookmarkRepository boardBookmarkRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 이미 북마크했으면 취소, 안 했으면 추가
    @Transactional
    public boolean toggleBookmark(Long memberId, Long boardId) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (!board.isVisibleTo(memberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }

        return boardBookmarkRepository.findByBoardIdAndMemberId(boardId, memberId)
                .map(existing -> {
                    boardBookmarkRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    BoardBookmark bookmark = BoardBookmark.builder()
                            .board(board)
                            .member(memberRepository.getReferenceById(memberId))
                            .build();
                    boardBookmarkRepository.save(bookmark);
                    if (board.getMember() != null && !board.getMember().getId().equals(memberId)) {
                        eventPublisher.publishEvent(new BoardBookmarkedEvent(board.getMember().getId(), memberId, boardId));
                    }
                    return true;
                });
    }

    public Page<BoardResponse> getMyBookmarks(Long memberId, Pageable pageable) {
        return boardBookmarkRepository.findBookmarkedBoardsByMemberId(memberId, pageable)
                .map(BoardResponse::from);
    }
}
