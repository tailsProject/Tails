package com.tails.pet;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.pet.dto.PetCreateRequest;
import com.tails.pet.dto.PetResponse;
import com.tails.pet.dto.PetUpdateRequest;
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

    @PutMapping("/{petId}")
    @Operation(summary = "반려동물 수정", description = "petId 반려동물 정보를 수정합니다. 본인 소유만 가능.")
    public ApiResponse<Void> update(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long petId,
                                     @Valid @RequestBody PetUpdateRequest request) {
        petService.update(userDetails.getMemberId(), petId, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{petId}")
    @Operation(summary = "반려동물 삭제", description = "petId 반려동물을 삭제합니다. 본인 소유만 가능.")
    public ApiResponse<Void> delete(@AuthenticationPrincipal CustomUserDetails userDetails,
                                     @PathVariable Long petId) {
        petService.delete(userDetails.getMemberId(), petId);
        return ApiResponse.success();
    }
}
