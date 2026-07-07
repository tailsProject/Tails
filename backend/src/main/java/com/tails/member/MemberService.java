package com.tails.member;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.dto.MemberJoinRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 관련 비즈니스 로직 (회원가입/중복확인)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Long join(MemberJoinRequest request) {
        requireMatchingPasswords(request.password(), request.passwordConfirm());
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().trim();
        if (isEmailDuplicated(email)) {
            throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (isNicknameDuplicated(nickname)) {
            throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
        }

        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nickname(nickname)
                .build();

        return memberRepository.save(member).getId();
    }

    public boolean isEmailDuplicated(String email) {
        return memberRepository.existsByEmail(normalizeEmail(email));
    }

    public boolean isNicknameDuplicated(String nickname) {
        return memberRepository.existsByNickname(nickname.trim());
    }

    private void requireMatchingPasswords(String password, String passwordConfirm) {
        if (!password.equals(passwordConfirm)) {
            throw new CustomException(ErrorCode.PASSWORD_NOT_MATCHED);
        }
    }

    // 대소문자/공백 차이로 다른 계정 취급되지 않도록 가입/로그인/중복체크 모두 이 기준으로 통일
    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
