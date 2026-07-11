package com.tails.bookmark;

import com.tails.bookmark.dto.BookmarkToggleResponse;
import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.place.dto.PlaceResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// 장소 찜 API. 로그인 필요
@RestController
@RequiredArgsConstructor
@Tag(name = "PlaceBookmark", description = "장소 찜 API")
public class PlaceBookmarkController {

    private final PlaceBookmarkService placeBookmarkService;

    @PostMapping("/api/places/{placeId}/bookmark")
    @Operation(summary = "장소 찜 토글", description = "장소 찜을 추가/취소합니다. 로그인 필요.")
    public ApiResponse<BookmarkToggleResponse> toggleBookmark(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                               @PathVariable Long placeId) {
        boolean bookmarked = placeBookmarkService.toggleBookmark(userDetails.getMemberId(), placeId);
        return ApiResponse.success(new BookmarkToggleResponse(bookmarked));
    }

    @GetMapping("/api/bookmarks/places")
    @Operation(summary = "내가 찜한 장소 목록 조회", description = "로그인한 회원이 찜한 장소 목록을 최근 찜 순으로 조회합니다. 로그인 필요.")
    public ApiResponse<Page<PlaceResponse>> getMyBookmarkedPlaces(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                   @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.success(placeBookmarkService.getMyBookmarks(userDetails.getMemberId(), pageable));
    }
}
