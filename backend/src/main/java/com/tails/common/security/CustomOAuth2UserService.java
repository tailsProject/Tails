package com.tails.common.security;

import com.tails.member.Member;
import com.tails.member.MemberRepository;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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

        boolean[] isNewMember = {false};
        Member member = memberRepository.findByProviderAndProviderId(provider, profile.providerId())
                .map(existing -> withProfileImageIfMissing(existing, profile))
                .orElseGet(() -> resolveByEmail(provider, profile, isNewMember));

        return new OAuth2UserPrincipal(member.getId(), isNewMember[0], oAuth2User.getAttributes());
    }

    // provider별 응답 구조를 (providerId, email, nickname, 프로필 사진 URL) 공통 형태로 변환
    private SocialProfile extractProfile(String provider, Map<String, Object> attributes) {
        if ("kakao".equals(provider)) {
            Map<String, Object> account = castMap(attributes.get("kakao_account"));
            Map<String, Object> profile = account != null ? castMap(account.get("profile")) : null;
            String email = account != null ? (String) account.get("email") : null;
            String nickname = profile != null ? (String) profile.get("nickname") : null;
            String profileImageUrl = profile != null ? (String) profile.get("profile_image_url") : null;
            return validated(new SocialProfile(String.valueOf(attributes.get("id")), email, nickname, profileImageUrl));
        }
        if ("google".equals(provider)) {
            return validated(new SocialProfile(
                    (String) attributes.get("sub"),
                    (String) attributes.get("email"),
                    (String) attributes.get("name"),
                    (String) attributes.get("picture")));
        }
        if ("naver".equals(provider)) {
            Map<String, Object> response = castMap(attributes.get("response"));
            String id = response != null ? (String) response.get("id") : null;
            String email = response != null ? (String) response.get("email") : null;
            String name = response != null ? (String) response.get("name") : null;
            String profileImageUrl = response != null ? (String) response.get("profile_image") : null;
            return validated(new SocialProfile(id, email, name, profileImageUrl));
        }
        throw authError("unsupported_provider", "지원하지 않는 소셜 로그인입니다: " + provider);
    }

    // 이메일이 이미 가입돼 있으면 그 계정에 소셜 로그인을 연동, 없으면 신규가입
    private Member resolveByEmail(String provider, SocialProfile profile, boolean[] isNewMember) {
        String email = profile.email().trim().toLowerCase();
        return memberRepository.findByEmail(email)
                .map(existing -> withProfileImageIfMissing(linkProvider(existing, provider, profile.providerId()), profile))
                .orElseGet(() -> {
                    isNewMember[0] = true;
                    return join(provider, email, profile);
                });
    }

    private Member linkProvider(Member member, String provider, String providerId) {
        member.linkProvider(provider, providerId);
        return member;
    }

    // 프로필 사진을 아직 등록 안 한 회원에게만 소셜 프로필 사진을 채워줌 - 직접 올린 사진은 덮어쓰지 않음
    private Member withProfileImageIfMissing(Member member, SocialProfile profile) {
        if (member.getProfileImg() == null && profile.profileImageUrl() != null) {
            member.changeProfileImg(profile.profileImageUrl());
        }
        return member;
    }

    private Member join(String provider, String email, SocialProfile profile) {
        Member member = Member.builder()
                .email(email)
                .nickname(uniqueNickname(profile.nickname()))
                .provider(provider)
                .providerId(profile.providerId())
                .build();
        member.markEmailVerified();
        member.changeProfileImg(profile.profileImageUrl());
        // findByEmail-save 사이 TOCTOU 대비 - 그새 가입됐으면 연동으로 전환
        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            return memberRepository.findByEmail(email)
                    .map(existing -> withProfileImageIfMissing(linkProvider(existing, provider, profile.providerId()), profile))
                    .orElseThrow(() -> authError("join_failed", "회원가입에 실패했습니다. 다시 시도해주세요."));
        }
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

    // 필터 내부 예외라 GlobalExceptionHandler 대신 OAuth2FailureHandler가 처리
    private OAuth2AuthenticationException authError(String code, String description) {
        return new OAuth2AuthenticationException(new OAuth2Error(code, description, null));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map ? (Map<String, Object>) value : null;
    }

    private record SocialProfile(String providerId, String email, String nickname, String profileImageUrl) {
    }
}
