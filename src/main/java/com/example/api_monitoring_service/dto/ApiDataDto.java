package com.example.api_monitoring_service.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ApiDataDto {
    private UUID id;
    private LocalDateTime createdAt;
    private boolean success;
    private String payload;
    private String version;
}