package com.tails.member;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 회원 데이터 관리 JPA Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);

    // pets는 LAZY라 fetch join 없이 조회하면 쿼리가 2번(회원 + pets) 나감. 1번으로 합치기 위한 조회
    @Query("select m from Member m left join fetch m.pets where m.id = :id")
    Optional<Member> findByIdWithPets(@Param("id") Long id);
}
