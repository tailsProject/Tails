package com.tails.comment;

import com.tails.board.Board;
import com.tails.board.BoardRepository;
import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentResponse;
import com.tails.comment.dto.CommentUpdateRequest;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.member.MemberRole;
import com.tails.notification.event.CommentCreatedEvent;
import com.tails.notification.event.CommentRepliedEvent;
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

    // 답글에도 다시 답글을 달 수 있음 (depth 제한 없이 무한 중첩)
    @Transactional
    public Long create(Long memberId, Long boardId, CommentCreateRequest request) {
        Board board = getBoardOrThrow(boardId);
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
            parent = target;
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
        // 답글 대상 댓글 작성자에게 알림
        if (parent != null && parent.getMember() != null && !parent.getMember().getId().equals(memberId)) {
            eventPublisher.publishEvent(new CommentRepliedEvent(parent.getMember().getId(), memberId, boardId));
        }

        return commentId;
    }

    // 최상위 댓글 기준으로 페이징하고, 게시글 전체 답글(모든 depth)을 부모 id로 묶어 재귀적으로 트리를 구성해 반환
    public Page<CommentResponse> getList(Long boardId, Long currentMemberId, Pageable pageable) {
        Board board = getBoardOrThrow(boardId);
        if (!board.isVisibleTo(currentMemberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        Page<Comment> roots = commentRepository.findByBoardIdAndParentIsNull(boardId, pageable);

        Map<Long, List<Comment>> childrenByParentId = commentRepository.findByBoardIdAndParentIsNotNullOrderByCreatedAtAsc(boardId).stream()
                .collect(Collectors.groupingBy(comment -> comment.getParent().getId()));

        return roots.map(root -> toResponseWithReplies(root, childrenByParentId));
    }

    // 댓글 하나를 답글까지 재귀적으로 응답 DTO로 변환 (답글의 답글도 depth 제한 없이 포함)
    private CommentResponse toResponseWithReplies(Comment comment, Map<Long, List<Comment>> childrenByParentId) {
        List<CommentResponse> replies = childrenByParentId.getOrDefault(comment.getId(), List.of()).stream()
                .map(child -> toResponseWithReplies(child, childrenByParentId))
                .toList();
        return CommentResponse.of(comment, replies);
    }

    // 작성자 본인만 수정 가능, 삭제된 댓글은 수정 불가
    @Transactional
    public void update(Long memberId, Long boardId, Long commentId, CommentUpdateRequest request) {
        Comment comment = getCommentInBoardOrThrow(boardId, commentId, memberId);
        requireOwner(comment, memberId);
        if (comment.isDeleted()) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        comment.changeContent(request.content());
    }

    // 작성자 본인 또는 ADMIN이 삭제 가능. 실제로 지우지 않고 "삭제된 댓글입니다"로만 표시
    @Transactional
    public void delete(Long memberId, Long boardId, Long commentId) {
        Comment comment = getCommentInBoardOrThrow(boardId, commentId, memberId);
        requireOwnerOrAdmin(comment, memberId);
        comment.softDelete();
    }

    private void requireOwner(Comment comment, Long memberId) {
        if (comment.getMember() == null || !comment.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_COMMENT_OWNER);
        }
    }

    // 삭제는 작성자 본인 또는 ADMIN이 할 수 있음 (신고된 댓글 강제 삭제 용도)
    private void requireOwnerOrAdmin(Comment comment, Long memberId) {
        if (comment.getMember() != null && comment.getMember().getId().equals(memberId)) {
            return;
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_COMMENT_OWNER));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new CustomException(ErrorCode.NOT_COMMENT_OWNER);
        }
    }

    private Board getBoardOrThrow(Long boardId) {
        return boardRepository.findById(boardId)
                .orElseThrow(() -> new CustomException(ErrorCode.BOARD_NOT_FOUND));
    }

    // commentId로 조회하고 URL의 boardId와 다른 게시글 소속이면 못 찾은 것처럼 처리.
    // 댓글이 달린 게시글이 DRAFT라 이 memberId에게 안 보이면(isVisibleTo) create()와 동일하게
    // BOARD_NOT_FOUND로 처리해서, 남의 DRAFT 글 댓글에 403(존재 유출) 대신 404가 나가게 한다
    private Comment getCommentInBoardOrThrow(Long boardId, Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getBoard().getId().equals(boardId)) {
            throw new CustomException(ErrorCode.COMMENT_NOT_FOUND);
        }
        if (!comment.getBoard().isVisibleTo(memberId)) {
            throw new CustomException(ErrorCode.BOARD_NOT_FOUND);
        }
        return comment;
    }
}
