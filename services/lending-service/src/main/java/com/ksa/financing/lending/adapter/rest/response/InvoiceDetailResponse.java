package com.ksa.financing.lending.adapter.rest.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Full invoice document rendered from an installment + loan + customer + company config.
 * Returned by GET /api/v1/loans/invoices/{invoiceId}. Includes the existing installment
 * fields for backward compatibility plus the fields needed to render the invoice PDF/UI.
 */
@Schema(description = "Invoice document with all fields needed for PDF rendering + delinquency snapshot")
public record InvoiceDetailResponse(
        // Header
        String invoiceNumber,
        LocalDate invoiceDate,
        LocalDate dueDate,
        @Schema(description = "B2B or B2C") String invoiceType,

        // Issuer (company)
        Company company,

        // Recipient (customer)
        Customer customer,

        // Line items
        List<LineItem> lineItems,

        // Totals
        BigDecimal totalBeforeVat,
        BigDecimal vatPercent,
        BigDecimal vatAmount,
        BigDecimal totalAfterVat,

        // Link to downloadable PDF (or HTML print view)
        String pdfDownloadUrl,

        // Installment fields (kept for back-compat with existing callers)
        String invoiceId,
        int installmentNumber,
        BigDecimal installmentAmount,
        BigDecimal principalComponent,
        BigDecimal profitComponent,
        BigDecimal outstandingBalance,
        String paymentStatus,
        LocalDate paidDate,
        BigDecimal paidAmount,
        boolean receiptAvailable,

        // Per-installment delinquency snapshot mirrored from collections-service
        Map<String, Object> delinquency
) {
    public record Company(String name, String address, String vatNumber, String email, String phone, String logoUrl) {}
    public record Customer(String name, String email, String phone, String nationalId, String customerId) {}
    public record LineItem(String description, int quantity, BigDecimal unitPrice, BigDecimal vatPercent) {}
}
