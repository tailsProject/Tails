package com.tails.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 댓글 작성 요청
public record CommentCreateRequest(
        @NotBlank(message = "댓글 내용을 입력해주세요.")
        @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다.")
        String content,

        // 최상위 댓글이면 null, 답글이면 부모 댓글 id
        Long parentId
) {
}
