package com.tails.image;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// 이미지 조회 레포지토리
public interface ImageRepository extends JpaRepository<Image, Long> {
    // 게시글 이미지 조회
    List<Image> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    // 게시글 이미지 조회 - sequence 기준(대표 이미지가 항상 먼저 오도록)
    List<Image> findByBoardIdOrderBySequenceAsc(Long boardId);
    // 후기 이미지 조회. Review의 PK 필드명이 reviewId(id가 아님)라서, "review 필드를 타고
    // 들어가 그 안의 reviewId 필드"라는 걸 언더스코어로 명시해야 한다
    List<Image> findByReview_ReviewIdOrderByCreatedAtAsc(Long reviewId);
    // 새로 업로드되는 이미지의 시작 sequence(기존 개수만큼 이어붙임)
    long countByBoardId(Long boardId);
    long countByReview_ReviewId(Long reviewId);
}
