package com.loopers.interfaces.consumer;

import com.loopers.confg.kafka.KafkaTestContainer;
import com.loopers.domain.event.DemoEvent;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class DemoKafkaConsumerTest extends KafkaTestContainer {

    @Autowired
    private DemoKafkaConsumer consumer;

    @Value("${demo-kafka.test.topic-name}")
    private String topicName;

    private Producer<String, DemoEvent> testProducer;

    @BeforeEach
    void setUp() {
        consumer.clearReceivedEvents();

        // 테스트용 Producer 생성
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(
                KAFKA_CONTAINER.getBootstrapServers()
        );
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, DemoEvent> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps);

        testProducer = producerFactory.createProducer();
    }

    @AfterEach
    void tearDown() {
        if (testProducer != null) {
            testProducer.close();
        }
        consumer.clearReceivedEvents();
    }

    @Test
    @DisplayName("Consumer가 Kafka 토픽에서 이벤트를 수신할 수 있다")
    void consumeEvent() {
        // given
        DemoEvent event = DemoEvent.builder()
                .id("test-id-1")
                .message("Test consumer message")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        ProducerRecord<String, DemoEvent> record = new ProducerRecord<>(
                topicName,
                event.getId(),
                event
        );
        testProducer.send(record);
        testProducer.flush();

        // then
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(consumer.getReceivedEvents()).hasSize(1);
                    DemoEvent receivedEvent = consumer.getReceivedEvents().get(0);
                    assertThat(receivedEvent.getId()).isEqualTo("test-id-1");
                    assertThat(receivedEvent.getMessage()).isEqualTo("Test consumer message");
                });
    }

    @Test
    @DisplayName("Consumer가 배치로 여러 이벤트를 수신할 수 있다")
    void consumeMultipleEvents() {
        // given
        int eventCount = 5;
        for (int i = 0; i < eventCount; i++) {
            DemoEvent event = DemoEvent.builder()
                    .id("test-id-" + i)
                    .message("Message " + i)
                    .timestamp(System.currentTimeMillis())
                    .build();

            ProducerRecord<String, DemoEvent> record = new ProducerRecord<>(
                    topicName,
                    event.getId(),
                    event
            );
            testProducer.send(record);
        }
        testProducer.flush();

        // then
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    assertThat(consumer.getReceivedEvents()).hasSizeGreaterThanOrEqualTo(eventCount);
                });

        assertThat(consumer.getReceivedEvents())
                .extracting(DemoEvent::getId)
                .contains("test-id-0", "test-id-1", "test-id-2", "test-id-3", "test-id-4");
    }
}
