package com.tails.admin;

import com.tails.admin.dto.RoleChangeRequest;
import com.tails.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 관리자 전용 API. /api/admin/**는 SecurityConfig에서 hasRole("ADMIN")으로 묶여 있음
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API - ADMIN 권한 필요")
public class AdminController {

    private final AdminService adminService;

    @PatchMapping("/members/{memberId}/role")
    @Operation(summary = "회원 권한 변경", description = "대상 회원의 권한을 USER/ADMIN으로 변경합니다. ADMIN 권한 필요.")
    public ApiResponse<Void> changeMemberRole(
            @PathVariable Long memberId,
            @Valid @RequestBody RoleChangeRequest request) {
        adminService.changeMemberRole(memberId, request.role());
        return ApiResponse.success();
    }
}
