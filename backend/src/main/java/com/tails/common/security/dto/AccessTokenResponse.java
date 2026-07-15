package com.tails.common.security.dto;

// 토큰 재발급 응답. refreshToken은 본문이 아니라 쿠키로 내려감
public record AccessTokenResponse(String accessToken) {
}
