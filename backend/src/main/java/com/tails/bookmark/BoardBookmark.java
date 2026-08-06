package com.tails.bookmark;

import com.tails.board.Board;
import com.tails.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// 게시글 북마크 정보를 저장하는 엔티티
@Entity
@Table(name = "board_bookmark", uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_bookmark_id")
    private Long id;

    // 게시글 삭제 시 관련 북마크 기록도 같이 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardBookmark(Board board, Member member) {
        this.board = board;
        this.member = member;
        this.createdAt = LocalDateTime.now();
    }
}
