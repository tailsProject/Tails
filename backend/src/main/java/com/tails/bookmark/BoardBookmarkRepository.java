package com.tails.bookmark;

import com.tails.board.Board;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 회원과 게시글의 북마크 조회 및 관리 Repository
public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {

    Optional<BoardBookmark> findByBoardIdAndMemberId(Long boardId, Long memberId);

    // 내 북마크 목록: JPQL로 직접 작성
    @Query("select bb.board from BoardBookmark bb where bb.member.id = :memberId order by bb.createdAt desc")
    Page<Board> findBookmarkedBoardsByMemberId(@Param("memberId") Long memberId, Pageable pageable);
}
