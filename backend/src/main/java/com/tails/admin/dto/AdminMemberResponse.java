package com.tails.admin.dto;

import com.tails.member.Member;
import java.time.LocalDateTime;

// 관리자 회원 목록/검색 응답
public record AdminMemberResponse(
        Long memberId,
        String email,
        String nickname,
        String role,
        LocalDateTime createdAt
) {
    public static AdminMemberResponse from(Member member) {
        return new AdminMemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getRole().name(),
                member.getCreatedAt()
        );
    }
}
