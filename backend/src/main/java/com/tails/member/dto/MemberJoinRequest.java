package com.tails.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 회원가입 요청 데이터를 전달하고 입력값 검증을 수행하는 DTO
public record MemberJoinRequest(

        @NotBlank(message = "이메일을 입력해주세요.")
        @Email(message = "올바른 이메일 형식으로 입력해주세요.")
        // Member.email 컬럼 길이(100)와 맞춤 - 없으면 DB INSERT 단계에서 DataIntegrityViolationException이 나 DUPLICATE_RESOURCE로 잘못 응답됨
        @Size(max = 100, message = "이메일은 100자를 넘을 수 없습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8자 이상이어야 합니다.")
        // 영문/숫자/특수문자가 각각 어딘가에 있는지만 확인하는 lookahead 3개 (순서 무관)
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String password,

        @NotBlank(message = "비밀번호 확인을 입력해주세요.")
        String passwordConfirm,

        @NotBlank(message = "닉네임을 입력해주세요.")
        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        // 선택 항목이라 프론트가 안 보내도(null) 되고, 그 경우 미동의로 처리
        Boolean agreeMarketing
) {
}
