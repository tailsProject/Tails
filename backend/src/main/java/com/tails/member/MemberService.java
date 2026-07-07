package com.tails.member;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.security.JwtProvider;
import com.tails.member.dto.LoginResponse;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 관련 비즈니스 로직 (회원가입/로그인/중복확인)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

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

    // 이메일로 회원 조회 후 비밀번호 검증, 성공 시 JWT 발급
    public LoginResponse login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtProvider.createToken(member.getId(), member.getEmail());
        return new LoginResponse(token, member.getId(), member.getNickname());
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
