package com.example.api_monitoring_service.service;

import com.example.api_monitoring_service.dto.ApiErrorDto;
import com.example.api_monitoring_service.entity.ApiDataEntity;
import com.example.api_monitoring_service.repository.ApiDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiService {

    private final RestTemplate restTemplate;
    private final ApiDataRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Retryable(
            value = {IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 2000, multiplier = 2))
    public JsonNode fetchApiData() throws IOException {
        String apiUrl = "https://api.coindesk.com/v1/bpi/currentprice.json";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String responseBody = response.getBody();

            // Сохраняем успешный ответ
            saveResponse(true, responseBody);

            // Отправляем сообщение в Kafka "api-data"
            kafkaTemplate.send("api-data", responseBody);

            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            // Обработка ошибок и ретраев через @Retryable
            saveResponse(false, e.getMessage());
            log.error("Ошибка при получении данных API: {}", e.getMessage(), e);
            throw e; // чтобы Retry сработал при исключениях
        } catch (Exception e) {
            // Обработка неожиданных ошибок
            saveResponse(false, e.getMessage());
            log.error("Неожиданная ошибка: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void saveResponse(boolean success, String payload) {
        ApiDataEntity entity = new ApiDataEntity();
//        entity.setId(UUID.randomUUID());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSuccess(success);
        entity.setPayload(payload);
        repository.save(entity);

        if (!success) {
            // Создаем DTO для ошибки
            ApiErrorDto errorDto = new ApiErrorDto(payload, LocalDateTime.now());
            try {
                String errorJson = objectMapper.writeValueAsString(errorDto);
                kafkaTemplate.send("api-errors", errorJson);
            } catch (JsonProcessingException e) {
                log.error("Ошибка сериализации ApiErrorDto", e);
            }
        }
    }
}

