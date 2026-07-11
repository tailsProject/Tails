package com.tails.image;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.image.dto.ImageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// 게시글 이미지 업로드/조회/삭제 API
@RestController
@RequiredArgsConstructor
@Tag(name = "Image", description = "게시글 이미지 업로드 / 조회 / 삭제 API")
public class ImageController {

    private final ImageService imageService;

    @PostMapping(value = "/api/boards/{boardId}/images", consumes = "multipart/form-data")
    @Operation(summary = "게시글 이미지 업로드", description = "게시글에 이미지를 여러 장 업로드합니다. 게시글 작성자 본인만 가능.")
    public ApiResponse<List<ImageResponse>> uploadBoardImages(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                                @PathVariable Long boardId,
                                                                @RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.success(imageService.uploadForBoard(userDetails.getMemberId(), boardId, files));
    }

    @GetMapping("/api/boards/{boardId}/images")
    @Operation(summary = "게시글 이미지 목록 조회", description = "게시글에 첨부된 이미지 목록을 조회합니다. 로그인 불필요.")
    public ApiResponse<List<ImageResponse>> getBoardImages(@PathVariable Long boardId) {
        return ApiResponse.success(imageService.getByBoard(boardId));
    }

    @DeleteMapping("/api/images/{imageId}")
    @Operation(summary = "이미지 삭제", description = "imageId 이미지를 삭제합니다. 게시글 작성자 본인만 가능.")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long imageId) {
        imageService.delete(userDetails.getMemberId(), imageId);
        return ApiResponse.success();
    }
}
