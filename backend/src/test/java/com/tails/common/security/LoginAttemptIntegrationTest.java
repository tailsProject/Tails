package com.tails.common.security;

import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 로그인 실패 횟수 제한(브루트포스 방어) 회귀테스트 - login.max-attempts 기본값(5) 기준
class LoginAttemptIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final int MAX_ATTEMPTS = 5;

    private void join(String email, String nickname) throws Exception {
        markSignupEmailVerified(email);
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"%s"}
                                """.formatted(email, nickname)))
                .andExpect(status().isOk());
    }

    private void loginWithWrongPassword(String email) throws Exception {
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"WrongPassword1!"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    void 로그인을_5회_연속_실패하면_비밀번호가_맞아도_ACCOUNT_LOCKED() throws Exception {
        String email = "lockout@test.com";
        join(email, "lockoutuser");

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            loginWithWrongPassword(email);
        }

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_LOCKED"));
    }

    @Test
    void 로그인_성공하면_실패카운트가_초기화되어_그_다음부터_다시_5회_유예가_생긴다() throws Exception {
        String email = "reset-count@test.com";
        join(email, "resetcount");

        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            loginWithWrongPassword(email);
        }

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isOk());

        for (int i = 0; i < MAX_ATTEMPTS - 1; i++) {
            loginWithWrongPassword(email);
        }
        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }
}
