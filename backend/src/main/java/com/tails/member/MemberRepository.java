package com.tails.member;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

// 회원 데이터 관리 JPA Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByNickname(String nickname);
}
