package com.tails.member;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 본인 글에 좋아요 남긴 회원 탈퇴 회귀테스트 - decreaseLikeCountBulk 벌크 쿼리 동작 확인
class MemberWithdrawIntegrationTest extends AbstractIntegrationTest {

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

        return objectMapper.readTree(body).at("/data/accessToken").asText();
    }

    @Test
    void 본인_글에_좋아요를_남긴_상태로_탈퇴해도_성공하고_좋아요수가_줄어든다() throws Exception {
        String token = join("withdraw-self-like@test.com", "selfliker");

        String boardBody = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"withdraw regression test","content":"content"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long boardId = objectMapper.readTree(boardBody).at("/data").asLong();

        mockMvc.perform(post("/api/boards/" + boardId + "/like")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/members/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        String detailBody = mockMvc.perform(get("/api/boards/" + boardId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = objectMapper.readTree(detailBody).at("/data");

        assertThat(data.get("authorId").isNull()).isTrue();
        assertThat(data.get("likeCount").asInt()).isEqualTo(0);
    }

    // 회귀테스트: Member에 commentLikes cascade가 없으면 댓글 좋아요를 남긴 회원의 탈퇴가
    // comment_like.member_id FK 위반(DataIntegrityViolationException)으로 실패한다
    @Test
    void 댓글에_좋아요를_남긴_상태로_탈퇴해도_성공한다() throws Exception {
        String authorToken = join("commentlike-withdraw-author@test.com", "clwauthor");
        String likerToken = join("commentlike-withdraw-liker@test.com", "clwliker");

        String boardBody = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"comment like withdraw test","content":"content"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long boardId = objectMapper.readTree(boardBody).at("/data").asLong();

        String commentBody = mockMvc.perform(post("/api/boards/" + boardId + "/comments")
                        .header("Authorization", "Bearer " + authorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"nice post"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        long commentId = objectMapper.readTree(commentBody).at("/data").asLong();

        mockMvc.perform(post("/api/boards/" + boardId + "/comments/" + commentId + "/like")
                        .header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/members/me").header("Authorization", "Bearer " + likerToken))
                .andExpect(status().isOk());
    }
}
