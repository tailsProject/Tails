package com.tails.notification;

// 알림 종류. COMMENT/REPLY/LIKE/BOOKMARK/REPORT_RESOLVED는 이벤트 발행/구독, TRAVEL은 스케줄러가 직접 생성
public enum NotificationType {
    COMMENT,
    REPLY,
    LIKE,
    BOOKMARK,
    TRAVEL,
    REPORT_RESOLVED
}
