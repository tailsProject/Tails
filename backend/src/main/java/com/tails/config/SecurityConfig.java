package com.tails.config;

import com.tails.common.security.CustomOAuth2UserService;
import com.tails.common.security.CustomUserDetailsService;
import com.tails.common.security.JwtAccessDeniedHandler;
import com.tails.common.security.JwtAuthenticationEntryPoint;
import com.tails.common.security.JwtFilter;
import com.tails.common.security.JwtProvider;
import com.tails.common.security.OAuth2FailureHandler;
import com.tails.common.security.OAuth2SuccessHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Spring Security 설정: JWT 기반 인증(세션 미사용), 인증 필터 체인 구성
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    @Value("${cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/members/join", "/api/members/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/members/check-email", "/api/members/check-nickname").permitAll()
                        // Access Token 만료 상태에서도 호출돼야 하는 API라 permitAll (본인 확인은 쿠키의 Refresh Token으로)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**", "/login/oauth2/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards", "/api/boards/{boardId:[0-9]+}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards/{boardId}/images", "/api/reviews/{reviewId}/images").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/reviews/recent", "/api/reviews/rating-summary").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/boards/{boardId:[0-9]+}/comments").permitAll()
                        // 업로드된 이미지 파일 자체(정적 리소스). 없으면 <img> 태그가 401을 맞음
                        .requestMatchers("/uploads/**").permitAll()
                        // 공유 토큰만 알면 로그인 없이 조회 가능. /api/travels/{travelId}(숫자 PK)와 경로 구조가 달라 충돌 없음
                        .requestMatchers(HttpMethod.GET, "/api/travels/shared/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // "/api/places/**" 하위 permitAll 규칙보다 먼저 선언해야 우선 매칭된다
                        .requestMatchers("/api/places/sync/**").hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler))
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler))
                .addFilterBefore(new JwtFilter(jwtProvider, customUserDetailsService), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 쿠키(refreshToken)를 주고받으려면 allowCredentials + 명시적 origin이 필요(와일드카드 불가)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
