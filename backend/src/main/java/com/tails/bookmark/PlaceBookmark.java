package com.tails.bookmark;

import com.tails.member.Member;
import com.tails.place.Place;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 장소 찜 기록 엔티티 (장소-회원 중복 찜 방지)
@Entity
@Table(name = "place_bookmark", uniqueConstraints = @UniqueConstraint(columnNames = {"place_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlaceBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "place_bookmark_id")
    private Long id;

    // 장소는 삭제 API가 없어 board 쪽과 달리 OnDelete 설정 불필요
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    // 회원 탈퇴 시 Member.placeBookmarks의 cascade로 함께 삭제
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public PlaceBookmark(Place place, Member member) {
        this.place = place;
        this.member = member;
        this.createdAt = LocalDateTime.now();
    }
}
