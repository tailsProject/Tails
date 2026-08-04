package com.tails.comment.dto;

import org.springframework.data.domain.Page;

// 댓글 목록 응답, 답글 포함 총 댓글 수를 별도로 내려줌
// comments는 최상위 댓글만 페이징(답글은 페이징 대상 아님), totalCommentCount는 답글까지 합산한 실제 전체 개수
public record CommentListResponse(
        Page<CommentResponse> comments,
        long totalCommentCount
) {
}
