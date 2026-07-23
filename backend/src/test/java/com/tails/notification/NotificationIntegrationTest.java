package com.tails.notification;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 좋아요/댓글/북마크 알림 생성 회귀테스트 - NotificationService REQUIRES_NEW 전파 확인
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String join(String email, String nickname) throws Exception {
        markSignupEmailVerified(email);
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"%s"}
                                """.formatted(email, nickname)))
                .andExpect(status().isOk());

        String body = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"Test1234!"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return json.at("/data/accessToken").asText();
    }

    @Test
    void 다른_사람_글에_좋아요를_누르면_글쓴이에게_알림이_생성된다() throws Exception {
        String ownerToken = join("notif-owner@test.com", "owner");
        String likerToken = join("notif-liker@test.com", "liker");

        String boardBody = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"notif test","content":"content"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long boardId = objectMapper.readTree(boardBody).at("/data").asLong();

        mockMvc.perform(post("/api/boards/" + boardId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk());

        String notifBody = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(notifBody).at("/data/content");
        assertThat(content).hasSize(1);
        assertThat(content.get(0).get("type").asText()).isEqualTo("LIKE");
        assertThat(content.get(0).get("read").asBoolean()).isFalse();
    }

    @Test
    void 본인_글에_본인이_좋아요를_눌러도_알림이_생성되지_않는다() throws Exception {
        String ownerToken = join("notif-self@test.com", "selfowner");

        String boardBody = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"self like test","content":"content"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long boardId = objectMapper.readTree(boardBody).at("/data").asLong();

        mockMvc.perform(post("/api/boards/" + boardId + "/like")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        String notifBody = mockMvc.perform(get("/api/notifications")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode content = objectMapper.readTree(notifBody).at("/data/content");
        assertThat(content).isEmpty();
    }
}
