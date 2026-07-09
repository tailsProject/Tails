package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.board.dto.BoardDetailResponse;
import com.tails.board.dto.BoardResponse;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import lombok.RequiredArgsConstructor;
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
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, BoardCreateRequest request) {
        Board board = Board.builder()
                .member(memberRepository.getReferenceById(memberId))
                .title(request.title())
                .content(request.content())
                .build();
        return boardRepository.save(board).getId();
    }

    public Page<BoardResponse> getList(Pageable pageable) {
        return boardRepository.findAll(pageable).map(BoardResponse::from);
    }

    // 호출될 때마다 조회수 1 증가 (중복 방지 없이 단순 카운트)
    @Transactional
    public BoardDetailResponse getDetail(Long boardId) {
        Board board = getBoardOrThrow(boardId);
        board.increaseViewCount();
        return BoardDetailResponse.of(board);
    }

    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }
}
