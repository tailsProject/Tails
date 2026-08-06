package com.tails.review.dto;

import com.tails.review.Review;
import java.time.LocalDateTime;

// 메인페이지 "최근 리뷰" 미리보기 응답. 장소명을 함께 내려준다는 점이 ReviewResponse와 다름
public record RecentReviewResponse(
        Long reviewId,
        Long placeId,
        String placeName,
        String authorNickname,
        String authorProfileImg,
        int rating,
        String content,
        LocalDateTime createdAt
) {
    public static RecentReviewResponse from(Review review) {
        String authorNickname = review.getMember() != null ? review.getMember().getNickname() : "탈퇴한 회원";
        String authorProfileImg = review.getMember() != null ? review.getMember().getProfileImg() : null;
        return new RecentReviewResponse(
                review.getReviewId(),
                review.getPlace().getPlaceId(),
                review.getPlace().getPlaceName(),
                authorNickname,
                authorProfileImg,
                review.getRating(),
                review.getContent(),
                review.getCreatedAt()
        );
    }
}
