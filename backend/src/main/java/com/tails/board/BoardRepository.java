package com.tails.board;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

// 게시글(BOARD) 데이터 관리 JPA Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // 벌크 쿼리로 처리
    @Modifying
    @Query("update Board b set b.viewCount = b.viewCount + 1 where b.id = :id")
    void increaseViewCount(@Param("id") Long id);

    @Query(value = "select b from Board b left join fetch b.member",
            countQuery = "select count(b) from Board b")
    Page<Board> findAllWithMember(Pageable pageable);
}
