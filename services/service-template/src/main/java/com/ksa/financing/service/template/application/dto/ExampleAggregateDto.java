package com.ksa.financing.service.template.application.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for ExampleAggregate.
 * Used for data transfer between layers and external communication.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExampleAggregateDto {

    private String id;
    private String tenantId;
    private String name;
    private String description;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
    private String lastModifiedBy;
    private LocalDateTime lastModifiedAt;
    private List<ExampleEntityDto> entities;
    private Double completionScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExampleEntityDto {
        private String id;
        private String name;
        private String value;
    }
}