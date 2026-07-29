package com.tails.board.dto;

import com.tails.board.Board;
import com.tails.board.BoardStatus;

import java.time.LocalDateTime;

// 게시글 목록 응답. 본문 전체(content)는 상세 조회에서만 내려주고, 목록에는 짧은 미리보기(excerpt)만 담음
public record BoardResponse(
        Long boardId,
        String title,
        String excerpt,
        String thumbnailUrl,
        String authorNickname,
        String authorProfileImg,
        int viewCount,
        int likeCount,
        int commentCount,
        BoardStatus status,
        LocalDateTime createdAt
) {
    private static final int EXCERPT_MAX_LENGTH = 80;

    // 탈퇴한 작성자는 "탈퇴한 회원"으로 표시, commentCount/thumbnailUrl은 배치 조회 결과를 별도 인자로 받음
    public static BoardResponse from(Board board, int commentCount, String thumbnailUrl) {
        String authorNickname = board.getMember() != null ? board.getMember().getNickname() : "탈퇴한 회원";
        String authorProfileImg = board.getMember() != null ? board.getMember().getProfileImg() : null;
        return new BoardResponse(
                board.getId(),
                board.getTitle(),
                buildExcerpt(board.getContent()),
                thumbnailUrl,
                authorNickname,
                authorProfileImg,
                board.getViewCount(),
                board.getLikeCount(),
                commentCount,
                board.getStatus(),
                board.getCreatedAt()
        );
    }

    private static String buildExcerpt(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.strip();
        if (normalized.length() <= EXCERPT_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, EXCERPT_MAX_LENGTH) + "…";
    }
}
