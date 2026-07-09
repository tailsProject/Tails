package com.tails.bookmark;

import com.tails.bookmark.dto.BookmarkToggleResponse;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 게시글 북마크 API. 로그인 필요 
@RestController
@RequiredArgsConstructor
@Tag(name = "Bookmark", description = "게시글 북마크 API")
public class BoardBookmarkController {

    private final BoardBookmarkService boardBookmarkService;

    @PostMapping("/api/boards/{boardId}/bookmark")
    @Operation(summary = "북마크 토글", description = "게시글 북마크를 추가/취소합니다. 로그인 필요.")
    public ApiResponse<BookmarkToggleResponse> toggleBookmark(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                               @PathVariable Long boardId) {
        boolean bookmarked = boardBookmarkService.toggleBookmark(userDetails.getMemberId(), boardId);
        return ApiResponse.success(new BookmarkToggleResponse(bookmarked));
    }
}
