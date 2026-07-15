package com.tails.common.security;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import io.jsonwebtoken.Claims;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 인증 세션(Refresh Token) 발급/재발급/무효화 담당
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final JwtProvider jwtProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final CookieUtil cookieUtil;
    private final MemberRepository memberRepository;
    private final long refreshExpirationMillis;

    public AuthService(JwtProvider jwtProvider,
                        RefreshTokenStore refreshTokenStore,
                        CookieUtil cookieUtil,
                        MemberRepository memberRepository,
                        @Value("${jwt.refresh-expiration}") long refreshExpirationMillis) {
        this.jwtProvider = jwtProvider;
        this.refreshTokenStore = refreshTokenStore;
        this.cookieUtil = cookieUtil;
        this.memberRepository = memberRepository;
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
}
