package com.tails.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

// 좋아요 여부 확인 및 취소 처리를 위해 좋아요 엔티티 조회
    Optional<BoardLike> findByBoardIdAndMemberId(Long boardId, Long memberId);
}
