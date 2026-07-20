package com.tails.common.mail;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTokenRepository extends JpaRepository<EmailToken, Long> {

    Optional<EmailToken> findByToken(String token);

    Optional<EmailToken> findByMemberIdAndType(Long memberId, EmailTokenType type);
}
