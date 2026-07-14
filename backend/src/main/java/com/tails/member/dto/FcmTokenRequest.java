package com.tails.member.dto;

import jakarta.validation.constraints.NotBlank;

// FCM 기기 토큰 등록 요청
public record FcmTokenRequest(
        @NotBlank(message = "FCM 토큰을 입력해주세요.")
        String fcmToken
) {
}
