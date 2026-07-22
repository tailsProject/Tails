package com.tails.image.dto;

import com.tails.image.Image;

import java.time.LocalDateTime;

// 게시글 이미지 조회 응답 DTO

public record ImageResponse(
        Long imageId,
        String imageUrl,
        String originalFileName,
        int sequence,
        LocalDateTime createdAt
) {
    // 저장된 파일명으로 이미지 접근 경로 생성
    private static final String URL_PREFIX = "/uploads/";

    public static ImageResponse from(Image image) {
        return new ImageResponse(
                image.getId(),
                URL_PREFIX + image.getStoredFileName(),
                image.getOriginalFileName(),
                image.getSequence(),
                image.getCreatedAt()
        );
    }
}
