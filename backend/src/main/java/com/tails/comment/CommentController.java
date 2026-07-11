package com.tails.comment;

import com.tails.comment.dto.CommentCreateRequest;
import com.tails.comment.dto.CommentResponse;
import com.tails.comment.dto.CommentUpdateRequest;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 게시글 댓글 API. 로그인 필요
@RestController
@RequestMapping("/api/boards/{boardId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comment", description = "게시글 댓글 API")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다. 로그인 필요.")
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long boardId,
                                     @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.success(commentService.create(userDetails.getMemberId(), boardId, request));
    }

    @GetMapping
    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다. 로그인 불필요.")
    public ApiResponse<List<CommentResponse>> getList(@PathVariable Long boardId) {
        return ApiResponse.success(commentService.getList(boardId));
    }

    @PatchMapping("/{commentId}")
    @Operation(summary = "댓글 수정", description = "commentId 댓글을 수정합니다. 작성자 본인만 가능.")
    public ApiResponse<Void> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long boardId,
                                     @PathVariable Long commentId,
                                     @Valid @RequestBody CommentUpdateRequest request) {
        commentService.update(userDetails.getMemberId(), boardId, commentId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "댓글 삭제", description = "commentId 댓글을 삭제합니다. 작성자 본인만 가능.")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long boardId,
                                     @PathVariable Long commentId) {
        commentService.delete(userDetails.getMemberId(), boardId, commentId);
        return ApiResponse.success();
    }
}
