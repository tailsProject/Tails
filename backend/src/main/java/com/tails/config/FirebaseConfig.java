package com.tails.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

// FCM 발송용 FirebaseApp 초기화. 서비스 계정 키가 없어도 앱은 정상 기동하도록 조건부 초기화
@Configuration
@Slf4j
public class FirebaseConfig {

    @Value("${firebase.credentials-path:}")
    private String credentialsPath;

    @PostConstruct
    public void initialize() {
        if (credentialsPath == null || credentialsPath.isBlank()) {
            log.warn("firebase.credentials-path가 설정되지 않아 FCM 푸시를 비활성화합니다.");
            return;
        }

        try (FileInputStream serviceAccount = new FileInputStream(credentialsPath)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("FirebaseApp 초기화 완료 - FCM 푸시 알림이 활성화됩니다.");
            }
        } catch (IOException e) {
            log.warn("Firebase 서비스 계정 키({})를 읽지 못해 FCM 푸시를 비활성화합니다: {}",
                    credentialsPath, e.getMessage());
        }
    }
}
