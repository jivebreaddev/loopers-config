package com.loopers.confg.kafka;

import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

public abstract class KafkaTestContainer {
    private static final String KAFKA_IMAGE = "confluentinc/cp-kafka:7.6.1";

    protected static final ConfluentKafkaContainer KAFKA_CONTAINER;

    static {
        KAFKA_CONTAINER = new ConfluentKafkaContainer(DockerImageName.parse(KAFKA_IMAGE))
                .withReuse(true);
        KAFKA_CONTAINER.start();

        // System Property로도 주입
        System.setProperty("spring.kafka.bootstrap-servers", KAFKA_CONTAINER.getBootstrapServers());
    }

}
