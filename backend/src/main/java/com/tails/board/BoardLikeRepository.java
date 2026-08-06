package com.tails.board;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BoardLikeRepository extends JpaRepository<BoardLike, Long> {

// 좋아요 여부 확인 및 취소 처리를 위해 좋아요 엔티티 조회
    Optional<BoardLike> findByBoardIdAndMemberId(Long boardId, Long memberId);

    // 게시글 삭제 전 좋아요 기록 정리 - board_id FK가 실제 DB에서는 CASCADE로 갱신 안 돼있을 수 있어 명시적으로 먼저 지움
    void deleteByBoardId(Long boardId);
}
