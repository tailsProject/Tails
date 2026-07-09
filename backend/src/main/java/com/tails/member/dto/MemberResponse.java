package com.tails.member.dto;

import com.tails.member.Member;
import com.tails.pet.dto.PetResponse;

import java.time.LocalDateTime;
import java.util.List;

// 내 정보 조회(GET /api/members/me) 응답
public record MemberResponse(
        Long memberId,
        String email,
        String nickname,
        String profileImg,
        LocalDateTime createdAt,
        List<PetResponse> pets
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getNickname(),
                member.getProfileImg(),
                member.getCreatedAt(),
                member.getPets().stream().map(PetResponse::from).toList()
        );
    }
}
