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
        String authorProfileImg,
        String content,
        int likeCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<CommentResponse> replies
) {
    // likedByMe는 로그인 회원의 좋아요 여부, 비로그인 조회면 항상 false
    public static CommentResponse of(Comment comment, boolean likedByMe, List<CommentResponse> replies) {
        Long authorId = comment.getMember() != null ? comment.getMember().getId() : null;
        String authorNickname = comment.getMember() != null ? comment.getMember().getNickname() : "탈퇴한 회원";
        String authorProfileImg = comment.getMember() != null ? comment.getMember().getProfileImg() : null;
        Long parentId = comment.getParent() != null ? comment.getParent().getId() : null;
        String content = comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent();
        return new CommentResponse(
                comment.getId(),
                comment.getBoard().getId(),
                parentId,
                authorId,
                authorNickname,
                authorProfileImg,
                content,
                comment.getLikeCount(),
                likedByMe,
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                replies
        );
    }
}
