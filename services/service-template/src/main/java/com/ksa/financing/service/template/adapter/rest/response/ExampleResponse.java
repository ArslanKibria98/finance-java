package com.ksa.financing.service.template.adapter.rest.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.ksa.financing.service.template.application.dto.ExampleAggregateDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Response DTO for example aggregate.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Example aggregate response")
public class ExampleResponse {

    @Schema(description = "Aggregate ID", example = "EXA-123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Name of the example", example = "Example Name")
    private String name;

    @Schema(description = "Description of the example")
    private String description;

    @Schema(description = "Current status", example = "ACTIVE")
    private String status;

    @Schema(description = "User who created the aggregate")
    private String createdBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Creation timestamp")
    private LocalDateTime createdAt;

    @Schema(description = "User who last modified the aggregate")
    private String lastModifiedBy;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Schema(description = "Last modification timestamp")
    private LocalDateTime lastModifiedAt;

    @Schema(description = "List of entities within the aggregate")
    private List<EntityResponse> entities;

    @Schema(description = "Completion score (0-100)", example = "75.5")
    private Double completionScore;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Entity within an aggregate")
    public static class EntityResponse {
        @Schema(description = "Entity ID")
        private String id;

        @Schema(description = "Entity name")
        private String name;

        @Schema(description = "Entity value")
        private String value;
    }

    /**
     * Convert from DTO to response.
     */
    public static ExampleResponse from(ExampleAggregateDto dto) {
        return ExampleResponse.builder()
                .id(dto.getId())
                .name(dto.getName())
                .description(dto.getDescription())
                .status(dto.getStatus())
                .createdBy(dto.getCreatedBy())
                .createdAt(dto.getCreatedAt())
                .lastModifiedBy(dto.getLastModifiedBy())
                .lastModifiedAt(dto.getLastModifiedAt())
                .entities(dto.getEntities() != null
                    ? dto.getEntities().stream()
                        .map(e -> EntityResponse.builder()
                            .id(e.getId())
                            .name(e.getName())
                            .value(e.getValue())
                            .build())
                        .collect(Collectors.toList())
                    : List.of())
                .completionScore(dto.getCompletionScore())
                .build();
    }
}