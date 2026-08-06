package com.tails.board.dto;

import com.tails.board.Board;
import com.tails.board.BoardStatus;
import com.tails.board.ContentFormat;

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
                buildExcerpt(board.getContent(), board.getContentFormat()),
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

    // PLAIN 글의 인라인 이미지 마커와 HTML 글의 태그를 제거해 순수 텍스트만 추출
    private static final java.util.regex.Pattern IMAGE_MARKER = java.util.regex.Pattern.compile("\\[\\[img:\\d+]]");
    private static final java.util.regex.Pattern HTML_TAG = java.util.regex.Pattern.compile("<[^>]+>");

    private static String buildExcerpt(String content, ContentFormat contentFormat) {
        if (content == null) {
            return "";
        }
        String stripped = contentFormat == ContentFormat.HTML
                ? HTML_TAG.matcher(content).replaceAll(" ")
                : IMAGE_MARKER.matcher(content).replaceAll(" ");
        String normalized = stripped.strip().replaceAll("\\s+", " ");
        if (normalized.length() <= EXCERPT_MAX_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, EXCERPT_MAX_LENGTH) + "…";
    }
}
