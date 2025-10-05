package com.loopers.integration;

import com.loopers.domain.cache.CacheService;
import com.loopers.testcontainers.RedisTestContainersConfig;
import com.loopers.utils.RedisCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Import(RedisTestContainersConfig.class)
class RedisIntegrationTest {

    @Autowired
    private CacheService cacheService;

    @Autowired
    private RedisCleanUp redisCleanUp;

    @Autowired
    private RedisTemplate<String, String> defaultRedisTemplate;

    @AfterEach
    void tearDown() {
        // 테스트 후 Redis 데이터 정리
        redisCleanUp.truncateAll();
    }

    @Test
    @DisplayName("Redis에 값을 저장하고 조회할 수 있다")
    void setAndGet() {
        // given
        String key = "test:key:1";
        String value = "test-value";

        // when
        cacheService.set(key, value);
        Optional<String> result = cacheService.get(key);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(value);
    }

    @Test
    @DisplayName("Redis에 TTL과 함께 값을 저장하면 만료 후 조회되지 않는다")
    void setWithTtl() throws InterruptedException {
        // given
        String key = "test:key:ttl";
        String value = "expiring-value";
        Duration ttl = Duration.ofSeconds(2);

        // when
        cacheService.setWithTtl(key, value, ttl);

        // then - TTL 내에는 조회 가능
        Optional<String> result = cacheService.get(key);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(value);

        // TTL 만료 대기
        Thread.sleep(2100);

        // TTL 만료 후에는 조회 불가
        Optional<String> expiredResult = cacheService.get(key);
        assertThat(expiredResult).isEmpty();
    }

    @Test
    @DisplayName("Redis에서 값을 삭제할 수 있다")
    void delete() {
        // given
        String key = "test:key:delete";
        String value = "to-be-deleted";
        cacheService.set(key, value);

        // when
        boolean deleted = cacheService.delete(key);

        // then
        assertThat(deleted).isTrue();
        Optional<String> result = cacheService.get(key);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("존재하지 않는 키를 삭제하면 false를 반환한다")
    void deleteNonExistentKey() {
        // given
        String key = "test:key:nonexistent";

        // when
        boolean deleted = cacheService.delete(key);

        // then
        assertThat(deleted).isFalse();
    }

    @Test
    @DisplayName("Redis에 키가 존재하는지 확인할 수 있다")
    void exists() {
        // given
        String existingKey = "test:key:exists";
        String nonExistingKey = "test:key:notexists";
        cacheService.set(existingKey, "value");

        // when & then
        assertThat(cacheService.exists(existingKey)).isTrue();
        assertThat(cacheService.exists(nonExistingKey)).isFalse();
    }

    @Test
    @DisplayName("Redis에 저장된 값의 만료 시간을 설정할 수 있다")
    void expire() throws InterruptedException {
        // given
        String key = "test:key:expire";
        String value = "value-with-expiration";
        cacheService.set(key, value);

        // when
        boolean expireSet = cacheService.expire(key, Duration.ofSeconds(1));

        // then
        assertThat(expireSet).isTrue();
        assertThat(cacheService.get(key)).isPresent();

        // 만료 대기
        Thread.sleep(1100);
        assertThat(cacheService.get(key)).isEmpty();
    }

    @Test
    @DisplayName("여러 개의 키-값 쌍을 생성하고 삭제할 수 있다")
    void multipleOperations() {
        // given
        String key1 = "test:multi:1";
        String key2 = "test:multi:2";
        String key3 = "test:multi:3";

        // when - 여러 값 저장
        cacheService.set(key1, "value1");
        cacheService.set(key2, "value2");
        cacheService.set(key3, "value3");

        // then - 모두 존재 확인
        assertThat(cacheService.exists(key1)).isTrue();
        assertThat(cacheService.exists(key2)).isTrue();
        assertThat(cacheService.exists(key3)).isTrue();

        // when - 일부 삭제
        cacheService.delete(key2);

        // then - 삭제된 것만 없음
        assertThat(cacheService.exists(key1)).isTrue();
        assertThat(cacheService.exists(key2)).isFalse();
        assertThat(cacheService.exists(key3)).isTrue();

        // when - 나머지 모두 삭제
        cacheService.delete(key1);
        cacheService.delete(key3);

        // then - 모두 없음
        assertThat(cacheService.exists(key1)).isFalse();
        assertThat(cacheService.exists(key2)).isFalse();
        assertThat(cacheService.exists(key3)).isFalse();
    }

    @Test
    @DisplayName("동일한 키에 값을 덮어쓸 수 있다")
    void overwriteValue() {
        // given
        String key = "test:key:overwrite";
        cacheService.set(key, "original-value");

        // when
        cacheService.set(key, "new-value");

        // then
        Optional<String> result = cacheService.get(key);
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo("new-value");
    }
}
