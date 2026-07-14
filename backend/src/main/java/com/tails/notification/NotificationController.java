package com.tails.notification;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.notification.dto.NotificationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 알림 API. 전부 로그인 필요
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notification", description = "알림 목록 조회 / 읽음 처리 API")
public class NotificationController {

    private final NotificationService notificationService;

    // 내 알림 목록 - 로그인 필요 
    @GetMapping
    @Operation(summary = "내 알림 목록 조회", description = "로그인한 회원의 알림 목록을 최신순으로 조회합니다. 로그인 필요.")
    public ApiResponse<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(notificationService.getMyNotifications(userDetails.getMemberId(), pageable));
    }

    // 알림 읽음 처리 - 본인 알림만 
    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "알림 읽음 처리", description = "알림 하나를 읽음 상태로 변경합니다. 본인의 알림만 가능.")
    public ApiResponse<Void> markAsRead(
            @PathVariable Long notificationId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAsRead(notificationId, userDetails.getMemberId());
        return ApiResponse.success();
    }

    // 전체 읽음 처리 - 로그인 필요
    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "읽지 않은 알림을 전부 읽음 상태로 변경합니다. 로그인 필요.")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        notificationService.markAllAsRead(userDetails.getMemberId());
        return ApiResponse.success();
    }
}
