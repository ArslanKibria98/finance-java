package com.ksa.financing.lending.adapter.temporal.workflow;

import com.ksa.islamic.orchestration.activity.lending.LoanApplicationActivity;
import com.ksa.islamic.orchestration.activity.lending.LoanApplicationWorkflow;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * SAGA-based loan application workflow implementation.
 * Compensates on failure by cancelling the application.
 */
public class LoanApplicationWorkflowImpl implements LoanApplicationWorkflow {

    private static final Logger log = Workflow.getLogger(LoanApplicationWorkflowImpl.class);

    // State
    private String currentStage = "INITIATED";
    private String applicationStatus = "DRAFT";
    private String applicationId;
    private String applicationNumber;

    // Signal data
    private DocumentsVerifiedSignal documentsSignal;
    private boolean documentsVerified = false;

    private ManualApprovalSignal approvalSignal;
    private boolean manualApprovalReceived = false;

    // Activity options
    private final ActivityOptions defaultOptions = ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofMinutes(2))
            .setRetryOptions(RetryOptions.newBuilder()
                    .setInitialInterval(Duration.ofSeconds(2))
                    .setMaximumInterval(Duration.ofSeconds(30))
                    .setBackoffCoefficient(2.0)
                    .setMaximumAttempts(3)
                    .build())
            .build();

    private final LoanApplicationActivity lendingActivity =
            Workflow.newActivityStub(LoanApplicationActivity.class, defaultOptions);

    @Override
    public LoanApplicationResult execute(LoanApplicationRequest request) {
        String workflowId = Workflow.getInfo().getWorkflowId();
        log.info("Starting loan application workflow: {}", workflowId);

        try {
            // ── STEP 1: Create Application ──
            currentStage = "CREATING_APPLICATION";
            log.info("Step 1: Creating loan application");

            var createResult = lendingActivity.createLoanApplication(
                    new LoanApplicationActivity.CreateApplicationInput(
                            request.tenantId(), request.customerId(),
                            request.productId(), request.productCode(),
                            request.shariaStructure(), request.requestedAmount(),
                            request.requestedTenureMonths(), request.partnerId(),
                            request.leadId(), request.createdBy()
                    )
            );

            applicationId = createResult.applicationId();
            applicationNumber = createResult.applicationNumber();
            applicationStatus = "DRAFT";

            // ── STEP 2: Submit Application ──
            currentStage = "SUBMITTING";
            log.info("Step 2: Submitting application: {}", applicationNumber);

            lendingActivity.updateApplicationStatus(
                    new LoanApplicationActivity.UpdateStatusInput(
                            request.tenantId(), applicationId, "SUBMITTED", request.createdBy()
                    )
            );
            applicationStatus = "SUBMITTED";

            // ── STEP 3: Document Verification (signal wait) ──
            currentStage = "DOCUMENTS_PENDING";
            lendingActivity.updateApplicationStatus(
                    new LoanApplicationActivity.UpdateStatusInput(
                            request.tenantId(), applicationId, "DOCUMENTS_PENDING", request.createdBy()
                    )
            );
            applicationStatus = "DOCUMENTS_PENDING";

            log.info("Step 3: Waiting for document verification signal");
            boolean docsReceived = Workflow.await(Duration.ofHours(72), () -> documentsVerified);
            if (!docsReceived) {
                return compensateAndFail(request.tenantId(), request.createdBy(), workflowId,
                        "Document verification timed out (72 hours)");
            }
            if (!documentsSignal.verified()) {
                return compensateAndFail(request.tenantId(), request.createdBy(), workflowId,
                        "Documents rejected");
            }

            // ── STEP 4: Credit Check ──
            currentStage = "CREDIT_CHECK";
            lendingActivity.updateApplicationStatus(
                    new LoanApplicationActivity.UpdateStatusInput(
                            request.tenantId(), applicationId, "CREDIT_CHECK", request.createdBy()
                    )
            );
            applicationStatus = "CREDIT_CHECK";

            log.info("Step 4: Performing credit check");
            var creditResult = lendingActivity.performCreditCheck(
                    new LoanApplicationActivity.CreditCheckInput(
                            request.tenantId(), request.customerId(),
                            null, request.requestedAmount(), request.requestedTenureMonths()
                    )
            );

            if (!creditResult.passed()) {
                // Reject application
                currentStage = "REJECTED";
                lendingActivity.updateApplicationStatus(
                        new LoanApplicationActivity.UpdateStatusInput(
                                request.tenantId(), applicationId, "REJECTED", request.createdBy()
                        )
                );
                applicationStatus = "REJECTED";
                return new LoanApplicationResult(
                        workflowId, applicationId, applicationNumber,
                        null, null, "REJECTED", creditResult.reason());
            }

            // ── STEP 5: Sharia Validation ──
            currentStage = "SHARIA_VALIDATION";
            lendingActivity.updateApplicationStatus(
                    new LoanApplicationActivity.UpdateStatusInput(
                            request.tenantId(), applicationId, "SHARIA_VALIDATION", request.createdBy()
                    )
            );
            applicationStatus = "SHARIA_VALIDATION";

            log.info("Step 5: Validating sharia compliance");
            var shariaResult = lendingActivity.validateShariaCompliance(
                    new LoanApplicationActivity.ShariaValidationInput(
                            request.tenantId(), request.productId(),
                            request.shariaStructure(), request.requestedAmount(),
                            request.requestedTenureMonths()
                    )
            );

            if (!shariaResult.compliant()) {
                return compensateAndFail(request.tenantId(), request.createdBy(), workflowId,
                        "Sharia validation failed: " + shariaResult.reason());
            }

            // ── STEP 6: Profit Calculation ──
            currentStage = "PROFIT_CALCULATION";
            log.info("Step 6: Calculating profit");

            var profitResult = lendingActivity.calculateProfit(
                    new LoanApplicationActivity.ProfitCalculationInput(
                            request.shariaStructure(), request.requestedAmount(),
                            new BigDecimal("0.05"), request.requestedTenureMonths()
                    )
            );

            // ── STEP 7: Approval Decision (signal wait for manual approval) ──
            currentStage = "PENDING_APPROVAL";
            lendingActivity.updateApplicationStatus(
                    new LoanApplicationActivity.UpdateStatusInput(
                            request.tenantId(), applicationId, "PENDING_APPROVAL", request.createdBy()
                    )
            );
            applicationStatus = "PENDING_APPROVAL";

            log.info("Step 7: Waiting for manual approval signal");
            boolean approvalReceived = Workflow.await(Duration.ofHours(168), () -> manualApprovalReceived);
            if (!approvalReceived) {
                return compensateAndFail(request.tenantId(), request.createdBy(), workflowId,
                        "Approval timed out (7 days)");
            }

            if (!approvalSignal.approved()) {
                currentStage = "REJECTED";
                lendingActivity.updateApplicationStatus(
                        new LoanApplicationActivity.UpdateStatusInput(
                                request.tenantId(), applicationId, "REJECTED", approvalSignal.approvedBy()
                        )
                );
                applicationStatus = "REJECTED";
                return new LoanApplicationResult(
                        workflowId, applicationId, applicationNumber,
                        null, null, "REJECTED", approvalSignal.reason());
            }

            // Approve the application with final terms
            var approvedAmount = approvalSignal.approvedAmount() != null
                    ? approvalSignal.approvedAmount() : request.requestedAmount();
            var approvedTenure = approvalSignal.approvedTenureMonths() > 0
                    ? approvalSignal.approvedTenureMonths() : request.requestedTenureMonths();
            var approvedRate = approvalSignal.approvedProfitRate() != null
                    ? approvalSignal.approvedProfitRate() : new BigDecimal("0.05");

            lendingActivity.processApproval(
                    new LoanApplicationActivity.ApprovalInput(
                            request.tenantId(), applicationId,
                            approvedAmount, approvedTenure, approvedRate,
                            profitResult.totalProfit(), profitResult.totalRepayment(),
                            profitResult.monthlyInstallment(), approvalSignal.approvedBy()
                    )
            );
            applicationStatus = "APPROVED";

            // ── STEP 8: Create Loan ──
            currentStage = "CREATING_LOAN";
            log.info("Step 8: Creating loan from approved application");

            var loanResult = lendingActivity.createLoan(
                    new LoanApplicationActivity.LoanCreationInput(
                            request.tenantId(), applicationId,
                            request.customerId(), request.productId(),
                            request.productCode(), request.shariaStructure(),
                            approvedAmount, profitResult.totalProfit(),
                            approvedRate, approvedTenure,
                            profitResult.monthlyInstallment()
                    )
            );

            currentStage = "COMPLETED";
            applicationStatus = "APPROVED";
            log.info("Loan application workflow completed. Loan: {}", loanResult.loanNumber());

            return new LoanApplicationResult(
                    workflowId, applicationId, applicationNumber,
                    loanResult.loanId(), loanResult.loanNumber(),
                    "APPROVED", null);

        } catch (ApplicationFailure af) {
            throw af;
        } catch (Exception e) {
            log.error("Loan application workflow failed: {}", e.getMessage(), e);
            return compensateAndFail(request.tenantId(), request.createdBy(), workflowId,
                    "Unexpected error: " + e.getMessage());
        }
    }

    // ── Signal Handlers ──

    @Override
    public void documentsVerified(DocumentsVerifiedSignal signal) {
        log.info("Received documents verified signal: {}", signal.verified());
        this.documentsSignal = signal;
        this.documentsVerified = true;
    }

    @Override
    public void manualApproval(ManualApprovalSignal signal) {
        log.info("Received manual approval signal: approved={}", signal.approved());
        this.approvalSignal = signal;
        this.manualApprovalReceived = true;
    }

    // ── Query Methods ──

    @Override
    public String getCurrentStage() {
        return currentStage;
    }

    @Override
    public String getApplicationStatus() {
        return applicationStatus;
    }

    // ── SAGA Compensation ──

    private LoanApplicationResult compensateAndFail(String tenantId, String userId,
                                                      String workflowId, String reason) {
        log.warn("Compensating: cancelling application {} — reason: {}", applicationId, reason);
        try {
            if (applicationId != null) {
                lendingActivity.cancelApplication(
                        new LoanApplicationActivity.CancelApplicationInput(
                                tenantId, applicationId, reason, userId
                        )
                );
            }
        } catch (Exception e) {
            log.error("SAGA compensation failed for application {}: {}", applicationId, e.getMessage());
        }

        currentStage = "FAILED";
        applicationStatus = "CANCELLED";
        throw ApplicationFailure.newNonRetryableFailure(reason, "LOAN_APPLICATION_FAILED");
    }
}
