package com.tails.common.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

// 소셜 로그인 실패 시 에러 코드를 쿼리 파라미터로 붙여 프론트로 리다이렉트
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String successRedirectUrl;

    public OAuth2FailureHandler(@Value("${oauth2.success-redirect-url}") String successRedirectUrl) {
        this.successRedirectUrl = successRedirectUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        String errorCode = exception instanceof OAuth2AuthenticationException oauth2Exception
                ? oauth2Exception.getError().getErrorCode()
                : "oauth2_login_failed";

        String url = successRedirectUrl + "?error=" + URLEncoder.encode(errorCode, StandardCharsets.UTF_8);
        response.sendRedirect(url);
    }
}
