package com.tails.common.security;

import com.tails.member.MemberRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// 인증된 요청에서 지금 로그인한 회원 정보를 담는 UserDetails 구현체
@Getter
public class CustomUserDetails implements UserDetails {

    private final Long memberId;
    private final String email;
    private final MemberRole role;

    public CustomUserDetails(Long memberId, String email, MemberRole role) {
        this.memberId = memberId;
        this.email = email;
        this.role = role;
    }

    // hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN" 권한 문자열을 찾으므로 접두사를 직접 붙인다
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    // JWT 검증 용도로만 쓰여 비밀번호 재검증이 필요 없으므로 null 반환
    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
