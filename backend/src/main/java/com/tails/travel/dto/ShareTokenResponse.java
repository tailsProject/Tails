package com.tails.travel.dto;

// 공유 링크 발급 응답 DTO. 프론트가 /travels/shared/{shareToken} 형태로 공유 URL을 조립할 수 있게
// 토큰 문자열 그대로 내려줌
public record ShareTokenResponse(String shareToken) {
}
