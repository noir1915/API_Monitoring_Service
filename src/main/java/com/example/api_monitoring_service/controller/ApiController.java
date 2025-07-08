package com.example.api_monitoring_service.controller;

import com.example.api_monitoring_service.dto.ApiDataDto;
import com.example.api_monitoring_service.entity.ApiDataEntity;
import com.example.api_monitoring_service.repository.ApiDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApiController {

    private final ApiDataRepository repository;

    @GetMapping("/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("API Monitoring Service in Running");
    }

    @GetMapping("/data")
    public ResponseEntity<List<ApiDataDto>> getLastTenRecords() {
        List<ApiDataEntity> entities = repository.findTop10ByOrderByCreatedAtDesc();

        List<ApiDataDto> dtos = entities.stream()
                .map(entity -> {
                    ApiDataDto dto = new ApiDataDto();
                    dto.setId(entity.getId());
                    dto.setCreatedAt(entity.getCreatedAt());
                    dto.setSuccess(entity.isSuccess());
                    dto.setPayload(entity.getPayload());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(dtos);
    }
}
