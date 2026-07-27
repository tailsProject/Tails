package com.tails.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만 페이징 조회 - 댓글 목록 페이징의 기준
    @EntityGraph(attributePaths = "member")
    Page<Comment> findByBoardIdAndParentIsNull(Long boardId, Pageable pageable);

    // 페이징된 최상위 댓글들의 답글을 한 번에 조회(N+1 방지)
    @EntityGraph(attributePaths = "member")
    List<Comment> findByParentIdInOrderByCreatedAtAsc(List<Long> parentIds);

    // 게시글별 댓글 수 배치 조회 (삭제 댓글 제외)
    @Query("select c.board.id, count(c) from Comment c where c.board.id in :boardIds and c.deleted = false group by c.board.id")
    List<Object[]> countByBoardIds(@Param("boardIds") List<Long> boardIds);
}
