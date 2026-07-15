package com.tails.common.security;

import com.tails.common.exception.CustomException;
import com.tails.common.exception.ErrorCode;
import com.tails.member.Member;
import com.tails.member.MemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

// 소셜 로그인 성공 시 JWT 세션(Refresh Token 쿠키)을 만들어 프론트로 리다이렉트
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final MemberRepository memberRepository;
    private final String successRedirectUrl;

    public OAuth2SuccessHandler(AuthService authService,
                                 MemberRepository memberRepository,
                                 @Value("${oauth2.success-redirect-url}") String successRedirectUrl) {
        this.authService = authService;
        this.memberRepository = memberRepository;
        this.successRedirectUrl = successRedirectUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        OAuth2UserPrincipal principal = (OAuth2UserPrincipal) authentication.getPrincipal();
        Member member = memberRepository.findById(principal.getMemberId())
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        AuthService.IssuedTokens tokens = authService.issueTokens(member);
        response.addHeader(HttpHeaders.SET_COOKIE, tokens.refreshCookie().toString());

        getRedirectStrategy().sendRedirect(request, response, successRedirectUrl);
    }
}
