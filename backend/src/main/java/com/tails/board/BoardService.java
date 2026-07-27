package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.board.dto.BoardDetailResponse;
import com.tails.board.dto.BoardResponse;
import com.tails.board.dto.BoardUpdateRequest;
import com.tails.board.dto.LikeToggleResponse;
import com.tails.comment.CommentRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.util.FileStorage;
import com.tails.bookmark.BoardBookmarkRepository;
import com.tails.image.Image;
import com.tails.image.ImageRepository;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.member.MemberRole;
import com.tails.notification.event.BoardLikedEvent;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
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
    private final BoardBookmarkRepository boardBookmarkRepository;
    private final MemberRepository memberRepository;
    private final ImageRepository imageRepository;
    private final CommentRepository commentRepository;
    private final FileStorage fileStorage;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public Long create(Long memberId, BoardCreateRequest request) {
        Board board = Board.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .content(request.content())
                .status(BoardStatus.PUBLISHED)
                .build();
        return boardRepository.save(board).getId();
    }

    // 목록/검색/인기순에 노출되지 않는 초안으로 저장
    @Transactional
    public Long createDraft(Long memberId, BoardCreateRequest request) {
        Board board = Board.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .content(request.content())
                .status(BoardStatus.DRAFT)
                .build();
        return boardRepository.save(board).getId();
    }

    // keyword가 있으면 검색, sortBy=popular면 인기순, 둘 다 없으면 기존 최신순 목록
    public Page<BoardResponse> getList(Pageable pageable, String keyword, String sortBy) {
        if (keyword != null && !keyword.isBlank()) {
            return withCommentCounts(boardRepository.searchByKeyword(keyword.trim(), pageable));
        }
        if ("popular".equals(sortBy)) {
            return withCommentCounts(boardRepository.findAllOrderByPopularity(pageable));
        }
        return withCommentCounts(boardRepository.findAllWithMember(pageable));
    }

    // 임시저장 글을 이어쓰기 내용과 함께 발행 전환. 이미 발행된 글이면 거절
    @Transactional
    public void publish(Long memberId, Long boardId, BoardUpdateRequest request) {
        Board board = getBoardOrThrow(boardId);
        requireOwner(board, memberId);
        if (!board.isDraft()) {
            throw new CustomException(ErrorCode.ALREADY_PUBLISHED);
        }
        board.changeTitleAndContent(request.title(), request.content());
        board.publish();
    }

    public Page<BoardResponse> getMyDrafts(Long memberId, Pageable pageable) {
        return withCommentCounts(boardRepository.findByMemberIdAndStatus(memberId, BoardStatus.DRAFT, pageable));
    }

    // 내가 쓴 글 목록 - DRAFT/PUBLISHED 구분 없이 전부 보임(마이페이지)
    public Page<BoardResponse> getMyBoards(Long memberId, Pageable pageable) {
        return withCommentCounts(boardRepository.findByMemberId(memberId, pageable));
    }

    // 게시글 목록에 댓글 수 배치 조회 후 반영
    private Page<BoardResponse> withCommentCounts(Page<Board> boards) {
        List<Long> boardIds = boards.getContent().stream().map(Board::getId).toList();
        Map<Long, Integer> commentCounts = commentRepository.countByBoardIds(boardIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
        return boards.map(board -> BoardResponse.from(board, commentCounts.getOrDefault(board.getId(), 0)));
    }

    // 게시글 상세 조회 + 조회수 1 증가. DRAFT는 작성자 본인만 조회 가능(그 외엔 BOARD_NOT_FOUND)
    @Transactional
    public BoardDetailResponse getDetail(Long boardId, Long currentMemberId) {
        Board board = getBoardOrThrow(boardId);
        if (!board.isVisibleTo(currentMemberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        boardRepository.increaseViewCount(boardId);

        boolean liked = currentMemberId != null && boardLikeRepository.findByBoardIdAndMemberId(boardId, currentMemberId).isPresent();
        boolean bookmarked = currentMemberId != null && boardBookmarkRepository.findByBoardIdAndMemberId(boardId, currentMemberId).isPresent();

        return BoardDetailResponse.of(board, board.getViewCount() + 1, liked, bookmarked);
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
        requireOwnerOrAdmin(board, memberId);

        // DB에서 삭제되지 않는 파일은 직접 삭제
        for (Image image : imageRepository.findByBoardIdOrderByCreatedAtAsc(boardId)) {
            fileStorage.deleteAfterCommit(image.getStoredFileName());
        }

        boardLikeRepository.deleteByBoardId(boardId);
        boardBookmarkRepository.deleteByBoardId(boardId);
        boardRepository.delete(board);
    }

    // 좋아요 추가/취소 토글
    @Transactional
    public LikeToggleResponse toggleLike(Long memberId, Long boardId) {
        Board board = getBoardOrThrow(boardId);
        if (!board.isVisibleTo(memberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }

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

    // 작성자 본인인지 확인. DRAFT 글은 먼저 isVisibleTo로 존재 자체를 숨겨서(404), 조회/좋아요와
    // 마찬가지로 남의 DRAFT 글에 대한 쓰기 시도에서도 존재 여부가 403으로 새지 않도록 한다
    private void requireOwner(Board board, Long memberId) {
        if (!board.isVisibleTo(memberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        if (board.getMember() == null || !board.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_BOARD_OWNER);
        }
    }

    // 삭제는 작성자 본인 또는 ADMIN이 할 수 있음 (신고된 게시글 강제 삭제 용도)
    private void requireOwnerOrAdmin(Board board, Long memberId) {
        if (board.getMember() != null && board.getMember().getId().equals(memberId)) {
            return;
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_BOARD_OWNER));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new CustomException(ErrorCode.NOT_BOARD_OWNER);
        }
    }

    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }
}
