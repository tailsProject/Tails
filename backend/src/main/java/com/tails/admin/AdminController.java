package com.tails.admin;

import com.tails.admin.dto.RoleChangeRequest;
import com.tails.common.response.ApiResponse;
import com.tails.report.ReportService;
import com.tails.report.ReportStatus;
import com.tails.report.dto.AdminReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 관리자 전용 API. /api/admin/**는 SecurityConfig에서 hasRole("ADMIN")으로 묶여 있음
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "관리자 전용 API - ADMIN 권한 필요")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    @PatchMapping("/members/{memberId}/role")
    @Operation(summary = "회원 권한 변경", description = "대상 회원의 권한을 USER/ADMIN으로 변경합니다. ADMIN 권한 필요.")
    public ApiResponse<Void> changeMemberRole(
            @PathVariable Long memberId,
            @Valid @RequestBody RoleChangeRequest request) {
        adminService.changeMemberRole(memberId, request.role());
        return ApiResponse.success();
    }

    @GetMapping("/reports")
    @Operation(summary = "신고 처리 큐 조회", description = "신고 목록을 상태별로 조회합니다. status 미지정 시 PENDING만 조회합니다. ADMIN 권한 필요.")
    public ApiResponse<Page<AdminReportResponse>> getReports(
            @RequestParam(required = false, defaultValue = "PENDING") ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(reportService.getReportsByStatus(status, pageable));
    }

    @PatchMapping("/reports/{reportId}/resolve")
    @Operation(summary = "신고 처리 완료 표시", description = "신고를 RESOLVED 상태로 변경합니다. ADMIN 권한 필요.")
    public ApiResponse<Void> resolveReport(@PathVariable Long reportId) {
        reportService.resolve(reportId);
        return ApiResponse.success();
    }
}
