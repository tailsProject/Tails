package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 게시글자유게시판 게시글 관련 REST API
@RestController
@RequestMapping("/api/boards")
@RequiredArgsConstructor
@Tag(name = "Board", description = "게시글 관련 API")
public class BoardController {

    private final BoardService boardService;

    @PostMapping
    @Operation(
            summary = "게시글 작성",
            description = "로그인한 사용자에게 제목, 내용을 입력받아 새 게시글을 작성합니다."
    )
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid @RequestBody BoardCreateRequest request) {
        return ApiResponse.success(boardService.create(userDetails.getMemberId(), request));
    }
}
