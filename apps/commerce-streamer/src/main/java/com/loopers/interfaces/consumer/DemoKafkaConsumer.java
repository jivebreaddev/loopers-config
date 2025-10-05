package com.loopers.interfaces.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.confg.kafka.KafkaConfig;
import com.loopers.domain.event.DemoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@RequiredArgsConstructor
@Component
public class DemoKafkaConsumer {

    private final ObjectMapper objectMapper;

    // 테스트를 위한 수신된 이벤트 저장
    private final List<DemoEvent> receivedEvents = new CopyOnWriteArrayList<>();

    @KafkaListener(
        topics = {"${demo-kafka.test.topic-name}"},
        containerFactory = KafkaConfig.BATCH_LISTENER
    )
    public void demoListener(
        List<ConsumerRecord<Object, Object>> messages,
        Acknowledgment acknowledgment
    ) {
        log.info("Received {} messages", messages.size());

        messages.forEach(record -> {
            try {
                Object value = record.value();
                log.info("Processing message: key={}, value={}, type={}",
                    record.key(), value, value != null ? value.getClass().getName() : "null");

                // String, Map, DemoEvent 모두 처리
                DemoEvent event = null;
                if (value instanceof DemoEvent demoEvent) {
                    event = demoEvent;
                } else if (value instanceof String jsonString) {
                    // JSON String을 DemoEvent로 파싱
                    event = objectMapper.readValue(jsonString, DemoEvent.class);
                } else if (value instanceof java.util.Map) {
                    // JSON deserializer가 Map으로 역직렬화한 경우
                    @SuppressWarnings("unchecked")
                    java.util.Map<String, Object> map = (java.util.Map<String, Object>) value;
                    event = DemoEvent.builder()
                        .id((String) map.get("id"))
                        .message((String) map.get("message"))
                        .timestamp(((Number) map.get("timestamp")).longValue())
                        .build();
                }

                if (event != null) {
                    receivedEvents.add(event);
                    log.info("Successfully processed event: {}", event.getId());
                }
            } catch (Exception e) {
                log.error("Error processing message", e);
            }
        });

        acknowledgment.acknowledge();
    }

    // 테스트용 메서드
    public List<DemoEvent> getReceivedEvents() {
        return receivedEvents;
    }

    public void clearReceivedEvents() {
        receivedEvents.clear();
    }
}
