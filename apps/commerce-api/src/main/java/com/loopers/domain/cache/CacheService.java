package com.loopers.domain.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component
public class CacheService {

    private final RedisTemplate<String, String> defaultRedisTemplate;

    /**
     * 캐시에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     */
    public void set(String key, String value) {
        log.debug("Setting cache - key: {}, value: {}", key, value);
        defaultRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * TTL과 함께 캐시에 값을 저장합니다.
     *
     * @param key 캐시 키
     * @param value 저장할 값
     * @param ttl TTL (Time To Live)
     */
    public void setWithTtl(String key, String value, Duration ttl) {
        log.debug("Setting cache with TTL - key: {}, value: {}, ttl: {}s", key, value, ttl.getSeconds());
        defaultRedisTemplate.opsForValue().set(key, value, ttl);
    }

    /**
     * 캐시에서 값을 조회합니다.
     *
     * @param key 캐시 키
     * @return 캐시 값 (Optional)
     */
    public Optional<String> get(String key) {
        String value = defaultRedisTemplate.opsForValue().get(key);
        log.debug("Getting cache - key: {}, value: {}", key, value);
        return Optional.ofNullable(value);
    }

    /**
     * 캐시를 삭제합니다.
     *
     * @param key 캐시 키
     * @return 삭제 성공 여부
     */
    public boolean delete(String key) {
        Boolean deleted = defaultRedisTemplate.delete(key);
        log.debug("Deleting cache - key: {}, deleted: {}", key, deleted);
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * 캐시가 존재하는지 확인합니다.
     *
     * @param key 캐시 키
     * @return 존재 여부
     */
    public boolean exists(String key) {
        Boolean exists = defaultRedisTemplate.hasKey(key);
        log.debug("Checking cache existence - key: {}, exists: {}", key, exists);
        return Boolean.TRUE.equals(exists);
    }

    /**
     * 캐시의 만료 시간을 설정합니다.
     *
     * @param key 캐시 키
     * @param ttl TTL (Time To Live)
     * @return 성공 여부
     */
    public boolean expire(String key, Duration ttl) {
        Boolean result = defaultRedisTemplate.expire(key, ttl);
        log.debug("Setting expiration - key: {}, ttl: {}s, result: {}", key, ttl.getSeconds(), result);
        return Boolean.TRUE.equals(result);
    }
}
