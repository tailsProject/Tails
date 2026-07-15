package com.tails.admin.dto;

import com.tails.member.MemberRole;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "변경할 권한(role)을 입력해주세요.")
        MemberRole role
) {
}
