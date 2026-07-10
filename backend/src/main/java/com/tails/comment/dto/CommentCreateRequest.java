package com.tails.comment.dto;

import jakarta.validation.constraints.NotBlank;

// 댓글 작성 요청
public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        String content
) {
}
