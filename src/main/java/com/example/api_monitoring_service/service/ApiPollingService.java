package com.example.api_monitoring_service.service;

import com.example.api_monitoring_service.entity.ApiDataEntity;
import com.example.api_monitoring_service.repository.ApiDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class ApiPollingService {

    private final ApiService apiService;
    private final ApiDataRepository repository;
    private final KafkaProducerService kafkaProducerService;

    @Value("${api.url}")
    private String apiUrl;

    @Scheduled(fixedRateString = "${api.pollingIntervalMs}")
    public void pollApi() {
        try {
            var jsonNode = apiService.fetchApiData();
            String payloadText = jsonNode.toString();

            // Сохраняем успешный ответ в базу данных.
            ApiDataEntity entity = ApiDataEntity.builder()
                    .createdAt(java.time.LocalDateTime.now())
                    .success(true)
                    .payload(payloadText)
                    .build();
            repository.save(entity);

            // Отправляем сообщение в Kafka о успешных данных.
            kafkaProducerService.sendMessage("api-data", payloadText);
            log.info("API data fetched and sent to Kafka successfully.");

        } catch (Exception e) {
            log.error("Error fetching API data: {}", e.getMessage());

            try {
                String errorMsg = e.getMessage();

                // Сохраняем ошибку в базу.
                ApiDataEntity entity = ApiDataEntity.builder()
                        .createdAt(LocalDateTime.now())
                        .success(false)
                        .payload(errorMsg)
                        .build();

                repository.save(entity);


                kafkaProducerService.sendMessage("api-errors", errorMsg);
            } catch (Exception ex) {
                log.error("Failed to log error or send to Kafka: {}", ex.getMessage());
            }
        }
    }
}
