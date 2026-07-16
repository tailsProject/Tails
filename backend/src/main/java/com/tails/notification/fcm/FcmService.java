package com.tails.notification.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.tails.member.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

// FCM 푸시 발송. DB 알림(폴링)과 별개인 보완 채널이라 발송 실패가 알림 생성 자체를 막으면 안 됨
@Service
@Slf4j
public class FcmService {

    // Firebase 미설정이거나 기기 미등록이면 조용히 건너뛴다(둘 다 정상 상태, 오류 아님)
    public void send(Member member, String title, String body) {
        if (FirebaseApp.getApps().isEmpty()) {
            return;
        }
        if (member.getFcmToken() == null || member.getFcmToken().isBlank()) {
            return;
        }

        // REQUIRES_NEW 트랜잭션 안에서 호출되므로, 여기서 새어나가는 예외는 방금 저장한
        // 알림 DB 행까지 롤백시킨다 - Exception 전체를 잡아 그 트랜잭션을 보호한다
        try {
            Message message = Message.builder()
                    .setToken(member.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();
            FirebaseMessaging.getInstance().send(message);
        } catch (Exception e) {
            log.warn("FCM 푸시 발송 실패 (memberId={}): {}", member.getId(), e.getMessage());
        }
    }
}
