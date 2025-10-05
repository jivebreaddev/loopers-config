package com.loopers.integration;

import com.loopers.confg.kafka.KafkaTestContainer;
import com.loopers.domain.event.DemoEvent;
import com.loopers.interfaces.consumer.DemoKafkaConsumer;
import com.loopers.interfaces.producer.DemoKafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
class KafkaProducerConsumerIntegrationTest extends KafkaTestContainer {

    @Autowired
    private DemoKafkaProducer producer;

    @Autowired
    private DemoKafkaConsumer consumer;

    @BeforeEach
    void setUp() throws InterruptedException {
        consumer.clearReceivedEvents();
        // Consumer가 준비될 시간 부여
        Thread.sleep(2000);
    }

    @Test
    @DisplayName("Producer에서 전송한 이벤트를 Consumer가 수신할 수 있다")
    void producerConsumerIntegration() {
        // given
        DemoEvent event = DemoEvent.builder()
                .id("integration-test-1")
                .message("Integration test message")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        producer.sendEvent(event);

        // then
        await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    List<DemoEvent> receivedEvents = consumer.getReceivedEvents();
                    assertThat(receivedEvents).hasSize(1);

                    DemoEvent receivedEvent = receivedEvents.get(0);
                    assertThat(receivedEvent.getId()).isEqualTo("integration-test-1");
                    assertThat(receivedEvent.getMessage()).isEqualTo("Integration test message");
                    assertThat(receivedEvent.getTimestamp()).isEqualTo(event.getTimestamp());
                });
    }

    @Test
    @DisplayName("Producer에서 전송한 여러 이벤트를 Consumer가 배치로 수신할 수 있다")
    void producerConsumerBatchIntegration() {
        // given
        int eventCount = 10;
        List<DemoEvent> events = IntStream.range(0, eventCount)
                .mapToObj(i -> DemoEvent.builder()
                        .id("batch-test-" + i)
                        .message("Batch message " + i)
                        .timestamp(System.currentTimeMillis())
                        .build())
                .toList();

        // when
        events.forEach(producer::sendEvent);

        // then
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<DemoEvent> receivedEvents = consumer.getReceivedEvents();
                    assertThat(receivedEvents).hasSizeGreaterThanOrEqualTo(eventCount);

                    List<String> receivedIds = receivedEvents.stream()
                            .map(DemoEvent::getId)
                            .toList();

                    IntStream.range(0, eventCount)
                            .forEach(i -> assertThat(receivedIds).contains("batch-test-" + i));
                });
    }

    @Test
    @DisplayName("순서대로 전송된 이벤트를 Consumer가 수신하여 처리할 수 있다")
    void eventOrderingTest() {
        // given
        String commonKey = "same-partition-key";
        List<DemoEvent> events = IntStream.range(0, 5)
                .mapToObj(i -> DemoEvent.builder()
                        .id(commonKey)  // 같은 키로 전송하여 파티션 보장
                        .message("Ordered message " + i)
                        .timestamp(System.currentTimeMillis() + i)
                        .build())
                .toList();

        // when
        events.forEach(producer::sendEvent);

        // then
        await()
                .atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> {
                    List<DemoEvent> receivedEvents = consumer.getReceivedEvents();
                    assertThat(receivedEvents).hasSizeGreaterThanOrEqualTo(5);

                    // timestamp 순서 확인
                    List<Long> timestamps = receivedEvents.stream()
                            .limit(5)
                            .map(DemoEvent::getTimestamp)
                            .toList();

                    for (int i = 0; i < timestamps.size() - 1; i++) {
                        assertThat(timestamps.get(i)).isLessThanOrEqualTo(timestamps.get(i + 1));
                    }
                });
    }
}
