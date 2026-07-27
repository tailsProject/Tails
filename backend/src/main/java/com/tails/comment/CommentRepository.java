package com.tails.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만 페이징 조회 - 댓글 목록 페이징의 기준
    @EntityGraph(attributePaths = "member")
    Page<Comment> findByBoardIdAndParentIsNull(Long boardId, Pageable pageable);

    // 게시글의 전체 답글 조회 (N+1 방지)
    @EntityGraph(attributePaths = "member")
    List<Comment> findByBoardIdAndParentIsNotNullOrderByCreatedAtAsc(Long boardId);
}
