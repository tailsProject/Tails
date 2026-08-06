package com.tails.report;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReportRepository extends JpaRepository<Report, Long> {

    // 중복 신고 사전 체크(최종 안전망은 테이블의 unique 제약)
    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    // 내가 신고한 목록 (마이페이지)
    Page<Report> findByReporterId(Long reporterId, Pageable pageable);

    // 관리자 신고 처리 큐 조회용. reporter 닉네임을 같이 보여줘야 해서 N+1 방지로 fetch join
    @Query(value = "select r from Report r left join fetch r.reporter where r.status = :status",
            countQuery = "select count(r) from Report r where r.status = :status")
    Page<Report> findByStatus(@Param("status") ReportStatus status, Pageable pageable);
}
