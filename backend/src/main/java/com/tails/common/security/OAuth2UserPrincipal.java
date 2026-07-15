package com.tails.common.security;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

// 소셜 로그인 성공 직후의 인증 주체. CustomOAuth2UserService가 회원 매핑을 끝낸 뒤 memberId를 담아
// 넘기면, OAuth2SuccessHandler가 여기서 memberId만 꺼내 JWT를 발급한다.
public class OAuth2UserPrincipal implements OAuth2User {

    private final Long memberId;
    private final Map<String, Object> attributes;

    public OAuth2UserPrincipal(Long memberId, Map<String, Object> attributes) {
        this.memberId = memberId;
        this.attributes = attributes;
    }

    public Long getMemberId() {
        return memberId;
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
