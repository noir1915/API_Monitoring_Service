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
        String apiUrl = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin&vs_currencies=usd";
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
            String responseBody = response.getBody();

            saveResponse(true, responseBody);

            kafkaTemplate.send("api-data", responseBody);

            return objectMapper.readTree(responseBody);
        } catch (IOException e) {
            saveResponse(false, e.getMessage());
            log.error("Ошибка при получении данных API: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            saveResponse(false, e.getMessage());
            log.error("Неожиданная ошибка: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void saveResponse(boolean success, String payload) {
        ApiDataEntity entity = new ApiDataEntity();
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSuccess(success);
        entity.setPayload(payload);
        repository.save(entity);

        if (!success) {
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