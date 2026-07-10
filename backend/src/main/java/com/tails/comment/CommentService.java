package com.tails.comment;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentResponse;
import com.tails.comment.dto.CommentUpdateRequest;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 게시글 댓글 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;

    // 답글에는 다시 답글을 달 수 없도록 항상 최상위 댓글을 부모로 설정
    @Transactional
    public Long create(Long memberId, Long boardId, CommentCreateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));

        Comment parent = null;
        if (request.parentId() != null) {
            Comment target = commentRepository.findById(request.parentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
            // 다른 게시글의 댓글에는 답글을 작성할 수 없음
            if (!target.getBoard().getId().equals(boardId)) {
                throw new CustomException(ErrorCode.PARENT_COMMENT_BOARD_MISMATCH);
            }
            parent = target.getParent() != null ? target.getParent() : target;
        }

        Comment comment = Comment.builder()
                .board(board)
                .member(memberRepository.getReferenceById(memberId))
                .parent(parent)
                .content(request.content())
                .build();
        return commentRepository.save(comment).getId();
    }

    // 최상위 댓글 기준으로 답글을 묶어 반환
    public List<CommentResponse> getList(Long boardId) {
        List<Comment> comments = commentRepository.findByBoardIdOrderByCreatedAtAsc(boardId);

        // 부모 댓글과 답글을 분리
        Map<Long, List<Comment>> repliesByParentId = new HashMap<>();
        List<Comment> roots = new ArrayList<>();
        for (Comment comment : comments) {
            if (comment.getParent() == null) {
                roots.add(comment);
            } else {
                repliesByParentId.computeIfAbsent(comment.getParent().getId(), key -> new ArrayList<>()).add(comment);
            }
        }

        return roots.stream()
                .map(root -> CommentResponse.of(root, toResponses(repliesByParentId.getOrDefault(root.getId(), List.of()))))
                .toList();
    }

    // 작성자 본인만 수정 가능
    @Transactional
    public void update(Long memberId, Long boardId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getCommentInBoardOrThrow(boardId, commentId);
        requireOwner(comment, memberId);
        comment.changeContent(request.content());
    }

    private List<CommentResponse> toResponses(List<Comment> comments) {
        return comments.stream().map(comment -> CommentResponse.of(comment, List.of())).toList();
    }

    private void requireOwner(Comment comment, Long memberId) {
        if (comment.getMember() == null || !comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_OWNER);
        }
    }

    // commentId로 조회하고 URL의 boardId와 다른 게시글 소속이면 못 찾은 것처럼 처리
    private Comment getCommentInBoardOrThrow(Long boardId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        return comment;
    }
}
