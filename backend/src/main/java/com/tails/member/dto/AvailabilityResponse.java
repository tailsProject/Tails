package com.tails.member.dto;

// 이메일/닉네임 중복확인 응답 값. reason은 available이 false일 때만 채워짐
// (DUPLICATE: 이미 가입된 이메일/닉네임, RECENTLY_WITHDRAWN: 탈퇴 후 재가입 제한 기간)
public record AvailabilityResponse(boolean available, String reason) {

    public static AvailabilityResponse ok() {
        return new AvailabilityResponse(true, null);
    }

    public static AvailabilityResponse unavailable(String reason) {
        return new AvailabilityResponse(false, reason);
    }
}
