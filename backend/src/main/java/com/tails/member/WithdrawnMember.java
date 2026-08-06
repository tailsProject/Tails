package com.tails.member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 탈퇴 이력. Member는 탈퇴 시 물리 삭제되므로(withdraw 참고) 재가입 쿨다운을 판단하려면
// Member와 별개로 이메일 + 탈퇴 시각만 남겨두는 테이블이 필요하다
@Entity
@Table(name = "withdrawn_member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WithdrawnMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "withdrawn_member_id")
    private Long id;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "withdrawn_at", nullable = false)
    private LocalDateTime withdrawnAt;

    @Builder
    public WithdrawnMember(String email, LocalDateTime withdrawnAt) {
        this.email = email;
        this.withdrawnAt = withdrawnAt;
    }
}
