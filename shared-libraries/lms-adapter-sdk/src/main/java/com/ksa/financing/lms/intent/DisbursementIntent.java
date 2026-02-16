package com.ksa.financing.lms.intent;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Disbursement Intent - Domain request for loan disbursement.
 */
@Data
@Builder
public class DisbursementIntent {

    private String loanAccountId;
    private BigDecimal disbursementAmount;
    private LocalDate disbursementDate;

    // Payment Details
    private DisbursementMethod method;
    private String beneficiaryAccountNumber;
    private String beneficiaryBankCode;
    private String beneficiaryName;

    // For partial disbursements
    private boolean isPartialDisbursement;
    private int disbursementSequence; // 1st, 2nd, etc.
    private BigDecimal totalApprovedAmount;

    // Islamic Finance Specific
    private String commodityPurchaseReference; // Tawarruq purchase ref
    private String commoditySaleReference;     // Tawarruq sale ref
    private boolean commodityTransferCompleted;

    // For idempotency
    private String idempotencyKey;
    private String transactionReference;

    // Audit
    private String disbursedBy;
    private String approvalReference;

    public enum DisbursementMethod {
        BANK_TRANSFER,
        SARIE_TRANSFER,  // Saudi fast payment
        CHECK,
        WALLET_CREDIT,
        VENDOR_PAYMENT   // Direct payment to vendor (Murabaha)
    }
}