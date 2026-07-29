package com.tails.board;

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

// 자유게시판 게시글(BOARD) 엔티티
@Entity
@Table(name = "board")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_id")
    private Long id;

    // 회원 탈퇴 후에도 게시글은 남기기 위해 nullable + ON DELETE SET NULL로 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    // 기존 행이 있는 테이블에 컬럼 추가라 DEFAULT 없으면 NOT NULL 추가가 막힘(MySQL strict mode)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED'")
    private BoardStatus status;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    @Builder
    public Board(Member member, String title, String content, BoardStatus status) {
        this.member = member;
        this.title = title;
        this.content = content;
        this.viewCount = 0;
        this.likeCount = 0;
        this.status = status;
    }

    public void changeTitleAndContent(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void publish() {
        this.status = BoardStatus.PUBLISHED;
    }

    public boolean isDraft() {
        return status == BoardStatus.DRAFT;
    }

    // DRAFT는 작성자 본인에게만, PUBLISHED는 누구나 볼 수 있음
    public boolean isVisibleTo(Long currentMemberId) {
        if (!isDraft()) {
            return true;
        }
        return isAuthor(currentMemberId);
    }

    // 작성자 본인 여부 확인
    public boolean isAuthor(Long currentMemberId) {
        return currentMemberId != null && member != null && member.getId().equals(currentMemberId);
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }
}
