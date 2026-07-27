package com.tails.board.dto;

import com.tails.board.Board;
import com.tails.board.BoardStatus;

import java.time.LocalDateTime;

// 게시글 목록 응답. 본문(content)은 상세 조회에서만 내려줌
public record BoardResponse(
        Long boardId,
        String title,
        String authorNickname,
        String authorProfileImg,
        int viewCount,
        int likeCount,
        BoardStatus status,
        LocalDateTime createdAt
) {
    // 탈퇴한 작성자는 "탈퇴한 회원"으로 표시
    public static BoardResponse from(Board board) {
        String authorNickname = board.getMember() != null ? board.getMember().getNickname() : "탈퇴한 회원";
        String authorProfileImg = board.getMember() != null ? board.getMember().getProfileImg() : null;
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                authorNickname,
                authorProfileImg,
                board.getViewCount(),
                board.getLikeCount(),
                board.getStatus(),
                board.getCreatedAt()
        );
    }
}
