package com.tails.member.dto;

// 이메일, 닉네임 중복확인 응답 값. reason은 사용 불가일 때만 구체적 사유를 담음
public record AvailabilityResponse(boolean available, String reason) {
}
