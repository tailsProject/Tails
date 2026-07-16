package com.tails.notification;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.notification.dto.NotificationResponse;
import com.tails.notification.fcm.FcmService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 알림 관련 비즈니스 로직.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final MemberRepository memberRepository;
    private final FcmService fcmService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long recipientId, NotificationType type, String content, Long targetId) {
        Member recipient = memberRepository.getReferenceById(recipientId);
        Notification notification = Notification.builder()
                .member(recipient)
                .type(type)
                .content(content)
                .targetId(targetId)
                .build();
        notificationRepository.save(notification);

        // recipient는 LAZY 프록시라 fcmService 안에서 getFcmToken() 호출 시 SELECT가 나간다
        fcmService.send(recipient, "Tails", content);
    }

    public Page<NotificationResponse> getMyNotifications(Long memberId, Pageable pageable) {
        return notificationRepository.findByMember_Id(memberId, pageable)
                .map(NotificationResponse::from);
    }

    // 본인 알림인지 확인 후에만 읽음 처리
    @Transactional
    public void markAsRead(Long notificationId, Long memberId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.NOT_NOTIFICATION_OWNER);
        }

        notification.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long memberId) {
        notificationRepository.markAllAsRead(memberId);
    }
}
