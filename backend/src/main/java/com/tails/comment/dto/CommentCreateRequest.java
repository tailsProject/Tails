package com.tails.comment.dto;

import jakarta.validation.constraints.NotBlank;

// 댓글 작성 요청
public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content,

        // 최상위 댓글이면 null, 답글이면 부모 댓글 id
        Long parentId
) {
}
