package com.tails.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 비밀번호 변경 요청. 현재 비밀번호 확인 후 새 비밀번호로 교체
public record PasswordChangeRequest(

        @NotBlank(message = "기존 비밀번호를 입력해주세요.")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8자 이상이어야 합니다.")
        // 회원가입과 동일한 강도 기준 유지
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {
}
