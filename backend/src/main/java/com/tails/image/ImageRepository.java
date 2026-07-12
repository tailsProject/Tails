package com.tails.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 이미지 조회 레포지토리
public interface ImageRepository extends JpaRepository<Image, Long> {
    // 게시글 이미지 조회
    List<Image> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    // 후기 이미지 조회
    List<Image> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
}
