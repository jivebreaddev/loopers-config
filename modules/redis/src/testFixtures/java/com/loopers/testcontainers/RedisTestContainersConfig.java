package com.loopers.testcontainers;

import com.redis.testcontainers.RedisContainer;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.utility.DockerImageName;

@Configuration
public class RedisTestContainersConfig {
    private static final RedisContainer redisContainer = new RedisContainer(DockerImageName.parse("redis:7.2-alpine"))
            .withReuse(true);

    static {
        redisContainer.start();

        // Static 블록에서 System Property 설정
        String host = redisContainer.getHost();
        String port = String.valueOf(redisContainer.getFirstMappedPort());

        System.setProperty("datasource.redis.database", "0");
        System.setProperty("datasource.redis.master.host", host);
        System.setProperty("datasource.redis.master.port", port);
        // 배열 형태의 property 설정
        System.setProperty("datasource.redis.replicas[0].host", host);
        System.setProperty("datasource.redis.replicas[0].port", port);

        System.out.println("Redis Testcontainer started at " + host + ":" + port);
    }

    public RedisTestContainersConfig() {
        // 생성자에서도 다시 설정 (보험용)
        String host = redisContainer.getHost();
        String port = String.valueOf(redisContainer.getFirstMappedPort());

        System.setProperty("datasource.redis.database", "0");
        System.setProperty("datasource.redis.master.host", host);
        System.setProperty("datasource.redis.master.port", port);
        System.setProperty("datasource.redis.replicas[0].host", host);
        System.setProperty("datasource.redis.replicas[0].port", port);
    }

    public static RedisContainer getRedisContainer() {
        return redisContainer;
    }
}
