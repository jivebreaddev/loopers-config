package com.loopers.interfaces.producer;

import com.loopers.domain.event.DemoEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class DemoKafkaProducer {

    @Value("${demo-kafka.test.topic-name}")
    private String topicName;

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    public void sendEvent(DemoEvent event) {
        log.info("Sending event to topic {}: {}", topicName, event);
        kafkaTemplate.send(topicName, event.getId(), event);
    }
}
