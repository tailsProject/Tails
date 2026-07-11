package com.tails.comment.dto;

import com.tails.comment.Comment;

import java.time.LocalDateTime;
import java.util.List;

// 댓글 응답
public record CommentResponse(
        Long commentId,
        Long boardId,
        Long parentId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies
) {
    public static CommentResponse of(Comment comment, List<CommentResponse> replies) {
        Long authorId = comment.getMember() != null ? comment.getMember().getId() : null;
        String authorNickname = comment.getMember() != null ? comment.getMember().getNickname() : "탈퇴한 회원";
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        String content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        return new CommentResponse(
                comment.getId(),
                comment.getBoard().getId(),
                parentId,
                authorId,
                authorNickname,
                content,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
