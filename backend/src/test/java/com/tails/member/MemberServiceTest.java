package com.tails.member;

import com.tails.bookmark.PlaceBookmarkRepository;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.common.security.AuthService;
import com.tails.common.security.LoginAttemptService;
import com.tails.common.util.FileStorage;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import com.tails.review.ReviewRepository;
import com.tails.travel.TravelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// MemberService 단위테스트 - 중복 체크/비밀번호 검증/로그인 실패 처리
@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthService authService;
    @Mock
    private LoginAttemptService loginAttemptService;
    @Mock
    private FileStorage fileStorage;
    @Mock
    private TravelRepository travelRepository;
    @Mock
    private PlaceBookmarkRepository placeBookmarkRepository;
    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private MemberService memberService;

    private MemberJoinRequest joinRequest;

    @BeforeEach
    void setUp() {
        joinRequest = new MemberJoinRequest("test@mail.com", "Test1234!", "Test1234!", "tester");
    }

    @Test
    void 비밀번호와_비밀번호확인이_다르면_가입에_실패한다() {
        MemberJoinRequest mismatched = new MemberJoinRequest("test@mail.com", "Test1234!", "Other1234!", "tester");

        assertThatThrownBy(() -> memberService.join(mismatched))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_NOT_MATCHED);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void 이미_가입된_이메일이면_가입에_실패한다() {
        when(memberRepository.existsByEmail("test@mail.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.join(joinRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_EMAIL);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void 이미_사용중인_닉네임이면_가입에_실패한다() {
        when(memberRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(memberRepository.existsByNickname("tester")).thenReturn(true);

        assertThatThrownBy(() -> memberService.join(joinRequest))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_NICKNAME);

        verify(memberRepository, never()).save(any());
    }

    @Test
    void 이메일은_대소문자와_공백을_정규화해서_저장한다() {
        MemberJoinRequest request = new MemberJoinRequest(" Test@Mail.com ", "Test1234!", "Test1234!", "tester");
        when(memberRepository.existsByEmail("test@mail.com")).thenReturn(false);
        when(memberRepository.existsByNickname("tester")).thenReturn(false);
        when(authService.isSignupEmailVerified("test@mail.com")).thenReturn(true);
        when(passwordEncoder.encode("Test1234!")).thenReturn("encoded");
        when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

        memberService.join(request);

        verify(memberRepository).existsByEmail("test@mail.com");
    }

    @Test
    void 존재하지_않는_이메일로_로그인하면_LOGIN_FAILED() {
        MemberLoginRequest request = new MemberLoginRequest("nobody@mail.com", "Test1234!");
        when(memberRepository.findByEmail("nobody@mail.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    void 잠긴_계정은_비밀번호가_맞아도_ACCOUNT_LOCKED() {
        MemberLoginRequest request = new MemberLoginRequest("test@mail.com", "Test1234!");
        Member locked = Member.builder().email("test@mail.com").password("encoded").nickname("tester").build();
        locked.lock(java.time.LocalDateTime.now().plusMinutes(10));
        when(memberRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(locked));

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ACCOUNT_LOCKED);
    }

    @Test
    void 비밀번호가_틀리면_실패기록을_남기고_LOGIN_FAILED() {
        MemberLoginRequest request = new MemberLoginRequest("test@mail.com", "wrong");
        Member member = Member.builder().email("test@mail.com").password("encoded").nickname("tester").build();
        when(memberRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("wrong", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);

        verify(loginAttemptService, times(1)).recordFailure(member.getId());
        verify(authService, never()).issueTokens(any());
    }

    @Test
    void 로그인_성공하면_토큰을_발급하고_성공기록을_남긴다() {
        MemberLoginRequest request = new MemberLoginRequest("test@mail.com", "Test1234!");
        Member member = Member.builder().email("test@mail.com").password("encoded").nickname("tester").build();
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "token-value").build();
        when(memberRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(member));
        when(passwordEncoder.matches("Test1234!", "encoded")).thenReturn(true);
        when(authService.issueTokens(member))
                .thenReturn(new AuthService.IssuedTokens("access-token", cookie));

        MemberService.LoginResult result = memberService.login(request);

        assertThat(result.response().accessToken()).isEqualTo("access-token");
        assertThat(result.response().nickname()).isEqualTo("tester");
        verify(loginAttemptService).recordSuccess(member);
        verify(loginAttemptService, never()).recordFailure(anyLong());
    }
}
