package com.tails.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Refresh Token 회귀테스트 - JWT type 클레임 구분, 재발급/로그아웃/회전 동작 확인
class RefreshTokenIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private record LoginResult(String accessToken, Cookie refreshCookie) {
    }

    private LoginResult join(String email, String nickname) throws Exception {
        markSignupEmailVerified(email);
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"%s"}
                                """.formatted(email, nickname)))
                .andExpect(status().isOk());

        MockHttpServletResponse response = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse();

        String accessToken = objectMapper.readTree(response.getContentAsString()).at("/data/accessToken").asText();
        Cookie refreshCookie = response.getCookie("refreshToken");
        assertThat(refreshCookie).isNotNull();
        return new LoginResult(accessToken, refreshCookie);
    }

    @Test
    void Refresh_Token을_Authorization_헤더에_그대로_넣으면_인증되지_않는다() throws Exception {
        LoginResult login = join("refresh-as-access@test.com", "refreshasaccess");

        mockMvc.perform(get("/api/members/me").header("Authorization", "Bearer " + login.accessToken()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + login.refreshCookie().getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    void 쿠키의_Refresh_Token으로_재발급하면_새_Access_Token을_받는다() throws Exception {
        LoginResult login = join("reissue-ok@test.com", "reissueok");

        mockMvc.perform(post("/api/auth/reissue").cookie(login.refreshCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").exists());
    }

    @Test
    void Access_Token을_Refresh_Token_쿠키자리에_넣어_재발급을_시도하면_거절된다() throws Exception {
        LoginResult login = join("access-as-refresh@test.com", "accessasrefresh");

        Cookie fakeRefreshCookie = new Cookie("refreshToken", login.accessToken());
        mockMvc.perform(post("/api/auth/reissue").cookie(fakeRefreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 로그아웃_후에는_그_Refresh_Token으로_재발급이_안_된다() throws Exception {
        LoginResult login = join("logout-then-reissue@test.com", "logoutreissue");

        mockMvc.perform(post("/api/auth/logout").cookie(login.refreshCookie()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reissue").cookie(login.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void 재발급으로_회전된_옛_Refresh_Token은_재사용할_수_없다() throws Exception {
        LoginResult login = join("rotate@test.com", "rotateuser");

        // 토큰 iat/exp가 초 단위라 초 경계를 넘겨야 실제로 다른 토큰이 발급됨
        Thread.sleep(1100);

        mockMvc.perform(post("/api/auth/reissue").cookie(login.refreshCookie()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/reissue").cookie(login.refreshCookie()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
    }
}
