package com.tails.comment;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentResponse;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// 게시글 댓글 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public Long create(Long memberId, Long boardId, CommentCreateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        Comment comment = Comment.builder()
                .board(board)
                .member(memberRepository.getReferenceById(memberId))
                .content(request.content())
                .build();
        return commentRepository.save(comment).getId();
    }

    public List<CommentResponse> getList(Long boardId) {
        return commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId).stream()
                .map(CommentResponse::from)
                .toList();
    }
}
