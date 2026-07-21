package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.board.dto.BoardDetailResponse;
import com.tails.board.dto.BoardResponse;
import com.tails.board.dto.BoardUpdateRequest;
import com.tails.board.dto.LikeToggleResponse;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.image.Image;
import com.tails.image.ImageRepository;
import com.tails.member.MemberRepository;
import com.tails.notification.event.BoardLikedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 게시글 관련 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardLikeRepository boardLikeRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(Long memberId, BoardCreateRequest request) {
        Board board = Board.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .content(request.content())
                .build();
        return boardRepository.save(board).getId();
    }

    // keyword가 있으면 검색, sortBy=popular면 인기순, 둘 다 없으면 기존 최신순 목록
    public Page<BoardResponse> getList(Pageable pageable, String keyword, String sortBy) {
        if (keyword != null && !keyword.isBlank()) {
            return boardRepository.searchByKeyword(keyword.trim(), pageable).map(BoardResponse::from);
        }
        if ("popular".equals(sortBy)) {
            return boardRepository.findAllOrderByPopularity(pageable).map(BoardResponse::from);
        }
        return boardRepository.findAllWithMember(pageable).map(BoardResponse::from);
    }

    // 게시글 상세 조회 + 조회수 1 증가
    @Transactional
    public BoardDetailResponse getDetail(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        boardRepository.increaseViewCount(boardId);
        return BoardDetailResponse.of(board, board.getViewCount() + 1);
    }

    @Transactional
    public void update(Long memberId, Long boardId, BoardUpdateRequest request) {
        Board board = getBoardOrThrow(boardId);
        requireOwner(board, memberId);
        board.changeTitleAndContent(request.title(), request.content());
    }

    @Transactional
    public void delete(Long memberId, Long boardId) {
        Board board = getBoardOrThrow(boardId);
        requireOwner(board, memberId);

        // DB에서 삭제되지 않는 파일은 직접 삭제
        for (Image image : imageRepository.findByBoardIdOrderByCreatedAtAsc(boardId)) {
            fileStorage.deleteAfterCommit(image.getStoredFileName());
        }

        boardRepository.delete(board);
    }

    // 좋아요 추가/취소 토글
    @Transactional
    public LikeToggleResponse toggleLike(Long memberId, Long boardId) {
        Board board = getBoardOrThrow(boardId);

        boolean liked = boardLikeRepository.findByBoardIdAndMemberId(boardId, memberId)
                .map(existing -> {
                    boardLikeRepository.delete(existing);
                    board.decreaseLikeCount();
                    return false;
                })
                .orElseGet(() -> {
                    BoardLike like = BoardLike.builder()
                            .board(board)
                            .member(memberRepository.getReferenceById(memberId))
                            .build();
                    boardLikeRepository.save(like);
                    board.increaseLikeCount();
                    if (board.getMember() != null && !board.getMember().getId().equals(memberId)) {
                        eventPublisher.publishEvent(new BoardLikedEvent(board.getMember().getId(), memberId, boardId));
                    }
                    return true;
                });

        return new LikeToggleResponse(liked, board.getLikeCount());
    }

    // 작성자 본인인지 확인
    private void requireOwner(Board board, Long memberId) {
        if (board.getMember() == null || !board.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_BOARD_OWNER);
        }
    }

    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }
}
