package com.tails.member;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 회원 데이터 관리 JPA Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    // 관리자 회원 검색 - 이메일 또는 닉네임에 keyword 포함. ADMIN/MANAGER가 승격 즉시 목록 위쪽에 오도록 권한순 정렬
    @Query("select m from Member m where m.email like concat('%', :email, '%') or m.nickname like concat('%', :nickname, '%') "
            + "order by case m.role when com.tails.member.MemberRole.ADMIN then 0 "
            + "when com.tails.member.MemberRole.MANAGER then 1 else 2 end, m.id desc")
    Page<Member> findByEmailContainingOrNicknameContaining(
            @Param("email") String email, @Param("nickname") String nickname, Pageable pageable);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // 소셜 로그인 회원 조회. providerId는 provider가 발급하는 불변 식별자라 이메일보다 안정적
    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    // pets는 LAZY라 fetch join 없이 조회하면 쿼리가 2번(회원 + pets) 나감. 1번으로 합치기 위한 조회
    @Query("select m from Member m left join fetch m.pets where m.id = :id")
    Optional<Member> findByIdWithPets(@Param("id") Long id);

    // 로그인 실패 카운트 원자적 증가 - 조회 후 +1 저장 방식은 병렬 요청에서 lost update가 나 잠금이 우회될 수 있었음
    @Modifying(clearAutomatically = true)
    @Query("update Member m set m.failedLoginCount = m.failedLoginCount + 1 where m.id = :id")
    void incrementFailedLoginCount(@Param("id") Long id);
}
