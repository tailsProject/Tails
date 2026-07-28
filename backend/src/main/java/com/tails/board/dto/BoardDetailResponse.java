package com.tails.board.dto;

import com.tails.board.Board;
import com.tails.board.BoardStatus;

import java.time.LocalDateTime;

// 게시글 상세(GET /api/boards/{boardId}) 응답
public record BoardDetailResponse(
        Long boardId,
        String title,
        String content,
        Long authorId,
        String authorNickname,
        String authorProfileImg,
        int viewCount,
        int likeCount,
        BoardStatus status,
        boolean liked,
        boolean bookmarked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static BoardDetailResponse of(Board board, int viewCount, boolean liked, boolean bookmarked) {
        Long authorId = board.getMember() != null ? board.getMember().getId() : null;
        String authorNickname = board.getMember() != null ? board.getMember().getNickname() : "탈퇴한 회원";
        String authorProfileImg = board.getMember() != null ? board.getMember().getProfileImg() : null;
        return new BoardDetailResponse(
                board.getId(),
                board.getTitle(),
                board.getContent(),
                authorId,
                authorNickname,
                authorProfileImg,
                viewCount,
                board.getLikeCount(),
                board.getStatus(),
                liked,
                bookmarked,
                board.getCreatedAt(),
                board.getUpdatedAt()
        );
    }
}
