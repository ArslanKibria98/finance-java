package com.ksa.financing.ledger.application.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ksa.financing.infra.pagination.PageMetadata;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDate;
import java.util.List;

@Builder
@Schema(description = "Paginated list of repayment schedules across loans in a date range")
public record RepaymentScheduleListResponse(
        @Schema(description = "Window from date") LocalDate fromDate,
        @Schema(description = "Window to date") LocalDate toDate,
        @Schema(description = "Repayment schedules (one per loan, paginated)")
        List<RepaymentScheduleReportResponse> items,
        @Schema(description = "Pagination metadata") @JsonIgnore PageMetadata pagination
) {}
