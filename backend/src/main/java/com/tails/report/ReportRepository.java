package com.tails.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 중복 신고 사전 체크(최종 안전망은 테이블의 unique 제약)
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 내가 신고한 목록 (마이페이지)
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);
}
