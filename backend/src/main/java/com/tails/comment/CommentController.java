package com.tails.comment;

import com.tails.comment.dto.CommentCreateRequest;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
}
