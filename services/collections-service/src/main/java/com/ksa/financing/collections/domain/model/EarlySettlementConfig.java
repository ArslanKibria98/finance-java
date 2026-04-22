package com.ksa.financing.collections.domain.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Early Settlement custom-frequency configuration row.
 * Used only when its parent rule (delinquencyType=1) has {@code isCustom=true}.
 *
 * <p>Layout mirrors the LMS API exactly:
 * <ul>
 *   <li>Singles — one row, {@code isRange=false}, {@code rangeNo=0}, {@code invoiceOrder} set.</li>
 *   <li>Ranges  — one row per span, {@code isRange=true}, {@code rangeNo>0} (1-based),
 *                {@code minInvoiceOrder/maxInvoiceOrder} set, {@code invoiceOrder} null.</li>
 * </ul>
 */
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class EarlySettlementConfig {

    private UUID id;
    private UUID delinquencyId;

    private Integer invoiceOrder;
    private int fromDay;
    private int tillDay;

    private boolean isPercentage;
    @Builder.Default
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    @Builder.Default
    private BigDecimal discountAmount     = BigDecimal.ZERO;

    private boolean isRange;
    private int rangeNo;
    private Integer minInvoiceOrder;
    private Integer maxInvoiceOrder;

    @Builder.Default
    private String channel = "LMS";
    @Builder.Default
    private int recordState = 1;
    @Builder.Default
    private int version = 1;

    private LocalDateTime created;
    private LocalDateTime updatedAt;
}
