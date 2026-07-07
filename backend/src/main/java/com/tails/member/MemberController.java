package com.tails.member;

import com.tails.common.response.ApiResponse;
import com.tails.member.dto.AvailabilityResponse;
import com.tails.member.dto.LoginResponse;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// 회원 관련 REST API
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@Tag(name = "Member", description = "회원 관련 API")
public class MemberController {

    private final MemberService memberService;

    @PostMapping("/join")
    @Operation(
            summary = "회원가입",
            description = "이메일, 비밀번호, 닉네임을 입력받아 신규 회원을 등록합니다."
    )
    public ApiResponse<Long> join(@Valid @RequestBody MemberJoinRequest request) {
        Long memberId = memberService.join(request);
        return ApiResponse.success(memberId);
    }

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "이메일, 비밀번호로 로그인하고 JWT를 발급받습니다."
    )
    public ApiResponse<LoginResponse> login(@Valid @RequestBody MemberLoginRequest request) {
        return ApiResponse.success(memberService.login(request));
    }

    @GetMapping("/check-email")
    @Operation(
            summary = "이메일 중복 확인",
            description = "입력한 이메일의 회원 가입 가능 여부를 확인합니다."
    )
    public ApiResponse<AvailabilityResponse> checkEmail(@RequestParam String email) {
        boolean available = !memberService.isEmailDuplicated(email);
        return ApiResponse.success(new AvailabilityResponse(available));
    }

    @GetMapping("/check-nickname")
    @Operation(
            summary = "닉네임 중복 확인",
            description = "입력한 닉네임의 사용 가능 여부를 확인합니다."
    )
    public ApiResponse<AvailabilityResponse> checkNickname(@RequestParam String nickname) {
        boolean available = !memberService.isNicknameDuplicated(nickname);
        return ApiResponse.success(new AvailabilityResponse(available));
    }
}
