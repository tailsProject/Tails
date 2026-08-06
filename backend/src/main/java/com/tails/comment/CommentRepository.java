package com.tails.comment;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    // 최상위 댓글만 페이징 조회 - 댓글 목록 페이징의 기준
    @EntityGraph(attributePaths = "member")
    Page<Comment> findByBoardIdAndParentIsNull(Long boardId, Pageable pageable);

    // 게시글의 전체 답글 조회 (N+1 방지)
    @EntityGraph(attributePaths = "member")
    List<Comment> findByBoardIdAndParentIsNotNullOrderByCreatedAtAsc(Long boardId);

    // 페이징된 최상위 댓글들의 답글을 한 번에 조회(N+1 방지)
    @EntityGraph(attributePaths = "member")
    List<Comment> findByParentIdInOrderByCreatedAtAsc(List<Long> parentIds);

    // 게시글별 댓글 수 배치 조회 (삭제 댓글 제외)
    @Query("select c.board.id, count(c) from Comment c where c.board.id in :boardIds and c.deleted = false group by c.board.id")
    List<Object[]> countByBoardIds(@Param("boardIds") List<Long> boardIds);

    // 삭제하려는 댓글에 답글이 있는지 확인, 삭제 방식 분기용
    boolean existsByParent_Id(Long parentId);

    // 좋아요 토글도 벌크 쿼리로 처리 - 엔티티 메서드+dirty checking 방식은 동시 요청 시
    // 마지막에 쓴 요청이 이전 변경을 덮어써 좋아요 수가 실제 row 개수와 어긋날 수 있음
    @Modifying
    @Query("update Comment c set c.likeCount = c.likeCount + 1 where c.id = :id")
    void increaseLikeCount(@Param("id") Long id);

    @Modifying
    @Query("update Comment c set c.likeCount = case when c.likeCount > 0 then c.likeCount - 1 else 0 end where c.id = :id")
    void decreaseLikeCount(@Param("id") Long id);

    @Query("select c.likeCount from Comment c where c.id = :id")
    int findLikeCountById(@Param("id") Long id);

}
