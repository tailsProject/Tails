package com.tails.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;

// MySQL + Redis 컨테이너 위에서 Spring 컨텍스트 전체를 띄우는 통합테스트 공통 베이스
// 싱글톤 컨테이너 패턴 - 정적 초기화 블록에서 한 번만 기동, stop() 없이 Ryuk가 정리
// 테스트 메서드에 @Transactional 금지 - AFTER_COMMIT 리스너가 트리거되지 않음
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    @ServiceConnection(name = "redis")
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    static {
        MYSQL.start();
        REDIS.start();
    }
}
