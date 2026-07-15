package com.tails.member;

import com.tails.board.BoardLike;
import com.tails.bookmark.BoardBookmark;
import com.tails.bookmark.PlaceBookmark;
import com.tails.notification.Notification;
import com.tails.pet.Pet;
import com.tails.travel.Travel;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// 회원 엔티티
@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // 소셜 로그인 회원은 비밀번호 없이 가입되므로 nullable
    @Column(length = 255)
    private String password;

    @Column(nullable = false, unique = true, length = 50)
    private String nickname;

    @Column(name = "profile_img", length = 500)
    private String profileImg;

    @Column(name = "email_verified", nullable = false, columnDefinition = "TINYINT(1) NOT NULL DEFAULT 0")
    private boolean emailVerified;

    // provider가 null이면 자체가입, "kakao"/"google"/"naver"면 소셜 가입 회원
    @Column(length = 20)
    private String provider;

    @Column(name = "provider_id", length = 100)
    private String providerId;

    // 로그인 실패 횟수 제한
    @Column(name = "failed_login_count", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private int failedLoginCount;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    // FCM 기기 토큰. 기기당 하나만 저장(다중 기기 미지원)
    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 동시 수정 충돌 감지용 낙관적 락 (닉네임 변경/비밀번호 변경이 동시에 들어와도 유실 없이 실패 처리)
    @Version
    private Long version;

    // 회원 탈퇴 시 반려동물도 함께 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pet> pets = new ArrayList<>();

    // 회원 탈퇴 시 북마크 기록도 함께 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardBookmark> boardBookmarks = new ArrayList<>();

    // 회원 탈퇴 시 장소 찜 기록도 함께 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceBookmark> placeBookmarks = new ArrayList<>();

    // 여행 일정 목록. 개인 데이터라 회원 삭제 시 함께 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Travel> travels = new ArrayList<>();

    // 회원 삭제 시 연관된 좋아요 기록 삭제 처리
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BoardLike> boardLikes = new ArrayList<>();

    // 회원 탈퇴 시 알림도 함께 삭제
    @OneToMany(mappedBy = "member", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Notification> notifications = new ArrayList<>();

    @Builder
    public Member(String email, String password, String nickname, String provider, String providerId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.provider = provider;
        this.providerId = providerId;
    }

    public void changeNickname(String nickname) {
        this.nickname = nickname;
    }

    public void changeProfileImg(String profileImg) {
        this.profileImg = profileImg;
    }

    public void changePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void changeFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void markEmailVerified() {
        this.emailVerified = true;
    }

    public void increaseFailedLoginCount() {
        this.failedLoginCount++;
    }

    public void resetFailedLoginCount() {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
    }

    public void lock(LocalDateTime until) {
        this.lockedUntil = until;
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }
}
