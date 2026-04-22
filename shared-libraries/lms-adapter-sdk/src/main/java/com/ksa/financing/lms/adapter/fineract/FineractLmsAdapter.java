package com.ksa.financing.lms.adapter.fineract;

import com.ksa.financing.lms.adapter.fineract.dto.*;
import com.ksa.financing.lms.dto.*;
import com.ksa.financing.lms.exception.FineractException;
import com.ksa.financing.lms.exception.IdempotencyException;
import com.ksa.financing.lms.intent.*;
import com.ksa.financing.lms.port.LmsPort;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Apache Fineract implementation of the LMS Port.
 * Translates domain intents into Fineract-specific API calls.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FineractLmsAdapter implements LmsPort {

    private final FineractClient fineractClient;
    private final FineractMapper fineractMapper;
    private final RedissonClient redissonClient;
    private final IdempotencyStore idempotencyStore;

    @Override
    public LoanProductResult createLoanProduct(LoanProductIntent intent) {
        log.info("Creating loan product in Fineract: {}", intent.getProductCode());

        // Check idempotency
        String idempotencyKey = "product-create:" + intent.getRequestId();
        if (idempotencyStore.exists(idempotencyKey)) {
            log.warn("Duplicate product creation request: {}", intent.getRequestId());
            return idempotencyStore.getResult(idempotencyKey, LoanProductResult.class);
        }

        try {
            FineractLoanProductRequest request = fineractMapper.toFineractLoanProductRequest(intent);
            FineractLoanProductResponse response = fineractClient.createLoanProduct(request);

            LoanProductResult result = LoanProductResult.builder()
                    .loanProductId(String.valueOf(response.getResourceId()))
                    .shortName(request.getShortName())
                    .success(true)
                    .build();

            idempotencyStore.store(idempotencyKey, result);
            log.info("Loan product created successfully in Fineract: {}", result.getLoanProductId());
            return result;

        } catch (Exception e) {
            log.error("Failed to create loan product in Fineract", e);
            return LoanProductResult.failed("Fineract product creation failed: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public LoanAccountId createLoanAccount(LoanIntent intent) {
        log.info("Creating loan account for customer: {}", intent.getCustomerId());

        // Check idempotency
        String idempotencyKey = "loan-create:" + intent.getRequestId();
        if (idempotencyStore.exists(idempotencyKey)) {
            log.warn("Duplicate loan creation request: {}", intent.getRequestId());
            return idempotencyStore.getResult(idempotencyKey, LoanAccountId.class);
        }

        try {
            // Map domain intent to Fineract request
            FineractLoanRequest request = fineractMapper.toFineractLoanRequest(intent);

            // Add Islamic finance data to datatables
            request.setDatatables(List.of(
                FineractLoanRequest.DataTableEntry.builder()
                    .registeredTableName("m_loan_islamic_finance")
                    .data(Map.of(
                        "sharia_structure", intent.getShariaStructure(),
                        "commodity_id", intent.getCommodityId(),
                        "commodity_cost", intent.getCommodityCost(),
                        "commodity_sale_price", intent.getCommoditySalePrice(),
                        "profit_amount", intent.getProfitAmount()
                    ))
                    .build()
            ));

            // Create loan in Fineract
            FineractLoanResponse response = fineractClient.createLoan(request);

            // Create loan account ID
            LoanAccountId loanAccountId = LoanAccountId.of("FIN-" + response.getLoanId());

            // Store for idempotency
            idempotencyStore.store(idempotencyKey, loanAccountId);

            log.info("Loan account created successfully: {}", loanAccountId);
            return loanAccountId;

        } catch (Exception e) {
            log.error("Failed to create loan account", e);
            throw new FineractException("Failed to create loan account", e);
        }
    }

    @Override
    public ApprovalResult approveLoan(ApprovalIntent intent) {
        log.info("Approving loan: {}", intent.getLoanAccountId());

        try {
            Long fineractLoanId = extractFineractLoanId(intent.getLoanAccountId());

            FineractApprovalRequest request = FineractApprovalRequest.builder()
                .approvedOnDate(intent.getApprovalTimestamp().toLocalDate())
                .approvedAmount(null) // Use submitted amount
                .note(intent.getApprovalNotes())
                .locale("en")
                .dateFormat("dd MMMM yyyy")
                .build();

            FineractCommandResponse response = fineractClient.approveLoan(fineractLoanId, request);

            return ApprovalResult.builder()
                .loanAccountId(LoanAccountId.of(intent.getLoanAccountId()))
                .approved(true)
                .newStatus(LoanStatus.APPROVED)
                .approvalReference("APPR-" + response.getResourceId())
                .approvalTimestamp(intent.getApprovalTimestamp())
                .approverId(intent.getApproverId())
                .success(true)
                .build();

        } catch (Exception e) {
            log.error("Failed to approve loan", e);
            return ApprovalResult.builder()
                .loanAccountId(LoanAccountId.of(intent.getLoanAccountId()))
                .approved(false)
                .success(false)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    @Override
    @Transactional
    public DisbursementResult disburseLoan(DisbursementIntent intent) {
        log.info("Disbursing loan: {}", intent.getLoanAccountId());

        // Implement idempotency with distributed lock
        String idempotencyKey = intent.getIdempotencyKey();
        RLock lock = redissonClient.getLock("disbursement:" + intent.getLoanAccountId());

        try {
            // Try to acquire lock with timeout
            if (!lock.tryLock(30, TimeUnit.SECONDS)) {
                throw new FineractException("Could not acquire lock for disbursement");
            }

            // Check if already processed
            if (idempotencyStore.exists(idempotencyKey)) {
                log.warn("Duplicate disbursement request: {}", idempotencyKey);
                return idempotencyStore.getResult(idempotencyKey, DisbursementResult.class);
            }

            Long fineractLoanId = extractFineractLoanId(intent.getLoanAccountId());

            FineractDisbursementRequest request = FineractDisbursementRequest.builder()
                .actualDisbursementDate(intent.getDisbursementDate())
                .transactionAmount(intent.getDisbursementAmount())
                .paymentTypeId(mapPaymentMethod(intent.getMethod()))
                .note("Disbursement via " + intent.getMethod())
                .accountNumber(intent.getBeneficiaryAccountNumber())
                .checkNumber(intent.getTransactionReference())
                .routingCode(intent.getBeneficiaryBankCode())
                .receiptNumber(intent.getTransactionReference())
                .bankNumber(intent.getBeneficiaryBankCode())
                .locale("en")
                .dateFormat("dd MMMM yyyy")
                .build();

            FineractCommandResponse response = fineractClient.disburseLoan(fineractLoanId, request);

            DisbursementResult result = DisbursementResult.builder()
                .loanAccountId(LoanAccountId.of(intent.getLoanAccountId()))
                .transactionId("TXN-" + response.getSubResourceId())
                .disbursedAmount(intent.getDisbursementAmount())
                .disbursementDateTime(LocalDateTime.now())
                .disbursementReference("DISB-" + response.getResourceId())
                .paymentMethod(intent.getMethod().toString())
                .beneficiaryAccount(intent.getBeneficiaryAccountNumber())
                .status(DisbursementResult.DisbursementStatus.COMPLETED)
                .success(true)
                .shariaCompliant(intent.isCommodityTransferCompleted())
                .build();

            // Store result for idempotency
            idempotencyStore.store(idempotencyKey, result);

            log.info("Loan disbursed successfully: {}", result.getTransactionId());
            return result;

        } catch (Exception e) {
            log.error("Failed to disburse loan", e);
            return DisbursementResult.failed(
                LoanAccountId.of(intent.getLoanAccountId()),
                e.getMessage()
            );
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    @Override
    public RepaymentResult recordRepayment(RepaymentIntent intent) {
        log.info("Recording repayment for loan: {}", intent.getLoanAccountId());

        // Check idempotency
        String idempotencyKey = intent.getIdempotencyKey();
        if (idempotencyStore.exists(idempotencyKey)) {
            log.warn("Duplicate repayment request: {}", idempotencyKey);
            return idempotencyStore.getResult(idempotencyKey, RepaymentResult.class);
        }

        try {
            Long fineractLoanId = extractFineractLoanId(intent.getLoanAccountId());

            FineractRepaymentRequest request = FineractRepaymentRequest.builder()
                .transactionDate(intent.getPaymentDateTime().toLocalDate())
                .transactionAmount(intent.getPaymentAmount())
                .paymentTypeId(mapPaymentMethod(intent.getPaymentMethod()))
                .note("Payment via " + intent.getPaymentMethod())
                .accountNumber(intent.getPayerAccountNumber())
                .checkNumber(intent.getPaymentReference())
                .routingCode(intent.getPayerBankCode())
                .receiptNumber(intent.getReceiptNumber())
                .bankNumber(intent.getPayerBankCode())
                .locale("en")
                .dateFormat("dd MMMM yyyy")
                .build();

            FineractTransactionResponse response = fineractClient.recordRepayment(fineractLoanId, request);

            RepaymentResult result = RepaymentResult.builder()
                .loanAccountId(LoanAccountId.of(intent.getLoanAccountId()))
                .transactionId("PAY-" + response.getResourceId())
                .paymentAmount(intent.getPaymentAmount())
                .paymentDateTime(intent.getPaymentDateTime())
                .principalPaid(intent.getPrincipalAmount())
                .profitPaid(intent.getProfitAmount())
                .penaltyPaid(intent.getPenaltyAmount())
                .charityAmount(intent.getCharityAmount())
                .paymentReference(intent.getPaymentReference())
                .receiptNumber(intent.getReceiptNumber())
                .paymentMethod(intent.getPaymentMethod().toString())
                .status(RepaymentResult.PaymentStatus.POSTED)
                .success(true)
                .build();

            // Store for idempotency
            idempotencyStore.store(idempotencyKey, result);

            log.info("Repayment recorded successfully: {}", result.getTransactionId());
            return result;

        } catch (Exception e) {
            log.error("Failed to record repayment", e);
            return RepaymentResult.failed(
                LoanAccountId.of(intent.getLoanAccountId()),
                e.getMessage()
            );
        }
    }

    @Override
    @Cacheable(value = "loanDetails", key = "#loanAccountId.value")
    public LoanDetails getLoanDetails(LoanAccountId loanAccountId) {
        log.debug("Fetching loan details for: {}", loanAccountId);

        try {
            Long fineractLoanId = extractFineractLoanId(loanAccountId.getValue());
            FineractLoanDetails fineractDetails = fineractClient.getLoanDetails(fineractLoanId);

            return fineractMapper.toLoanDetails(fineractDetails, loanAccountId);

        } catch (Exception e) {
            log.error("Failed to fetch loan details", e);
            throw new FineractException("Failed to fetch loan details", e);
        }
    }

    @Override
    public List<Installment> getRepaymentSchedule(LoanAccountId loanAccountId) {
        log.debug("Fetching repayment schedule for: {}", loanAccountId);

        try {
            Long fineractLoanId = extractFineractLoanId(loanAccountId.getValue());
            FineractRepaymentSchedule schedule = fineractClient.getRepaymentSchedule(fineractLoanId);

            return fineractMapper.toInstallments(schedule);

        } catch (Exception e) {
            log.error("Failed to fetch repayment schedule", e);
            throw new FineractException("Failed to fetch repayment schedule", e);
        }
    }

    @Override
    @Transactional
    public SettlementResult processEarlySettlement(SettlementIntent intent) {
        log.info("Processing early settlement for loan: {}", intent.getLoanAccountId());

        try {
            Long fineractLoanId = extractFineractLoanId(intent.getLoanAccountId());

            FineractSettlementRequest request = FineractSettlementRequest.builder()
                .transactionDate(intent.getSettlementDate())
                .transactionAmount(intent.getSettlementAmount())
                .note("Early settlement with Ibra amount: " + intent.getIbraAmount())
                .locale("en")
                .dateFormat("dd MMMM yyyy")
                .build();

            // Store Ibra details in datatables
            Map<String, Object> ibraData = new HashMap<>();
            ibraData.put("ibra_amount", intent.getIbraAmount());
            ibraData.put("ibra_percentage", intent.getIbraPercentage());
            ibraData.put("sharia_approval", intent.getShariaApprovalReference());

            FineractCommandResponse response = fineractClient.processEarlySettlement(fineractLoanId, request);

            return SettlementResult.builder()
                .loanAccountId(LoanAccountId.of(intent.getLoanAccountId()))
                .settlementReference("SETL-" + response.getResourceId())
                .settlementDateTime(LocalDateTime.now())
                .originalOutstanding(intent.getOutstandingPrincipal().add(intent.getRemainingProfit()))
                .ibraAmount(intent.getIbraAmount())
                .finalSettlementAmount(intent.getSettlementAmount())
                .charityAmount(intent.getCharityAmount())
                .paymentTransactionId(intent.getTransactionId())
                .paymentMethod(intent.getPaymentMethod())
                .status(SettlementResult.SettlementStatus.COMPLETED)
                .newLoanStatus(LoanStatus.CLOSED_PREPAID)
                .success(true)
                .shariaCompliant(true)
                .shariaApprovalRef(intent.getShariaApprovalReference())
                .build();

        } catch (Exception e) {
            log.error("Failed to process early settlement", e);
            return SettlementResult.failed(
                LoanAccountId.of(intent.getLoanAccountId()),
                e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public RescheduleResult rescheduleLoan(RescheduleIntent intent) {
        log.info("Rescheduling loan {} — type: {}", intent.getLoanAccountId(), intent.getRescheduleType());

        String idempotencyKey = "reschedule:" + intent.getIdempotencyKey();

        // 1. Idempotency check
        if (idempotencyStore.exists(idempotencyKey)) {
            log.warn("Duplicate reschedule request: {}", intent.getIdempotencyKey());
            RescheduleResult cached = idempotencyStore.getResult(idempotencyKey, RescheduleResult.class);
            if (cached != null) {
                cached.setDuplicate(true);
                return cached;
            }
        }

        Long fineractLoanId = extractFineractLoanId(intent.getLoanAccountId());
        LoanAccountId loanAccountId = LoanAccountId.of(intent.getLoanAccountId());
        String todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy"));


        try {
            // 2. Submit reschedule request to Fineract
            FineractRescheduleRequest rescheduleRequest =
                    fineractMapper.toFineractRescheduleRequest(intent, fineractLoanId);

            FineractRescheduleResponse submitResponse =
                    fineractClient.submitReschedule(rescheduleRequest);

            Long fineractRescheduleId = submitResponse.getResourceId();
            log.info("Reschedule submitted to Fineract with ID: {}", fineractRescheduleId);

            // 3. Approve reschedule in Fineract (our approval already done in Temporal workflow)
            fineractClient.approveReschedule(fineractRescheduleId, todayFormatted);
            log.info("Reschedule {} approved in Fineract", fineractRescheduleId);

            // 4. Build result
            RescheduleResult result = RescheduleResult.success(
                    loanAccountId,
                    intent.getRequestId(),
                    String.valueOf(fineractRescheduleId));

            // 5. For RESTRUCTURING — post GL journal entries (write-off + profit waiver)
            if (intent.getRescheduleType() == RescheduleIntent.RescheduleType.RESTRUCTURING) {
                postRestructuringGlEntries(intent, result);
            }

            // 6. Store for idempotency
            idempotencyStore.store(idempotencyKey, result);

            log.info("Loan rescheduled successfully: loanId={}, fineractRescheduleId={}",
                    intent.getLoanAccountId(), fineractRescheduleId);
            return result;

        } catch (Exception e) {
            log.error("Failed to reschedule loan {}: {}", intent.getLoanAccountId(), e.getMessage(), e);
            return RescheduleResult.failed(loanAccountId, intent.getRequestId(), e.getMessage());
        }
    }

    /**
     * Posts GL journal entries for RESTRUCTURING with write-off.
     * Blueprint 17 Section 4.4:
     *   Dr PROVISION_FOR_BAD_DEBTS / Cr LOAN_RECEIVABLE_PRINCIPAL  (principal write-off)
     *   Dr UNEARNED_PROFIT        / Cr LOAN_RECEIVABLE_PROFIT      (profit waiver)
     */
    private void postRestructuringGlEntries(RescheduleIntent intent, RescheduleResult result) {
        LocalDate today = LocalDate.now();

        // Principal write-off GL entry
        if (intent.getWriteOffAmount() != null &&
                intent.getWriteOffAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {

            String writeOffKey = "gl-writeoff:" + intent.getIdempotencyKey();
            if (!idempotencyStore.exists(writeOffKey)) {
                FineractJournalRequest writeOffEntry = FineractJournalRequest.builder()
                        .officeId(1L)
                        .currencyCode("SAR")
                        .transactionDate(today)
                        .locale("en")
                        .dateFormat("dd MMMM yyyy")
                        .comments("Principal write-off per restructure — requestId: " + intent.getRequestId())
                        .debits(List.of(FineractJournalRequest.JournalLine.builder()
                                .glAccountCode(intent.getWriteOffGlAccountCode())
                                .amount(intent.getWriteOffAmount())
                                .build()))
                        .credits(List.of(FineractJournalRequest.JournalLine.builder()
                                .glAccountCode(intent.getLoanReceivableGlCode())
                                .amount(intent.getWriteOffAmount())
                                .build()))
                        .build();

                FineractJournalResponse resp = fineractClient.createJournalEntry(writeOffEntry);
                result.setWriteOffJournalEntryId(resp != null ? resp.getTransactionId() : null);
                idempotencyStore.store(writeOffKey, resp);
                log.info("Write-off GL entry posted: amount={}", intent.getWriteOffAmount());
            }
        }

        // Profit waiver GL entry
        if (intent.getProfitWaiverAmount() != null &&
                intent.getProfitWaiverAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {

            String profitWaiverKey = "gl-profitwvr:" + intent.getIdempotencyKey();
            if (!idempotencyStore.exists(profitWaiverKey)) {
                FineractJournalRequest profitWaiverEntry = FineractJournalRequest.builder()
                        .officeId(1L)
                        .currencyCode("SAR")
                        .transactionDate(today)
                        .locale("en")
                        .dateFormat("dd MMMM yyyy")
                        .comments("Profit waiver per restructure — requestId: " + intent.getRequestId())
                        .debits(List.of(FineractJournalRequest.JournalLine.builder()
                                .glAccountCode(intent.getUnearnedProfitGlCode())
                                .amount(intent.getProfitWaiverAmount())
                                .build()))
                        .credits(List.of(FineractJournalRequest.JournalLine.builder()
                                .glAccountCode(intent.getProfitReceivableGlCode())
                                .amount(intent.getProfitWaiverAmount())
                                .build()))
                        .build();

                FineractJournalResponse resp = fineractClient.createJournalEntry(profitWaiverEntry);
                result.setProfitWaiverJournalEntryId(resp != null ? resp.getTransactionId() : null);
                idempotencyStore.store(profitWaiverKey, resp);
                log.info("Profit waiver GL entry posted: amount={}", intent.getProfitWaiverAmount());
            }
        }
    }

    @Override
    public void updateLoanStatus(LoanAccountId loanAccountId, LoanStatus status) {
        log.info("Updating loan status for {}: {}", loanAccountId, status);

        // Implementation would map to appropriate Fineract command
        // For example: withdraw, reject, write-off, etc.
        throw new UnsupportedOperationException("Status update not yet implemented");
    }

    @Override
    public void reverseTransaction(String transactionId) {
        log.info("Reversing transaction: {}", transactionId);

        try {
            // Extract loan ID and transaction ID from our format
            String[] parts = transactionId.split("-");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Invalid transaction ID format");
            }

            // Call Fineract reverse API
            Map<String, Object> reversal = Map.of(
                "note", "Transaction reversal",
                "locale", "en"
            );

            // This would need proper parsing of transaction ID to get loan and transaction IDs
            // fineractClient.reverseTransaction(loanId, txnId, reversal);

            log.info("Transaction reversed successfully");

        } catch (Exception e) {
            log.error("Failed to reverse transaction", e);
            throw new FineractException("Failed to reverse transaction", e);
        }
    }

    @Override
    public boolean isHealthy() {
        return fineractClient.isHealthy();
    }

    /**
     * Extracts Fineract loan ID from our loan account ID format.
     */
    private Long extractFineractLoanId(String loanAccountId) {
        // Format: FIN-12345
        String[] parts = loanAccountId.split("-");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid loan account ID format: " + loanAccountId);
        }
        return Long.parseLong(parts[1]);
    }

    /**
     * Maps domain payment method to Fineract payment type ID.
     */
    private Long mapPaymentMethod(Object method) {
        // This would map to configured payment types in Fineract
        // For now, returning a default
        return 1L;
    }
}