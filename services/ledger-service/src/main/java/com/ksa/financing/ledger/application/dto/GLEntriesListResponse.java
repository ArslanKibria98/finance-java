package com.ksa.financing.ledger.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "GL Entries List Response")
public record GLEntriesListResponse(
    @Schema(description = "List of GL entries")
    List<GLEntryResponse> entries,
    @Schema(description = "Total count")
    Integer totalCount
) {}
