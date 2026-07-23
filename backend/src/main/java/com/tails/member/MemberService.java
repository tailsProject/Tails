package com.tails.member;

import com.tails.board.BoardRepository;
import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.security.AuthService;
import com.tails.common.security.LoginAttemptService;
import com.tails.common.util.FileStorage;
import com.tails.member.dto.LoginResponse;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import com.tails.member.dto.MemberResponse;
import com.tails.member.dto.MemberUpdateRequest;
import com.tails.member.dto.MyStatsResponse;
import com.tails.member.dto.PasswordChangeRequest;
import com.tails.review.ReviewRepository;
import com.tails.travel.TravelRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

// 회원 관련 비즈니스 로직 (회원가입/로그인/중복확인/내 정보 조회·수정)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final String UPLOAD_URL_PREFIX = "/uploads/";

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final LoginAttemptService loginAttemptService;
    private final FileStorage fileStorage;
    // 회원 탈퇴 시 좋아요 눌렀던 게시글의 likeCount를 벌크로 낮추는 용도(withdraw 참고)
    private final BoardRepository boardRepository;
    // 마이페이지 통계(getMyStats)용
    private final TravelRepository travelRepository;
    private final PlaceBookmarkRepository placeBookmarkRepository;
    private final ReviewRepository reviewRepository;

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
        if (!authService.isSignupEmailVerified(email)) {
            throw new CustomException(ErrorCode.EMAIL_NOT_VERIFIED);
        }

        Member member = Member.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .nickname(nickname)
                .build();
        member.markEmailVerified();

        Long memberId = memberRepository.save(member).getId();
        authService.clearSignupVerification(email);
        return memberId;
    }

    // 컨트롤러가 본문(LoginResponse)과 별개로 Refresh Token 쿠키를 Set-Cookie에 실어야 해서 묶어서 반환
    public record LoginResult(LoginResponse response, ResponseCookie refreshCookie) {
    }

    @Transactional
    public LoginResult login(MemberLoginRequest request) {
        Member member = memberRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));

        if (member.isLocked()) {
            throw new CustomException(ErrorCode.ACCOUNT_LOCKED);
        }

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            loginAttemptService.recordFailure(member.getId());
            throw new CustomException(ErrorCode.LOGIN_FAILED);
        }

        loginAttemptService.recordSuccess(member);

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
    public void updateFcmToken(Long memberId, String fcmToken) {
        Member member = getMemberOrThrow(memberId);
        member.changeFcmToken(fcmToken);
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

    // Board(게시글) 자체는 남기고(탈퇴한 회원으로 표시), 좋아요 눌렀던 글의 likeCount만 벌크로 낮춤
    @Transactional
    public void withdraw(Long memberId) {
        Member member = getMemberOrThrow(memberId);
        List<Long> likedBoardIds = member.getBoardLikes().stream()
                .map(like -> like.getBoard().getId())
                .toList();
        if (!likedBoardIds.isEmpty()) {
            boardRepository.decreaseLikeCountBulk(likedBoardIds);
        }
        authService.revokeSession(memberId);
        memberRepository.delete(member);
    }

    // 기존 프로필 이미지가 우리 서버 파일이면 교체 시 함께 정리
    @Transactional
    public String uploadProfileImage(Long memberId, MultipartFile file) {
        Member member = getMemberOrThrow(memberId);

        String storedFileName = fileStorage.store(file);
        deleteStoredProfileImageIfExists(member);
        member.changeProfileImg(UPLOAD_URL_PREFIX + storedFileName);
        return member.getProfileImg();
    }

    @Transactional
    public void deleteProfileImage(Long memberId) {
        Member member = getMemberOrThrow(memberId);
        deleteStoredProfileImageIfExists(member);
        member.changeProfileImg(null);
    }

    private void deleteStoredProfileImageIfExists(Member member) {
        String currentUrl = member.getProfileImg();
        if (currentUrl != null && currentUrl.startsWith(UPLOAD_URL_PREFIX)) {
            fileStorage.deleteAfterCommit(currentUrl.substring(UPLOAD_URL_PREFIX.length()));
        }
    }

    public MyStatsResponse getMyStats(Long memberId) {
        return new MyStatsResponse(
                travelRepository.countByMember_Id(memberId),
                placeBookmarkRepository.countByMemberId(memberId),
                reviewRepository.countByMember_Id(memberId));
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
