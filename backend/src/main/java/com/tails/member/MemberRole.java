package com.tails.member;

// 회원 권한. DB에는 @Enumerated(STRING)으로 "USER"/"MANAGER"/"ADMIN" 문자열 그대로 저장
// ADMIN, MANAGER는 관리 기능(신고 처리, 게시글/댓글/리뷰 모더레이션, 회원 관리 페이지 접근)이 동일함
// MANAGER는 ADMIN/MANAGER를 추방하거나 ADMIN 권한을 부여/변경할 수 없음 (AdminService에서 검사)
public enum MemberRole {
    USER,
    MANAGER,
    ADMIN;

    public boolean isStaff() {
        return this == MANAGER || this == ADMIN;
    }
}
