package com.tails.common.security.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmRequest(

        @NotBlank(message = "토큰이 없습니다.")
        String token,

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$",
                message = "비밀번호는 영문, 숫자, 특수문자를 모두 포함해야 합니다.")
        String newPassword,

        @NotBlank(message = "새 비밀번호 확인을 입력해주세요.")
        String newPasswordConfirm
) {
}
