package com.tails.common.security;

import com.tails.member.Member;
import com.tails.member.MemberRepository;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// 로그인 실패 횟수 제한(브루트포스 방어)
@Service
public class LoginAttemptService {

    private final MemberRepository memberRepository;
    private final int maxAttempts;
    private final long lockDurationMinutes;

    public LoginAttemptService(MemberRepository memberRepository,
                                @Value("${login.max-attempts}") int maxAttempts,
                                @Value("${login.lock-duration-minutes}") long lockDurationMinutes) {
        this.memberRepository = memberRepository;
        this.maxAttempts = maxAttempts;
        this.lockDurationMinutes = lockDurationMinutes;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long memberId) {
        memberRepository.incrementFailedLoginCount(memberId);
        memberRepository.findById(memberId).ifPresent(member -> {
            if (member.getFailedLoginCount() >= maxAttempts) {
                member.lock(LocalDateTime.now().plusMinutes(lockDurationMinutes));
            }
        });
    }

    @Transactional
    public void recordSuccess(Member member) {
        member.resetFailedLoginCount();
    }
}
