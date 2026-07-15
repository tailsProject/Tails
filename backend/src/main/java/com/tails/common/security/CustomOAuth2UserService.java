package com.tails.common.security;

import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 소셜 로그인(카카오/구글/네이버) 사용자 정보를 우리 회원(Member)으로 매핑하는 서비스
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final MemberRepository memberRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        SocialProfile profile = extractProfile(provider, oAuth2User.getAttributes());

        Member member = memberRepository.findByProviderAndProviderId(provider, profile.providerId())
                .orElseGet(() -> join(provider, profile));

        return new OAuth2UserPrincipal(member.getId(), oAuth2User.getAttributes());
    }

    // provider마다 사용자 정보 응답 구조가 달라서 (providerId, email, nickname) 공통 형태로 맞춘다
    private SocialProfile extractProfile(String provider, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<String, Object> account = castMap(attributes.get("kakao_account"));
            Map<String, Object> profile = account != null ? castMap(account.get("profile")) : null;
            String email = account != null ? (String) account.get("email") : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;
            return validated(new SocialProfile(String.valueOf(attributes.get("id")), email, nickname));
        }
        if ("google".equals(provider)) {
            return validated(new SocialProfile(
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name")));
        }
        if ("naver".equals(provider)) {
            Map<String, Object> response = castMap(attributes.get("response"));
            String id = response != null ? (String) response.get("id") : null;
            String email = response != null ? (String) response.get("email") : null;
            String name = response != null ? (String) response.get("name") : null;
            return validated(new SocialProfile(id, email, name));
        }
        throw authError("unsupported_provider", "지원하지 않는 소셜 로그인입니다: " + provider);
    }

    // 소셜 계정 첫 로그인 = 자동 회원가입. 이미 일반 가입된 이메일이면 자동 연동하지 않고 막는다
    private Member join(String provider, SocialProfile profile) {
        String email = profile.email().trim().toLowerCase();
        if (memberRepository.existsByEmail(email)) {
            throw authError(ErrorCode.OAUTH_EMAIL_ALREADY_REGISTERED.name(), ErrorCode.OAUTH_EMAIL_ALREADY_REGISTERED.getMessage());
        }

        Member member = Member.builder()
                .email(email)
                .nickname(uniqueNickname(profile.nickname()))
                .provider(provider)
                .providerId(profile.providerId())
                .build();
        member.markEmailVerified();
        return memberRepository.save(member);
    }

    private String uniqueNickname(String base) {
        String name = (base == null || base.isBlank()) ? "여행자" : base.trim();
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        String candidate = name;
        while (memberRepository.existsByNickname(candidate)) {
            candidate = name + UUID.randomUUID().toString().substring(0, 4);
        }
        return candidate;
    }

    private SocialProfile validated(SocialProfile profile) {
        if (profile.providerId() == null || "null".equals(profile.providerId())) {
            throw authError("invalid_profile", "소셜 계정 정보를 가져오지 못했습니다.");
        }
        if (profile.email() == null || profile.email().isBlank()) {
            throw authError("email_required", "소셜 로그인에는 이메일 제공 동의가 필요합니다.");
        }
        return profile;
    }

    // 시큐리티 필터 내부라 GlobalExceptionHandler가 못 잡음 - OAuth2AuthenticationException으로 던져야
    // OAuth2FailureHandler로 넘어간다
    private OAuth2AuthenticationException authError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private record SocialProfile(String providerId, String email, String nickname) {
    }
}
