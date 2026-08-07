package com.tails.admin;

import com.tails.admin.dto.AdminMemberResponse;
import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import com.tails.member.MemberRole;
import com.tails.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 관리자 전용 기능의 비즈니스 로직
@Service
@RequiredArgsConstructor
public class AdminService {

    private final MemberRepository memberRepository;
    private final MemberService memberService;

    // 최초 관리자 계정은 이 API로 만들 수 없음. DB에서 직접 UPDATE로 지정
    // MANAGER는 ADMIN 권한을 부여하거나, ADMIN의 권한을 바꿀 수 없음 (USER<->MANAGER만 가능)
    @Transactional
    public void changeMemberRole(Long currentMemberId, MemberRole currentRole, Long memberId, MemberRole role) {
        if (currentMemberId.equals(memberId)) {
            throw new CustomException(ErrorCode.CANNOT_CHANGE_OWN_ROLE);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        if (currentRole == MemberRole.MANAGER && (member.getRole() == MemberRole.ADMIN || role == MemberRole.ADMIN)) {
            throw new CustomException(ErrorCode.CANNOT_CHANGE_ADMIN_ROLE);
        }
        member.changeRole(role);
    }

    // 이메일/닉네임 키워드로 회원 검색
    public Page<AdminMemberResponse> getMembers(String keyword, Pageable pageable) {
        String trimmed = keyword == null ? "" : keyword.trim();
        return memberRepository.findByEmailContainingOrNicknameContaining(trimmed, trimmed, pageable)
                .map(AdminMemberResponse::from);
    }

    // 문제 회원 강제 추방 - 기존 회원 탈퇴 로직 재사용
    // MANAGER는 ADMIN/MANAGER를 추방할 수 없음 (USER만 추방 가능)
    @Transactional
    public void expelMember(Long adminMemberId, MemberRole adminRole, Long targetMemberId) {
        if (adminMemberId.equals(targetMemberId)) {
            throw new CustomException(ErrorCode.CANNOT_EXPEL_SELF);
        }
        Member target = memberRepository.findById(targetMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
        if (adminRole == MemberRole.MANAGER && target.getRole().isStaff()) {
            throw new CustomException(ErrorCode.CANNOT_EXPEL_STAFF);
        }
        memberService.withdraw(targetMemberId);
    }
}
