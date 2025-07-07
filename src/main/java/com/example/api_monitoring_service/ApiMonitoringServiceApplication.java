package com.example.api_monitoring_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableRetry // для использования @Retryable
public class ApiMonitoringServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiMonitoringServiceApplication.class, args);
	}

}
