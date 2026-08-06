package com.tails.report;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.report.dto.ReportCreateRequest;
import com.tails.report.dto.ReportResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 신고(게시글/댓글/유저) API. 전부 로그인 필요
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Report", description = "게시글/댓글/유저 신고 등록 / 내 신고 목록 조회 API")
public class ReportController {

    private final ReportService reportService;

    @PostMapping
    @Operation(summary = "신고 등록", description = "게시글/댓글/유저를 신고합니다. 자기 자신(또는 자기 글/댓글) 신고, 중복 신고는 거부됩니다. 로그인 필요.")
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success(reportService.create(userDetails.getMemberId(), request));
    }

    @GetMapping("/my")
    @Operation(summary = "내 신고 목록 조회", description = "내가 등록한 신고 목록을 조회합니다. 로그인 필요.")
    public ApiResponse<Page<ReportResponse>> getMyReports(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(reportService.getMyReports(userDetails.getMemberId(), pageable));
    }
}
