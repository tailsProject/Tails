package com.tails.review;

import com.tails.member.Member;
import com.tails.place.Place;
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

// 장소 리뷰 엔티티. (place_id, member_id) 유니크 — 한 회원은 한 장소에 리뷰 하나만
@Entity
@Table(name = "place_review", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long reviewId;

    // 회원 탈퇴해도 리뷰는 "탈퇴한 회원"으로 남김 (게시글/댓글과 동일 패턴)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 별점 1~5. 범위 검증은 요청 dto의 @Min/@Max가 담당
    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Builder
    public Review(Member member, Place place, int rating, String content) {
        this.member = member;
        this.place = place;
        this.rating = rating;
        this.content = content;
    }
}
