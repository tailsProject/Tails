package com.tails.board;

import com.tails.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// 게시글 좋아요 기록 엔티티 (게시글-회원 중복 좋아요 방지)
@Entity
@Table(name = "board_like", uniqueConstraints = @UniqueConstraint(columnNames = {"board_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_like_id")
    private Long id;

    // 게시글 삭제 시 관련 좋아요 기록 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;
    
    // 회원 삭제 시 관련 좋아요 기록 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public BoardLike(Board board, Member member) {
        this.board = board;
        this.member = member;
        this.createdAt = LocalDateTime.now();
    }
}
