package com.tails.admin;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.member.MemberRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 전용 기능의 비즈니스 로직. SecurityConfig의 hasRole("ADMIN")이 이미 접근을 막아주므로
// 여기서는 권한 재검증을 하지 않는다
@Service
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;

    // 최초 관리자 계정은 이 API로 만들 수 없다(닭과 달걀 문제) - DB에서 직접 UPDATE로 지정
    @Transactional
    public void changeMemberRole(Long memberId, MemberRole role) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        member.changeRole(role);
    }
}
