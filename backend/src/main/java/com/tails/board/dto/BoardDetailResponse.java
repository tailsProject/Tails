package com.tails.board.dto;

import com.tails.board.Board;

import java.time.LocalDateTime;

// 게시글 상세(GET /api/boards/{boardId}) 응답
public record BoardDetailResponse(
        Long boardId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        int viewCount,
        int likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    
    public static BoardDetailResponse of(Board board, int viewCount) {
        Long authorId = board.getMember() != null ? board.getMember().getId() : null;
        String authorNickname = board.getMember() != null ? board.getMember().getNickname() : "탈퇴한 회원";
        return new BoardDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                authorId,
                authorNickname,
                viewCount,
                board.getLikeCount(),
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
