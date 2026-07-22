package com.tails.board;

import java.util.List;
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

    // 회원 탈퇴 시 그 회원이 좋아요 눌렀던 게시글의 likeCount를 벌크로 낮춤(MemberService.withdraw)
    @Modifying
    @Query("update Board b set b.likeCount = case when b.likeCount > 0 then b.likeCount - 1 else 0 end where b.id in :boardIds")
    void decreaseLikeCountBulk(@Param("boardIds") List<Long> boardIds);

    @Query(value = "select b from Board b left join fetch b.member where b.status = com.tails.board.BoardStatus.PUBLISHED",
            countQuery = "select count(b) from Board b where b.status = com.tails.board.BoardStatus.PUBLISHED")
    Page<Board> findAllWithMember(Pageable pageable);

    // 제목/내용에 keyword가 포함된 게시글 검색
    @Query(value = "select b from Board b left join fetch b.member "
            + "where b.status = com.tails.board.BoardStatus.PUBLISHED "
            + "and (b.title like concat('%', :keyword, '%') or b.content like concat('%', :keyword, '%'))",
            countQuery = "select count(b) from Board b "
                    + "where b.status = com.tails.board.BoardStatus.PUBLISHED "
                    + "and (b.title like concat('%', :keyword, '%') or b.content like concat('%', :keyword, '%'))")
    Page<Board> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 좋아요 수 기준 인기순 정렬
    @Query(value = "select b from Board b left join fetch b.member "
            + "where b.status = com.tails.board.BoardStatus.PUBLISHED order by b.likeCount desc",
            countQuery = "select count(b) from Board b where b.status = com.tails.board.BoardStatus.PUBLISHED")
    Page<Board> findAllOrderByPopularity(Pageable pageable);

    // 내 임시저장 목록
    Page<Board> findByMemberIdAndStatus(Long memberId, BoardStatus status, Pageable pageable);

    // 내가 쓴 글 목록 - DRAFT/PUBLISHED 구분 없이 전부 보임
    Page<Board> findByMemberId(Long memberId, Pageable pageable);
}
