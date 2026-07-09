package com.tails.member.dto;

import jakarta.validation.constraints.NotBlank;

// 로그인 요청 데이터를 전달하는 DTO
public record MemberLoginRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        String password
) {
}
