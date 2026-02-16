package com.ksa.islamic.orchestration.saga;

import com.ksa.islamic.domain.core.model.Money;
import io.temporal.workflow.Workflow;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Currency;

/**
 * Example SAGA implementation for loan disbursement
 *
 * Demonstrates the 3-step SAGA pattern:
 * 1. Reserve funds in wallet
 * 2. Update loan status in LMS
 * 3. Transfer funds to customer
 *
 * If any step fails, previous steps are compensated
 */
@Slf4j
public class LoanDisbursementSaga implements SagaWorkflow<LoanDisbursementSaga.DisbursementRequest, LoanDisbursementSaga.DisbursementResult> {

    @Override
    public DisbursementResult execute(DisbursementRequest input) {
        log.info("Starting loan disbursement SAGA for loan: {}", input.getLoanId());

        // Build the saga steps
        SagaOrchestrator<DisbursementRequest> orchestrator = SagaOrchestrator.<DisbursementRequest>builder()
                .options(SagaOrchestrator.SagaOptions.builder()
                        .chainStepResults(false)
                        .parallelCompensation(false)
                        .build())
                .build();

        // Step 1: Reserve funds in wallet
        orchestrator.addStep(SagaStep.<DisbursementRequest, WalletReservation>builder()
                .name("ReserveFunds")
                .description("Reserve funds in customer wallet")
                .action(this::reserveFundsInWallet)
                .compensation(this::releaseWalletReservation)
                .critical(true)
                .maxRetries(3)
                .build());

        // Step 2: Update loan status in LMS
        orchestrator.addStep(SagaStep.<DisbursementRequest, LoanStatusUpdate>builder()
                .name("UpdateLoanStatus")
                .description("Update loan status to DISBURSED")
                .action(this::updateLoanStatus)
                .compensation(this::revertLoanStatus)
                .critical(true)
                .maxRetries(5)
                .build());

        // Step 3: Transfer funds to customer (point of no return)
        orchestrator.addStep(SagaStep.<DisbursementRequest, FundTransfer>builder()
                .name("TransferFunds")
                .description("Transfer funds to customer account")
                .action(this::transferFundsToCustomer)
                .compensatable(false) // Cannot reverse actual money transfer
                .critical(true)
                .maxRetries(3)
                .build());

        // Execute the saga
        SagaOrchestrator.SagaResult<DisbursementRequest> result = orchestrator.execute(input);

        if (result.isSuccess()) {
            log.info("Loan disbursement completed successfully for loan: {}", input.getLoanId());
            return DisbursementResult.success(input.getLoanId(), input.getAmount());
        } else {
            log.error("Loan disbursement failed at step: {}", result.getFailedStep());
            return DisbursementResult.failure(
                    input.getLoanId(),
                    result.getFailedStep(),
                    result.getError().getMessage()
            );
        }
    }

    // ========== Step Implementations ==========

    private WalletReservation reserveFundsInWallet(DisbursementRequest request) {
        log.info("Reserving {} in wallet for loan {}", request.getAmount(), request.getLoanId());

        // Simulate wallet reservation
        String reservationId = "RES-" + request.getLoanId() + "-" + System.currentTimeMillis();

        // In real implementation, this would call the wallet service
        return WalletReservation.builder()
                .reservationId(reservationId)
                .walletId(request.getWalletId())
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void releaseWalletReservation(WalletReservation reservation) {
        log.info("Releasing wallet reservation: {}", reservation.getReservationId());
        // In real implementation, this would call the wallet service to release the reservation
    }

    private LoanStatusUpdate updateLoanStatus(DisbursementRequest request) {
        log.info("Updating loan status to DISBURSED for loan: {}", request.getLoanId());

        // In real implementation, this would call the LMS service
        return LoanStatusUpdate.builder()
                .loanId(request.getLoanId())
                .previousStatus("APPROVED")
                .newStatus("DISBURSED")
                .timestamp(LocalDateTime.now())
                .build();
    }

    private void revertLoanStatus(LoanStatusUpdate update) {
        log.info("Reverting loan status from {} to {} for loan: {}",
                update.getNewStatus(), update.getPreviousStatus(), update.getLoanId());
        // In real implementation, this would call the LMS service to revert the status
    }

    private FundTransfer transferFundsToCustomer(DisbursementRequest request) {
        log.info("Transferring {} to customer account {} for loan {}",
                request.getAmount(), request.getCustomerAccount(), request.getLoanId());

        // In real implementation, this would call the payment service
        return FundTransfer.builder()
                .transactionId("TXN-" + request.getLoanId() + "-" + System.currentTimeMillis())
                .fromAccount(request.getWalletId())
                .toAccount(request.getCustomerAccount())
                .amount(request.getAmount())
                .timestamp(LocalDateTime.now())
                .status("COMPLETED")
                .build();
    }

    // ========== Data Models ==========

    @Data
    @Builder
    public static class DisbursementRequest {
        private String loanId;
        private String customerId;
        private String walletId;
        private String customerAccount;
        private Money amount;
    }

    @Data
    @Builder
    public static class DisbursementResult {
        private boolean success;
        private String loanId;
        private Money amountDisbursed;
        private String failedStep;
        private String errorMessage;
        private LocalDateTime timestamp;

        public static DisbursementResult success(String loanId, Money amount) {
            return DisbursementResult.builder()
                    .success(true)
                    .loanId(loanId)
                    .amountDisbursed(amount)
                    .timestamp(LocalDateTime.now())
                    .build();
        }

        public static DisbursementResult failure(String loanId, String failedStep, String errorMessage) {
            return DisbursementResult.builder()
                    .success(false)
                    .loanId(loanId)
                    .failedStep(failedStep)
                    .errorMessage(errorMessage)
                    .timestamp(LocalDateTime.now())
                    .build();
        }
    }

    @Data
    @Builder
    private static class WalletReservation {
        private String reservationId;
        private String walletId;
        private Money amount;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    private static class LoanStatusUpdate {
        private String loanId;
        private String previousStatus;
        private String newStatus;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    private static class FundTransfer {
        private String transactionId;
        private String fromAccount;
        private String toAccount;
        private Money amount;
        private LocalDateTime timestamp;
        private String status;
    }
}