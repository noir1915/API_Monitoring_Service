package com.example.api_monitoring_service.service;

import com.example.api_monitoring_service.entity.ApiDataEntity;
import com.example.api_monitoring_service.repository.ApiDataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


    @Scheduled(fixedRateString = "${api.pollingIntervalMs}")
    public void pollApi() {
        try {
            var jsonNode = apiService.fetchApiData();
            String payloadText = jsonNode.toString();

            ApiDataEntity entity = ApiDataEntity.builder()
                    .createdAt(java.time.LocalDateTime.now())
                    .success(true)
                    .payload(payloadText)
                    .build();
            repository.save(entity);

            kafkaProducerService.sendMessage("api-data", payloadText);
            log.info("Данные API успешно получены и отправлены в Kafka");

        } catch (Exception e) {
            log.error("Ошибка при получении данных с API: {}", e.getMessage());

            try {
                String errorMsg = e.getMessage();

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
