package com.example.api_monitoring_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message)
                .thenAccept(result -> {
                    log.info("Message sent to topic {} successfully.", topic);
                })
                .exceptionally(ex -> {
                    log.error("Failed to send message to topic {}.", topic, ex);
                    return null;
                });
    }
}