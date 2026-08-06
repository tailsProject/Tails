package com.tails.image;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tails.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// 게시글 이미지 순서 변경 회귀테스트 - UNIQUE 제약 위반 없이 재배정되는지 확인
class ImageReorderIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private long boardId;

    @BeforeEach
    void setUp() throws Exception {
        markSignupEmailVerified("image-reorder@test.com");
        mockMvc.perform(post("/api/members/join")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"image-reorder@test.com","password":"Test1234!","passwordConfirm":"Test1234!","nickname":"imagereorder"}
                                """))
                .andExpect(status().isOk());

        String loginBody = mockMvc.perform(post("/api/members/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"image-reorder@test.com","password":"Test1234!"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(loginBody).at("/data/accessToken").asText();

        String boardBody = mockMvc.perform(post("/api/boards")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"이미지 순서 테스트","content":"content"}
                                """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        boardId = objectMapper.readTree(boardBody).at("/data").asLong();
    }

    private MockMultipartFile pngFile(String name) {
        return new MockMultipartFile("files", name, "image/png", new byte[]{1, 2, 3, 4});
    }

    @Test
    void 이미지_세장을_올리고_첫번째와_세번째_순서를_바꿔도_UNIQUE_제약_위반_없이_성공한다() throws Exception {
        String uploadBody = mockMvc.perform(multipart("/api/boards/" + boardId + "/images")
                        .file(pngFile("a.png"))
                        .file(pngFile("b.png"))
                        .file(pngFile("c.png"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode images = objectMapper.readTree(uploadBody).at("/data");
        long firstId = images.get(0).get("imageId").asLong();
        long secondId = images.get(1).get("imageId").asLong();
        long thirdId = images.get(2).get("imageId").asLong();

        mockMvc.perform(patch("/api/boards/" + boardId + "/images/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"imageIds":[%d,%d,%d]}
                                """.formatted(thirdId, secondId, firstId)))
                .andExpect(status().isOk());

        String listBody = mockMvc.perform(get("/api/boards/" + boardId + "/images"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode reordered = objectMapper.readTree(listBody).at("/data");

        assertThat(reordered.get(0).get("imageId").asLong()).isEqualTo(thirdId);
        assertThat(reordered.get(0).get("sequence").asInt()).isEqualTo(0);
        assertThat(reordered.get(1).get("imageId").asLong()).isEqualTo(secondId);
        assertThat(reordered.get(2).get("imageId").asLong()).isEqualTo(firstId);
        assertThat(reordered.get(2).get("sequence").asInt()).isEqualTo(2);
    }
}
