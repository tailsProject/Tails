package com.tails.comment;

import com.tails.board.Board;
import com.tails.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

// 게시글 댓글 엔티티
@Entity
@Table(name = "board_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    // 게시글이 삭제되면 댓글도 같이 삭제 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "board_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Board board;

    // 회원 탈퇴해도 댓글은 "탈퇴한 회원"으로 남김
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    // 답글인 경우 부모 댓글을 참조, 최상위 댓글이면 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", nullable = true)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Comment parent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    // 답글이 있는 댓글은 실제 삭제하지 않고 삭제 여부만 변경
    @Column(nullable = false)
    private boolean deleted;

    // 게시글 좋아요 수와 같은 비정규화 컬럼, 목록 조회마다 집계하지 않도록 토글 시점에 증감
    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Comment(Board board, Member member, Comment parent, String content) {
        this.board = board;
        this.member = member;
        this.parent = parent;
        this.content = content;
    }

    public void changeContent(String content) {
        this.content = content;
    }

    public void softDelete() {
        this.deleted = true;
    }
}
