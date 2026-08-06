package com.tails.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

// 소셜 로그인 성공 후 인증 주체. memberId와 신규가입 여부를 담는다.
public class OAuth2UserPrincipal implements OAuth2User {

    private final Long memberId;
    private final boolean newMember;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(Long memberId, boolean newMember, Map<String, Object> attributes) {
        this.memberId = memberId;
        this.newMember = newMember;
        this.attributes = attributes;
    }

    public Long getMemberId() {
        return memberId;
    }

    public boolean isNewMember() {
        return newMember;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public String getName() {
        return String.valueOf(memberId);
    }
}
