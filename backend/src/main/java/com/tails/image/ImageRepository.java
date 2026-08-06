package com.tails.image;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

// 이미지 조회 레포지토리
public interface ImageRepository extends JpaRepository<Image, Long> {
    // 게시글 이미지 조회
    List<Image> findByBoardIdOrderByCreatedAtAsc(Long boardId);
    // 게시글 이미지 조회 - sequence 기준(대표 이미지가 항상 먼저 오도록)
    List<Image> findByBoardIdOrderBySequenceAsc(Long boardId);
    // 게시글 목록에서 대표 이미지(sequence=0)만 배치로 조회(N+1 방지). 이미지가 없는 게시글은 결과에서 빠짐
    List<Image> findByBoardIdInAndSequence(List<Long> boardIds, int sequence);
    // 후기 이미지 조회. Review의 PK 필드명이 reviewId(id가 아님)라서, "review 필드를 타고
    // 들어가 그 안의 reviewId 필드"라는 걸 언더스코어로 명시해야 한다
    List<Image> findByReview_ReviewIdOrderByCreatedAtAsc(Long reviewId);

    // 새로 업로드되는 이미지의 시작 sequence 계산용. count 기반으로 하면 중간 이미지를 삭제한
    // 뒤 업로드할 때 남아있는 sequence와 충돌할 수 있어 실제 최댓값 + 1을 써야 함
    @Query("SELECT COALESCE(MAX(i.sequence), -1) FROM Image i WHERE i.board.id = :boardId")
    Integer findMaxSequenceByBoardId(@Param("boardId") Long boardId);

    @Query("SELECT COALESCE(MAX(i.sequence), -1) FROM Image i WHERE i.review.reviewId = :reviewId")
    Integer findMaxSequenceByReviewId(@Param("reviewId") Long reviewId);
}
