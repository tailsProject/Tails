package com.tails.common.security;

import com.tails.common.mail.EmailToken;
import com.tails.common.mail.EmailTokenRepository;
import com.tails.common.mail.EmailTokenType;
import com.tails.common.mail.EmailVerificationCodeRepository;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 이메일 인증/비밀번호 재설정 회귀테스트 - 계정 열거 방지(가입 안 된 이메일도 성공 응답) + 정상 흐름 확인
// JavaMailSender는 @MockitoBean으로 대체, 토큰은 EmailTokenRepository에서 직접 조회
class EmailAuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private EmailTokenRepository emailTokenRepository;
    @Autowired
    private EmailVerificationCodeRepository emailVerificationCodeRepository;
    @MockitoBean
    private JavaMailSender javaMailSender;

    private void join(String email, String nickname) throws Exception {
        markSignupEmailVerified(email);
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"%s"}
                                """.formatted(email, nickname)))
                .andExpect(status().isOk());
    }

    @Test
    void 가입_안된_이메일로_인증메일_요청해도_MEMBER_NOT_FOUND가_아니라_그냥_성공응답이_온다() throws Exception {
        mockMvc.perform(post("/api/auth/email/verify-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody-at-all@test.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 가입_안된_이메일로_비번재설정_요청해도_MEMBER_NOT_FOUND가_아니라_그냥_성공응답이_온다() throws Exception {
        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody-at-all-2@test.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void 이메일_인증_요청후_토큰으로_검증하면_emailVerified가_true가_된다() throws Exception {
        // 링크 기반 재인증은 미인증(레거시) 회원을 직접 만들어 검증
        String email = "verify-flow@test.com";
        memberRepository.save(Member.builder()
                .email(email)
                .password("encoded")
                .nickname("verifyflow")
                .build());

        mockMvc.perform(post("/api/auth/email/verify-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        Member member = memberRepository.findByEmail(email).orElseThrow();
        EmailToken token = emailTokenRepository.findByMemberIdAndType(member.getId(), EmailTokenType.SIGNUP_VERIFY)
                .orElseThrow();

        mockMvc.perform(get("/api/auth/email/verify").param("token", token.getToken()))
                .andExpect(status().isOk());

        assertThat(memberRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void 잘못된_토큰으로_이메일_인증하면_EMAIL_TOKEN_INVALID() throws Exception {
        mockMvc.perform(get("/api/auth/email/verify").param("token", "no-such-token"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMAIL_TOKEN_INVALID"));
    }

    @Test
    void 비번재설정_요청후_토큰으로_새비번_설정하면_새비번으로_로그인된다() throws Exception {
        String email = "reset-flow@test.com";
        join(email, "resetflow");

        mockMvc.perform(post("/api/auth/password/reset-request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        Member member = memberRepository.findByEmail(email).orElseThrow();
        EmailToken token = emailTokenRepository.findByMemberIdAndType(member.getId(), EmailTokenType.PASSWORD_RESET)
                .orElseThrow();

        mockMvc.perform(post("/api/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"NewPass1234!","newPasswordConfirm":"NewPass1234!"}
                                """.formatted(token.getToken())))
                .andExpect(status().isOk());

        // 옛 비밀번호로는 더 이상 로그인이 안 되고
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());

        // 새 비밀번호로는 로그인된다.
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"NewPass1234!"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void 이메일_인증번호_확인_없이_가입하면_EMAIL_NOT_VERIFIED() throws Exception {
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"no-code@test.com","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"nocode"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));
    }

    @Test
    void 인증번호_발송후_확인하면_가입이_성공하고_emailVerified가_true다() throws Exception {
        String email = "code-flow@test.com";
        mockMvc.perform(post("/api/auth/email/signup-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        String code = emailVerificationCodeRepository.findByEmail(email).orElseThrow().getCode();

        mockMvc.perform(post("/api/auth/email/signup-code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"%s"}
                                """.formatted(email, code)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"codeflow"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        assertThat(memberRepository.findByEmail(email).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void 틀린_인증번호로_확인하면_EMAIL_CODE_INVALID() throws Exception {
        String email = "wrong-code@test.com";
        mockMvc.perform(post("/api/auth/email/signup-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/email/signup-code/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","code":"000000"}
                                """.formatted(email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("EMAIL_CODE_INVALID"));
    }
}
