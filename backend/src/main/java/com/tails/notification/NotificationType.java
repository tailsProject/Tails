package com.tails.notification;

// 알림 종류. COMMENT/LIKE/BOOKMARK는 이벤트 발행/구독, TRAVEL은 스케줄러가 직접 생성
public enum NotificationType {
    COMMENT,
    LIKE,
    BOOKMARK,
    TRAVEL
}
