package com.example.api_monitoring_service.scheduler;

import com.example.api_monitoring_service.service.ApiService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ApiPollScheduler {

    private final ApiService apiService;

    @Scheduled(cron = "0 * * * * *") //
    public void scheduledTask() throws IOException {
        apiService.fetchApiData();
    }
}
