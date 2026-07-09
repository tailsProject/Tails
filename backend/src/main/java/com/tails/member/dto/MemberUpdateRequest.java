package com.tails.member.dto;

import jakarta.validation.constraints.Size;

// 내 정보 수정(PATCH /api/members/me) 요청
public record MemberUpdateRequest(

        @Size(min = 2, max = 20, message = "닉네임은 2~20자여야 합니다.")
        String nickname,

        // 파일업로드 기능 완성 전까지는 URL 문자열만 받음. 
        // Member.profileImg 컬럼 길이(500)에 맞춰 검증
        @Size(max = 500, message = "프로필 이미지 URL이 너무 깁니다.")
        String profileImg
) {
}
