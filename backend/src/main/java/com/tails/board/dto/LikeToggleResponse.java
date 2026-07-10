package com.tails.board.dto;

// 좋아요 토글 결과 응답 DTO (처리 후 좋아요 상태와 개수 반환)
public record LikeToggleResponse(boolean liked, int likeCount) {
}
