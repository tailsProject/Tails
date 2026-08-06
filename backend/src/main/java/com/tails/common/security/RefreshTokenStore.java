package com.tails.common.security;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

// Refresh Token을 Redis에 저장/조회/삭제. 회원당 값 하나(로그인마다 덮어써서 rotate)
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh-token:member:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long memberId, String token, Duration ttl) {
        redisTemplate.opsForValue().set(key(memberId), token, ttl);
    }

    public Optional<String> find(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(memberId)));
    }

    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}
