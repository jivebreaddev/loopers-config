package com.loopers.interfaces.producer;

import com.loopers.confg.kafka.KafkaTestContainer;
import com.loopers.domain.event.DemoEvent;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DemoKafkaProducerTest extends KafkaTestContainer {

    @Autowired
    private DemoKafkaProducer producer;

    @Value("${demo-kafka.test.topic-name}")
    private String topicName;

    private Consumer<String, DemoEvent> testConsumer;

    @BeforeEach
    void setUp() {
        // 테스트용 Consumer 생성 - 매번 새로운 group으로 생성
        String uniqueGroupId = "test-group-" + System.currentTimeMillis();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps(
                KAFKA_CONTAINER.getBootstrapServers(),
                uniqueGroupId,
                "true"
        );
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, DemoEvent.class);

        DefaultKafkaConsumerFactory<String, DemoEvent> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps);

        testConsumer = consumerFactory.createConsumer();
        testConsumer.subscribe(List.of(topicName));

        // Consumer가 파티션에 할당될 때까지 대기
        testConsumer.poll(java.time.Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDown() {
        if (testConsumer != null) {
            testConsumer.close();
        }
    }

    @Test
    @DisplayName("Producer가 Kafka 토픽에 이벤트를 전송할 수 있다")
    void sendEvent() {
        // given
        DemoEvent event = DemoEvent.builder()
                .id("test-id-1")
                .message("Test message")
                .timestamp(System.currentTimeMillis())
                .build();

        // when
        producer.sendEvent(event);

        // then
        ConsumerRecords<String, DemoEvent> records = KafkaTestUtils.getRecords(
                testConsumer,
                Duration.ofSeconds(10)
        );

        assertThat(records).isNotEmpty();
        assertThat(records.count()).isGreaterThanOrEqualTo(1);

        DemoEvent receivedEvent = records.iterator().next().value();
        assertThat(receivedEvent).isNotNull();
        assertThat(receivedEvent.getId()).isEqualTo("test-id-1");
        assertThat(receivedEvent.getMessage()).isEqualTo("Test message");
        assertThat(receivedEvent.getTimestamp()).isEqualTo(event.getTimestamp());
    }

}
