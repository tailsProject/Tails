package com.tails.board;

import com.tails.board.dto.BoardCreateRequest;
import com.tails.board.dto.BoardDetailResponse;
import com.tails.board.dto.BoardResponse;
import com.tails.board.dto.BoardUpdateRequest;
import com.tails.board.dto.LikeToggleResponse;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
            description = "사용자에게 제목, 내용을 입력받아 새 게시글을 작성합니다. 로그인 필요."
    )
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid @RequestBody BoardCreateRequest request) {
        return ApiResponse.success(boardService.create(userDetails.getMemberId(), request));
    }

    // keyword로 검색, sortBy=popular로 인기순 정렬(keyword와 동시 사용 시 keyword 우선)
    // 정렬 파라미터명이 sort가 아니라 sortBy인 이유: sort는 Spring Data Pageable의 예약어라 충돌함
    @GetMapping
    @Operation(
            summary = "게시글 목록 조회",
            description = "게시글 목록을 페이지 단위로 조회합니다. keyword로 검색, sortBy=popular로 인기순 정렬. 로그인 불필요."
    )
    public ApiResponse<Page<BoardResponse>> getList(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortBy) {
        return ApiResponse.success(boardService.getList(pageable, keyword, sortBy));
    }

    @PostMapping("/draft")
    @Operation(
            summary = "게시글 임시저장",
            description = "작성 중인 글을 임시저장(DRAFT)합니다. 목록에는 노출되지 않습니다. 로그인 필요."
    )
    public ApiResponse<Long> createDraft(@AuthenticationPrincipal CustomUserDetails userDetails,
                                          @Valid @RequestBody BoardCreateRequest request) {
        return ApiResponse.success(boardService.createDraft(userDetails.getMemberId(), request));
    }

    @GetMapping("/drafts")
    @Operation(
            summary = "내 임시저장 목록 조회",
            description = "로그인한 회원의 임시저장(DRAFT) 게시글 목록을 조회합니다. 로그인 필요."
    )
    public ApiResponse<Page<BoardResponse>> getMyDrafts(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                         @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(boardService.getMyDrafts(userDetails.getMemberId(), pageable));
    }

    @PutMapping("/{boardId}/publish")
    @Operation(
            summary = "임시저장 글 발행",
            description = "boardId 임시저장 글을 수정 내용과 함께 발행(PUBLISHED)합니다. 작성자 본인만 가능."
    )
    public ApiResponse<Void> publish(@AuthenticationPrincipal CustomUserDetails userDetails,
                                      @PathVariable Long boardId,
                                      @Valid @RequestBody BoardUpdateRequest request) {
        boardService.publish(userDetails.getMemberId(), boardId, request);
        return ApiResponse.success();
    }

    @GetMapping("/{boardId}")
    @Operation(
            summary = "게시글 상세 조회",
            description = "boardId에 해당하는 게시글 상세 정보를 조회합니다. 로그인 불필요."
    )
    public ApiResponse<BoardDetailResponse> getDetail(@PathVariable Long boardId) {
        return ApiResponse.success(boardService.getDetail(boardId));
    }

    @PatchMapping("/{boardId}")
    @Operation(
            summary = "게시글 수정",
            description = "boardId에 해당하는 게시글을 수정합니다. 작성자 본인만 수정할 수 있습니다. 로그인 필요."
    )
    public ApiResponse<Void> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long boardId,
                                     @Valid @RequestBody BoardUpdateRequest request) {
        boardService.update(userDetails.getMemberId(), boardId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{boardId}")
    @Operation(
            summary = "게시글 삭제",
            description = "boardId에 해당하는 게시글을 삭제합니다. 작성자 본인만 수정할 수 있습니다. 로그인 필요."
    )
    public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long boardId) {
        boardService.delete(userDetails.getMemberId(), boardId);
        return ApiResponse.success();
    }

    @PostMapping("/{boardId}/like")
    @Operation(
            summary = "좋아요 토글",
            description = "게시글 좋아요를 추가, 취소합니다. 로그인 필요."
    )
    public ApiResponse<LikeToggleResponse> toggleLike(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @PathVariable Long boardId) {
        return ApiResponse.success(boardService.toggleLike(userDetails.getMemberId(), boardId));
    }
}
