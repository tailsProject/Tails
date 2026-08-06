package com.tails.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// JPA 엔티티의 생성일 및 수정일 자동 기록 Auditing 설정
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
