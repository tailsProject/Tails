package com.tails.member;

import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WithdrawnMemberRepository extends JpaRepository<WithdrawnMember, Long> {

    boolean existsByEmailAndWithdrawnAtAfter(String email, LocalDateTime after);
}
