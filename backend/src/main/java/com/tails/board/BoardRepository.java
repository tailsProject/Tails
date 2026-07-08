package com.tails.board;

import org.springframework.data.jpa.repository.JpaRepository;

// 게시글(BOARD) 데이터 관리 JPA Repository
public interface BoardRepository extends JpaRepository<Board, Long> {
}
