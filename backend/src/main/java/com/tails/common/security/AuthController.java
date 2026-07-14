package com.tails.common.security;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.dto.AccessTokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 토큰 재발급/로그아웃 API - SecurityConfig에서 전부 permitAll
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "토큰 재발급 / 로그아웃 API")
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "쿠키의 Refresh Token으로 새 Access Token을 발급받습니다.")
    public ApiResponse<AccessTokenResponse> reissue(
            @CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new CustomException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        AuthService.IssuedTokens tokens = authService.reissue(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, tokens.refreshCookie().toString());
        return ApiResponse.success(new AccessTokenResponse(tokens.accessToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "Refresh Token을 무효화하고 쿠키를 제거합니다.")
    public ApiResponse<Void> logout(
            @CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.createExpiredRefreshTokenCookie().toString());
        return ApiResponse.success();
    }
}
