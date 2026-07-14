package com.tails.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByMember_Id(Long memberId, Pageable pageable);

    // 벌크 UPDATE
    @Modifying
    @Query("update Notification n set n.read = true where n.member.id = :memberId and n.read = false")
    int markAllAsRead(@Param("memberId") Long memberId);
}
