package com.tails.common.security;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.mail.EmailToken;
import com.tails.common.mail.EmailTokenRepository;
import com.tails.common.mail.EmailTokenType;
import com.tails.common.mail.MailService;
import com.tails.common.security.dto.PasswordResetConfirmRequest;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 인증 세션(Refresh Token) 발급/재발급/무효화, 이메일 인증 담당
@Service
@Transactional(readOnly = true)
public class AuthService {

    private static final long EMAIL_TOKEN_TTL_MINUTES = 30;

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieUtil cookieUtil;
    private final MemberRepository memberRepository;
    private final EmailTokenRepository emailTokenRepository;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final long refreshExpirationMillis;

    public AuthService(JwtProvider jwtProvider,
                        RefreshTokenStore refreshTokenStore,
                        CookieUtil cookieUtil,
                        MemberRepository memberRepository,
                        EmailTokenRepository emailTokenRepository,
                        MailService mailService,
                        PasswordEncoder passwordEncoder,
                        @Value("${jwt.refresh-expiration}") long refreshExpirationMillis) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.cookieUtil = cookieUtil;
        this.memberRepository = memberRepository;
        this.emailTokenRepository = emailTokenRepository;
        this.mailService = mailService;
        this.passwordEncoder = passwordEncoder;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }

    // Access Token + Refresh Token 쿠키 한 쌍
    public record IssuedTokens(String accessToken, ResponseCookie refreshCookie) {
    }

    // 로그인 성공(일반/소셜 공통) 시 호출
    public IssuedTokens issueTokens(Member member) {
        String accessToken = jwtProvider.createToken(member.getId(), member.getEmail());
        String refreshToken = jwtProvider.createRefreshToken(member.getId());

        refreshTokenStore.save(member.getId(), refreshToken, Duration.ofMillis(refreshExpirationMillis));

        return new IssuedTokens(accessToken, cookieUtil.createRefreshTokenCookie(refreshToken));
    }

    public IssuedTokens reissue(String refreshToken) {
        Claims claims = jwtProvider.parseClaims(refreshToken)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        // Access Token으로 재발급을 요청하는 것을 방지
        if (!jwtProvider.isRefreshToken(claims)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        Long memberId = jwtProvider.getMemberId(claims);

        String saved = refreshTokenStore.find(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        if (!saved.equals(refreshToken)) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));
        return issueTokens(member);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null) {
            return;
        }
        jwtProvider.parseClaims(refreshToken)
                .ifPresent(claims -> refreshTokenStore.delete(jwtProvider.getMemberId(claims)));
    }

    // 회원 탈퇴 등 세션을 강제로 끝내야 할 때 호출
    public void revokeSession(Long memberId) {
        refreshTokenStore.delete(memberId);
    }

    // 가입 안 된 이메일이어도 조용히 반환(계정 열거 방지)
    @Transactional
    public void requestEmailVerification(String email) {
        memberRepository.findByEmail(email.trim().toLowerCase()).ifPresent(member -> {
            if (member.isEmailVerified()) {
                throw new CustomException(ErrorCode.EMAIL_ALREADY_VERIFIED);
            }
            String token = upsertEmailToken(member, EmailTokenType.SIGNUP_VERIFY);
            mailService.sendVerificationMail(member.getEmail(), token);
        });
    }

    @Transactional
    public void verifyEmail(String token) {
        EmailToken emailToken = getValidEmailTokenOrThrow(token, EmailTokenType.SIGNUP_VERIFY);
        emailToken.getMember().markEmailVerified();
        emailTokenRepository.delete(emailToken);
    }

    // 가입 안 된 이메일이어도 조용히 반환(계정 열거 방지)
    @Transactional
    public void requestPasswordReset(String email) {
        memberRepository.findByEmail(email.trim().toLowerCase()).ifPresent(member -> {
            String token = upsertEmailToken(member, EmailTokenType.PASSWORD_RESET);
            mailService.sendPasswordResetMail(member.getEmail(), token);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetConfirmRequest request) {
        if (!request.newPassword().equals(request.newPasswordConfirm())) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCHED);
        }
        EmailToken emailToken = getValidEmailTokenOrThrow(request.token(), EmailTokenType.PASSWORD_RESET);

        Member member = emailToken.getMember();
        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }
        member.changePassword(passwordEncoder.encode(request.newPassword()));
        emailTokenRepository.delete(emailToken);
        refreshTokenStore.delete(member.getId());
    }

    // (member, type)당 한 행만 유지 - 기존 토큰이 있으면 갱신, 없으면 신규 생성
    private String upsertEmailToken(Member member, EmailTokenType type) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiredAt = LocalDateTime.now().plusMinutes(EMAIL_TOKEN_TTL_MINUTES);

        emailTokenRepository.findByMemberIdAndType(member.getId(), type)
                .ifPresentOrElse(
                        existing -> existing.renew(token, expiredAt),
                        () -> emailTokenRepository.save(EmailToken.builder()
                                .member(member)
                                .token(token)
                                .type(type)
                                .expiredAt(expiredAt)
                                .build()));
        return token;
    }

    private EmailToken getValidEmailTokenOrThrow(String token, EmailTokenType expectedType) {
        EmailToken emailToken = emailTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException(ErrorCode.EMAIL_TOKEN_INVALID));
        if (emailToken.getType() != expectedType || emailToken.isExpired()) {
            throw new CustomException(ErrorCode.EMAIL_TOKEN_INVALID);
        }
        return emailToken;
    }
}
