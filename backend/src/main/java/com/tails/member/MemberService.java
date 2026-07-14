package com.tails.member;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.security.AuthService;
import com.tails.member.dto.LoginResponse;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import com.tails.member.dto.MemberResponse;
import com.tails.member.dto.MemberUpdateRequest;
import com.tails.member.dto.PasswordChangeRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 회원 관련 비즈니스 로직 (회원가입/로그인/중복확인/내 정보 조회·수정)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

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

    // 컨트롤러가 본문(LoginResponse)과 별개로 Refresh Token 쿠키를 Set-Cookie에 실어야 해서 묶어서 반환
    public record LoginResult(LoginResponse response, ResponseCookie refreshCookie) {
    }

    @Transactional
    public LoginResult login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        var tokens = authService.issueTokens(member);
        return new LoginResult(
                new LoginResponse(tokens.accessToken(), member.getId(), member.getNickname()),
                tokens.refreshCookie());
    }

    public MemberResponse getMyInfo(Long memberId) {
        Member member = memberRepository.findByIdWithPets(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        return MemberResponse.from(member);
    }

    @Transactional
    public void updateMyInfo(Long memberId, MemberUpdateRequest request) {
        Member member = getMemberOrThrow(memberId);

        if (request.nickname() != null) {
            String nickname = request.nickname().trim();
            if (!nickname.equals(member.getNickname())) {
                if (isNicknameDuplicated(nickname)) {
                    throw new CustomException(ErrorCode.DUPLICATE_NICKNAME);
                }
                member.changeNickname(nickname);
            }
        }
        if (request.profileImg() != null) {
            member.changeProfileImg(request.profileImg());
        }
    }

    @Transactional
    public void changePassword(Long memberId, PasswordChangeRequest request) {
        Member member = getMemberOrThrow(memberId);

        if (!passwordEncoder.matches(request.currentPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }
        if (passwordEncoder.matches(request.newPassword(), member.getPassword())) {
            throw new CustomException(ErrorCode.SAME_AS_OLD_PASSWORD);
        }
        requireMatchingPasswords(request.newPassword(), request.newPasswordConfirm());

        member.changePassword(passwordEncoder.encode(request.newPassword()));
    }

    @Transactional
    public void withdraw(Long memberId) {
        Member member = getMemberOrThrow(memberId);
        authService.revokeSession(memberId);
        memberRepository.delete(member);
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

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }
}
