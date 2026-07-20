package com.tails.pet;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.pet.dto.PetCreateRequest;
import com.tails.pet.dto.PetResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 반려동물 CRUD API. 전부 로그인 필요
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
@Tag(name = "Pet", description = "반려동물 등록 / 목록 조회 / 수정 / 삭제 API")
public class PetController {

    private final PetService petService;

    @PostMapping
    @Operation(summary = "반려동물 등록", description = "새 반려동물을 등록합니다. 로그인 필요.")
    public ApiResponse<Long> create(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @Valid @RequestBody PetCreateRequest request) {
        return ApiResponse.success(petService.create(userDetails.getMemberId(), request));
    }

    @GetMapping
    @Operation(summary = "내 반려동물 목록 조회", description = "로그인한 회원이 등록한 반려동물 목록을 조회합니다. 로그인 필요.")
    public ApiResponse<List<PetResponse>> getMyPets(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(petService.getMyPets(userDetails.getMemberId()));
    }
}
