package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Computed "early-settlement invoice" — the installment's discounted payable amount
 * when it qualifies under the EARLY_SETTLEMENT DelinquencyRule. Not persisted; derived
 * on-the-fly from the current collections-side eligibility snapshot + discount fields.
 */
@Schema(description = "Discounted early-settlement invoice for one eligible installment")
public record EarlySettlementInvoiceResponse(
        @Schema(description = "Original installment invoice id")
        String originalInvoiceId,

        @Schema(description = "Virtual early-settlement invoice id (ESI-<loan-prefix>-NNN). Not stored; valid while eligibility holds.")
        String earlySettlementInvoiceId,

        @Schema(description = "Installment number (1-based)")
        int installmentNumber,

        @Schema(description = "Installment's scheduled due date")
        LocalDate originalDueDate,

        @Schema(description = "Installment outstanding amount before discount")
        BigDecimal originalAmount,

        @Schema(description = "Discount percentage applied (null if flat-amount discount)")
        BigDecimal discountPercentage,

        @Schema(description = "Discount amount applied (null if percentage discount, else the flat figure)")
        BigDecimal discountAmount,

        @Schema(description = "Total absolute discount reduced from the installment")
        BigDecimal totalDiscount,

        @Schema(description = "Customer-payable amount after the discount")
        BigDecimal finalAmount,

        @Schema(description = "Rule's tillDay window (max DPD eligible)")
        Integer validUntilDay,

        @Schema(description = "Latest date this ESI remains eligible (originalDueDate + validUntilDay)")
        LocalDate validUntil,

        @Schema(description = "Current DPD of the installment")
        Integer currentDpd,

        @Schema(description = "Current collections-side status (DUE/SCHEDULED/OVERDUE/PAID)")
        String status
) {}
