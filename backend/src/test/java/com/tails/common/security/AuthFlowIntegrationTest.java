package com.tails.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 회원가입 → 로그인(JWT 발급) → 보호된 API 접근까지의 인증 흐름을 실제 필터체인으로 검증. */
class AuthFlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 토큰_없이_보호된_API에_접근하면_401과_ApiResponse_형식의_본문을_받는다() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 가입후_로그인하면_토큰이_발급되고_그_토큰으로_내정보_조회가_된다() throws Exception {
        markSignupEmailVerified("authflow@test.com");
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"authflow@test.com","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"authflow"}
                                """))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"authflow@test.com","password":"Test1234!"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andReturn().getResponse().getContentAsString();

        String accessToken = objectMapper.readTree(loginBody).at("/data/accessToken").asText();

        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("authflow@test.com"))
                .andExpect(jsonPath("$.data.nickname").value("authflow"));
    }

    @Test
    void 잘못된_비밀번호로_로그인하면_LOGIN_FAILED_에러를_받는다() throws Exception {
        markSignupEmailVerified("wrongpw@test.com");
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpw@test.com","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"wrongpw"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"wrongpw@test.com","password":"WrongPassword1!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("LOGIN_FAILED"));
    }

    @Test
    void 변조된_토큰으로_접근하면_401을_받는다() throws Exception {
        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.token.value"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }
}
