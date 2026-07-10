package com.tails.comment.dto;

import com.tails.comment.Comment;

import java.time.LocalDateTime;

// 댓글 응답
public record CommentResponse(
        Long commentId,
        Long boardId,
        Long authorId,
        String authorNickname,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static CommentResponse from(Comment comment) {
        Long authorId = comment.getMember() != null ? comment.getMember().getId() : null;
        String authorNickname = comment.getMember() != null ? comment.getMember().getNickname() : "탈퇴한 회원";
        return new CommentResponse(
                comment.getId(),
                comment.getBoard().getId(),
                authorId,
                authorNickname,
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}
