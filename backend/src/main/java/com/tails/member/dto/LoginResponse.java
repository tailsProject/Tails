package com.tails.member.dto;

// 로그인 성공 응답 (JWT 및 회원 기본 정보)
public record LoginResponse(
        String accessToken,
        Long memberId,
        String nickname
) {
}
