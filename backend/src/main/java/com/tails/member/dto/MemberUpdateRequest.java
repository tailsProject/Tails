package com.tails.member.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// 내 정보 수정(PATCH /api/members/me) 요청
public record MemberUpdateRequest(

        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        // 특수문자와 이모지는 허용하고 공백, 제어문자, 자음모음 단독 문자는 차단
        @Pattern(regexp = "^[^\\s\\p{Cntrl}\\u3131-\\u318E]*$", message = "닉네임에 사용할 수 없는 문자가 포함되어 있습니다.")
        String nickname,

        // 파일업로드 기능 완성 전까지는 URL 문자열만 받음.
        // Member.profileImg 컬럼 길이(500)에 맞춰 검증
        @Size(max = 500, message = "프로필 이미지 URL이 너무 깁니다.")
        String profileImg,

        // null이면 변경 안 함(다른 필드만 부분 수정하는 요청과 구분하기 위함)
        Boolean marketingAgreed
) {
}
