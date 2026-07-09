package com.tails.common.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

// 요청마다 Authorization 헤더의 JWT를 검사해 유효하면 인증 상태로 만들어주는 필터
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService customUserDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = resolveToken(request);

        if (token != null) {
            Optional<Claims> claims = jwtProvider.parseClaims(token);
            if (claims.isPresent()) {
                authenticate(jwtProvider.getMemberId(claims.get()));
            }
        }

        // 토큰이 없거나 유효하지 않아도 여기서 막지 않고 통과. 인증이 실제로 필요한지는
        // SecurityConfig의 authorizeHttpRequests가 판단.
        filterChain.doFilter(request, response);
    }

    private void authenticate(Long memberId) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserByUsername(String.valueOf(memberId));
            var authentication = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (UsernameNotFoundException e) {
            // 탈퇴 등으로 더 이상 존재하지 않는 회원 -> 인증 처리 없이 통과
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)) {
            return header.substring(PREFIX.length());
        }
        return null;
    }
}
