package com.tails.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// Refresh Token httpOnly 쿠키 생성/만료 처리
@Component
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";
    private static final String COOKIE_PATH = "/api/auth";

    private final boolean secure;
    private final long refreshExpirationSeconds;

    public CookieUtil(@Value("${cookie.secure}") boolean secure,
                       @Value("${jwt.refresh-expiration}") long refreshExpirationMillis) {
        this.secure = secure;
        this.refreshExpirationSeconds = refreshExpirationMillis / 1000;
    }

    public ResponseCookie createRefreshTokenCookie(String token) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(refreshExpirationSeconds)
                .build();
    }

    public ResponseCookie createExpiredRefreshTokenCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(COOKIE_PATH)
                .maxAge(0)
                .build();
    }
}
