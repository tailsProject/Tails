package com.tails.member;

// 회원 권한. DB에는 @Enumerated(STRING)으로 "USER"/"ADMIN" 문자열 그대로 저장
public enum MemberRole {
    USER,
    ADMIN
}
