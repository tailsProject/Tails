package com.tails.board;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// DRAFT(임시저장) 게시글 가시성 회귀테스트 - 좋아요/북마크/댓글/이미지 전 API에서 Board.isVisibleTo() 적용 확인
class BoardDraftVisibilityIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String ownerToken;
    private String otherToken;
    private long draftBoardId;

    @BeforeEach
    void setUp() throws Exception {
        // 매번 새 이메일 사용(DUPLICATE_EMAIL 방지)
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        ownerToken = join("draft-owner-" + suffix + "@test.com", "dOwner" + suffix);
        otherToken = join("draft-other-" + suffix + "@test.com", "dOther" + suffix);

        String body = mockMvc.perform(post("/api/boards/draft")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"비밀 초안","content":"아직 발행 안 함"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        draftBoardId = objectMapper.readTree(body).at("/data").asLong();
    }

    private String join(String email, String nickname) throws Exception {
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
    void 비로그인_상태로_남의_임시저장_글에_댓글목록_조회하면_BOARD_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/boards/" + draftBoardId + "/comments"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    void 비로그인_상태로_남의_임시저장_글의_이미지목록_조회하면_BOARD_NOT_FOUND() throws Exception {
        mockMvc.perform(get("/api/boards/" + draftBoardId + "/images"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    void 로그인했지만_작성자가_아니면_남의_임시저장_글에_좋아요를_누를_수_없다() throws Exception {
        mockMvc.perform(post("/api/boards/" + draftBoardId + "/like")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    void 로그인했지만_작성자가_아니면_남의_임시저장_글을_북마크할_수_없다() throws Exception {
        mockMvc.perform(post("/api/boards/" + draftBoardId + "/bookmark")
                        .header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    void 작성자가_아니면_남의_임시저장_글에_댓글을_달_수_없다() throws Exception {
        mockMvc.perform(post("/api/boards/" + draftBoardId + "/comments")
                        .header("Authorization", "Bearer " + otherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"몰래 단 댓글","parentId":null}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BOARD_NOT_FOUND"));
    }

    @Test
    void 작성자_본인은_자기_임시저장_글에_좋아요_북마크_댓글이_전부_가능하다() throws Exception {
        mockMvc.perform(post("/api/boards/" + draftBoardId + "/like")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/boards/" + draftBoardId + "/bookmark")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/boards/" + draftBoardId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"content":"내 초안에 내가 단 댓글","parentId":null}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/boards/" + draftBoardId + "/comments")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].content").value("내 초안에 내가 단 댓글"));
    }
}
