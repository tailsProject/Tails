package com.tails.review.dto;

import com.tails.review.Review;

import java.time.LocalDateTime;

// 리뷰 목록의 각 항목 응답
public record ReviewResponse(
        Long reviewId,
        Long authorId,
        String authorNickname,
        int rating,
        String content,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    // 탈퇴한 작성자는 Board/Comment와 동일하게 "탈퇴한 회원"으로 표시
    public static ReviewResponse from(Review review) {
        Long authorId = review.getMember() != null ? review.getMember().getId() : null;
        String authorNickname = review.getMember() != null ? review.getMember().getNickname() : "탈퇴한 회원";
        return new ReviewResponse(
                review.getReviewId(),
                authorId,
                authorNickname,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt()
        );
    }
}
