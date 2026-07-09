package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.board.dto.BoardDetailResponse;
import com.tails.board.dto.BoardResponse;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 게시글(자유게시판) 관련 REST API
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

    @GetMapping
    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글 목록을 페이지 단위로 조회합니다. 로그인 불필요."
    )
    public ApiResponse<Page<BoardResponse>> getList(@PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(boardService.getList(pageable));
    }

    @GetMapping("/{boardId}")
    @Operation(
            summary = "게시글 상세 조회",
            description = "boardId로 게시글 상세 정보를 조회합니다. 로그인 불필요."
    )
    public ApiResponse<BoardDetailResponse> getDetail(@PathVariable Long boardId) {
        return ApiResponse.success(boardService.getDetail(boardId));
    }
}
