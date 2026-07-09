package com.tails.bookmark;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 회원과 게시글의 북마크 조회 및 관리 Repository
public interface BoardBookmarkRepository extends JpaRepository<BoardBookmark, Long> {

    Optional<BoardBookmark> findByBoardIdAndMemberId(Long boardId, Long memberId);
}
