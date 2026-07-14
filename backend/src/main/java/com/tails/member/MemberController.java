package com.tails.member;

import com.tails.common.response.ApiResponse;
import com.tails.common.security.CustomUserDetails;
import com.tails.member.dto.AvailabilityResponse;
import com.tails.member.dto.LoginResponse;
import com.tails.member.dto.MemberJoinRequest;
import com.tails.member.dto.MemberLoginRequest;
import com.tails.member.dto.MemberResponse;
import com.tails.member.dto.MemberUpdateRequest;
import com.tails.member.dto.PasswordChangeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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

    // Access Token은 응답 본문, Refresh Token은 httpOnly 쿠키로 발급
    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "이메일, 비밀번호로 로그인합니다. Access Token은 본문, Refresh Token은 httpOnly 쿠키로 발급됩니다."
    )
    public ApiResponse<LoginResponse> login(@Valid @RequestBody MemberLoginRequest request,
                                             HttpServletResponse response) {
        MemberService.LoginResult result = memberService.login(request);
        response.addHeader(HttpHeaders.SET_COOKIE, result.refreshCookie().toString());
        return ApiResponse.success(result.response());
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

    @GetMapping("/me")
    @Operation(
            summary = "내 정보 조회",
            description = "로그인한 회원의 정보를 조회합니다."
    )
    public ApiResponse<MemberResponse> getMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ApiResponse.success(memberService.getMyInfo(userDetails.getMemberId()));
    }

    @PatchMapping("/me")
    @Operation(
            summary = "내 정보 수정",
            description = "로그인한 회원의 닉네임/프로필 사진을 수정합니다."
    )
    public ApiResponse<Void> updateMyInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody MemberUpdateRequest request) {
        memberService.updateMyInfo(userDetails.getMemberId(), request);
        return ApiResponse.success();
    }

    @PatchMapping("/me/password")
    @Operation(
            summary = "비밀번호 변경",
            description = "로그인한 회원의 현재 비밀번호 확인 후 새 비밀번호로 변경합니다."
    )
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                             @Valid @RequestBody PasswordChangeRequest request) {
        memberService.changePassword(userDetails.getMemberId(), request);
        return ApiResponse.success();
    }

    @DeleteMapping("/me")
    @Operation(
            summary = "회원 탈퇴",
            description = "로그인한 회원 본인의 계정을 삭제합니다."
    )
    public ApiResponse<Void> withdraw(@AuthenticationPrincipal CustomUserDetails userDetails) {
        memberService.withdraw(userDetails.getMemberId());
        return ApiResponse.success();
    }
}
