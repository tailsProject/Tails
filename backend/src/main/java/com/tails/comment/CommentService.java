package com.tails.comment;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentResponse;
import com.tails.comment.dto.CommentUpdateRequest;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.MemberRepository;
import com.tails.notification.event.CommentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// 게시글 댓글 비즈니스 로직
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final BoardRepository boardRepository;
    private final MemberRepository memberRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 답글에는 다시 답글을 달 수 없도록 항상 최상위 댓글을 부모로 설정
    @Transactional
    public Long create(Long memberId, Long boardId, CommentCreateRequest request) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (!board.isVisibleTo(memberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }

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
        Long commentId = commentRepository.save(comment).getId();

        if (board.getMember() != null && !board.getMember().getId().equals(memberId)) {
            eventPublisher.publishEvent(new CommentCreatedEvent(board.getMember().getId(), memberId, boardId));
        }

        return commentId;
    }

    // 최상위 댓글 기준으로 페이징하고, 그 페이지의 답글만 별도로 모아 트리로 묶어 반환
    public Page<CommentResponse> getList(Long boardId, Long currentMemberId, Pageable pageable) {
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
        if (!board.isVisibleTo(currentMemberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        Page<Comment> roots = commentRepository.findByBoardIdAndParentIsNull(boardId, pageable);

        List<Long> rootIds = roots.getContent().stream().map(Comment::getId).toList();
        Map<Long, List<Comment>> repliesByParentId = rootIds.isEmpty()
                ? Map.of()
                : commentRepository.findByParentIdInOrderByCreatedAtAsc(rootIds).stream()
                        .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        return roots.map(root -> CommentResponse.of(root, toResponses(repliesByParentId.getOrDefault(root.getId(), List.of()))));
    }

    // 작성자 본인만 수정 가능
    @Transactional
    public void update(Long memberId, Long boardId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getCommentInBoardOrThrow(boardId, commentId);
        requireOwner(comment, memberId);
        comment.changeContent(request.content());
    }

    // 작성자 본인만 삭제 가능. 실제로 지우지 않고 "삭제된 댓글입니다"로만 표시
    @Transactional
    public void delete(Long memberId, Long boardId, Long commentId) {
        Comment comment = getCommentInBoardOrThrow(boardId, commentId);
        requireOwner(comment, memberId);
        comment.softDelete();
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
