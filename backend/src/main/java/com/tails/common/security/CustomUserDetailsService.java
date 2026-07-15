package com.tails.common.security;

import com.tails.member.Member;
import com.tails.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// JwtFilter가 넘긴 회원 ID로 DB를 재조회해, 탈퇴한 회원의 토큰을 즉시 무효화시키는 역할
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    // username에는 로그인 아이디가 아니라 JWT subject(회원 ID)가 들어온다
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findById(Long.valueOf(username))
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않거나 탈퇴한 회원입니다: " + username));
        // role을 토큰이 아니라 여기서 DB로 읽는 이유: 권한 회수(ADMIN -> USER)가 다음 요청부터 즉시 반영되도록
        return new CustomUserDetails(member.getId(), member.getEmail(), member.getRole());
    }
}
