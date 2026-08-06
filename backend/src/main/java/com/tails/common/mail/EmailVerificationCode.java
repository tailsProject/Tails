package com.tails.common.mail;

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

// 회원가입 전 이메일 인증번호. email을 키로 관리(가입 전이라 Member 없음)
@Entity
@Table(name = "email_verification_code")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "code_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private boolean verified;

    @Column(name = "expired_at", nullable = false)
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public EmailVerificationCode(String email, String code, LocalDateTime expiredAt) {
        this.email = email;
        this.code = code;
        this.expiredAt = expiredAt;
        this.createdAt = LocalDateTime.now();
    }

    // 재발송 시 이전 인증 상태는 무효화하고 새 코드로 교체
    public void renew(String newCode, LocalDateTime newExpiredAt) {
        this.code = newCode;
        this.expiredAt = newExpiredAt;
        this.verified = false;
    }

    public void markVerified() {
        this.verified = true;
    }

    public boolean isExpired() {
        return expiredAt.isBefore(LocalDateTime.now());
    }

    public boolean matches(String input) {
        return code.equals(input);
    }
}
