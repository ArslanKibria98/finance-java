package com.ksa.financing.lms.adapter.fineract;

import com.ksa.financing.lms.adapter.fineract.dto.*;
import com.ksa.financing.lms.config.FineractConfig;
import com.ksa.financing.lms.exception.FineractException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

/**
 * REST client for Apache Fineract API communication.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FineractClient {

    private final RestTemplate fineractRestTemplate;
    private final FineractConfig fineractConfig;

    /**
     * Creates a new loan product in Fineract.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractLoanProductResponse createLoanProduct(FineractLoanProductRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loanproducts";
        log.debug("Creating loan product in Fineract: {}", request.getName());

        try {
            ResponseEntity<FineractLoanProductResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractLoanProductResponse.class);

            log.info("Loan product created successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create loan product: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to create loan product", e);
        }
    }

    /**
     * Creates a new loan in Fineract.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractLoanResponse createLoan(FineractLoanRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loans";
        log.debug("Creating loan in Fineract: {}", request);

        try {
            ResponseEntity<FineractLoanResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractLoanResponse.class);

            log.info("Loan created successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create loan: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to create loan", e);
        }
    }

    /**
     * Approves a loan application.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractCommandResponse approveLoan(Long loanId, FineractApprovalRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loans/" + loanId + "?command=approve";
        log.debug("Approving loan {}: {}", loanId, request);

        try {
            ResponseEntity<FineractCommandResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractCommandResponse.class);

            log.info("Loan approved successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to approve loan: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to approve loan", e);
        }
    }

    /**
     * Disburses a loan.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractCommandResponse disburseLoan(Long loanId, FineractDisbursementRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loans/" + loanId + "?command=disburse";
        log.debug("Disbursing loan {}: {}", loanId, request);

        try {
            ResponseEntity<FineractCommandResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractCommandResponse.class);

            log.info("Loan disbursed successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to disburse loan: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to disburse loan", e);
        }
    }

    /**
     * Records a loan repayment.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractTransactionResponse recordRepayment(Long loanId, FineractRepaymentRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loans/" + loanId + "/transactions?command=repayment";
        log.debug("Recording repayment for loan {}: {}", loanId, request);

        try {
            ResponseEntity<FineractTransactionResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractTransactionResponse.class);

            log.info("Repayment recorded successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to record repayment: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to record repayment", e);
        }
    }

    /**
     * Retrieves loan details.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractLoanDetails getLoanDetails(Long loanId) {
        String url = UriComponentsBuilder.fromHttpUrl(fineractConfig.getBaseUrl() + "/loans/" + loanId)
            .queryParam("associations", "all")
            .toUriString();

        log.debug("Fetching loan details for: {}", loanId);

        try {
            ResponseEntity<FineractLoanDetails> response = fineractRestTemplate.getForEntity(
                url, FineractLoanDetails.class);

            log.info("Loan details fetched successfully");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch loan details: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to fetch loan details", e);
        }
    }

    /**
     * Retrieves repayment schedule.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractRepaymentSchedule getRepaymentSchedule(Long loanId) {
        String url = UriComponentsBuilder.fromHttpUrl(fineractConfig.getBaseUrl() + "/loans/" + loanId)
            .queryParam("associations", "repaymentSchedule")
            .toUriString();

        log.debug("Fetching repayment schedule for loan: {}", loanId);

        try {
            ResponseEntity<FineractRepaymentSchedule> response = fineractRestTemplate.getForEntity(
                url, FineractRepaymentSchedule.class);

            log.info("Repayment schedule fetched successfully");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch repayment schedule: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to fetch repayment schedule", e);
        }
    }

    /**
     * Creates a journal entry.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractJournalResponse createJournalEntry(FineractJournalRequest request) {
        String url = fineractConfig.getBaseUrl() + "/journalentries";
        log.debug("Creating journal entry: {}", request);

        try {
            ResponseEntity<FineractJournalResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractJournalResponse.class);

            log.info("Journal entry created successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to create journal entry: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to create journal entry", e);
        }
    }

    /**
     * Gets GL account balance.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractAccountBalance getAccountBalance(Long glAccountId) {
        String url = fineractConfig.getBaseUrl() + "/glaccounts/" + glAccountId + "/balance";
        log.debug("Fetching GL account balance for: {}", glAccountId);

        try {
            ResponseEntity<FineractAccountBalance> response = fineractRestTemplate.getForEntity(
                url, FineractAccountBalance.class);

            log.info("Account balance fetched successfully");
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to fetch account balance: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to fetch account balance", e);
        }
    }

    /**
     * Processes early settlement.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractCommandResponse processEarlySettlement(Long loanId, FineractSettlementRequest request) {
        String url = fineractConfig.getBaseUrl() + "/loans/" + loanId + "?command=prepayLoan";
        log.debug("Processing early settlement for loan {}: {}", loanId, request);

        try {
            ResponseEntity<FineractCommandResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractCommandResponse.class);

            log.info("Early settlement processed successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to process early settlement: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to process early settlement", e);
        }
    }

    /**
     * Submits a loan reschedule request to Fineract.
     * POST /rescheduleloans
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractRescheduleResponse submitReschedule(FineractRescheduleRequest request) {
        String url = fineractConfig.getBaseUrl() + "/rescheduleloans";
        log.debug("Submitting reschedule request for loan {}: {}", request.getLoanId(), request.getRescheduleFromDate());

        try {
            ResponseEntity<FineractRescheduleResponse> response = fineractRestTemplate.postForEntity(
                url, request, FineractRescheduleResponse.class);

            log.info("Reschedule request submitted successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to submit reschedule request: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to submit reschedule to Fineract", e);
        }
    }

    /**
     * Approves a pending reschedule request in Fineract.
     * POST /rescheduleloans/{id}?command=approve
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractRescheduleResponse approveReschedule(Long rescheduleId, String approvedOnDate) {
        String url = fineractConfig.getBaseUrl() + "/rescheduleloans/" + rescheduleId + "?command=approve";
        log.debug("Approving reschedule {} in Fineract", rescheduleId);

        Map<String, Object> approvalBody = Map.of(
            "approvedOnDate", approvedOnDate,
            "dateFormat", "dd MMMM yyyy",
            "locale", "en"
        );

        try {
            ResponseEntity<FineractRescheduleResponse> response = fineractRestTemplate.postForEntity(
                url, approvalBody, FineractRescheduleResponse.class);

            log.info("Reschedule approved successfully in Fineract: {}", rescheduleId);
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to approve reschedule {}: {}", rescheduleId, e.getResponseBodyAsString());
            throw new FineractException("Failed to approve reschedule in Fineract", e);
        }
    }

    /**
     * Reverses a transaction.
     */
    @Retry(name = "fineract-api")
    @CircuitBreaker(name = "fineract-api")
    public FineractCommandResponse reverseTransaction(Long loanId, Long transactionId, Map<String, Object> reversal) {
        String url = fineractConfig.getBaseUrl() + "/loans/" + loanId + "/transactions/" + transactionId + "?command=adjust";
        log.debug("Reversing transaction {} for loan {}", transactionId, loanId);

        try {
            ResponseEntity<FineractCommandResponse> response = fineractRestTemplate.postForEntity(
                url, reversal, FineractCommandResponse.class);

            log.info("Transaction reversed successfully: {}", response.getBody());
            return response.getBody();
        } catch (HttpClientErrorException e) {
            log.error("Failed to reverse transaction: {}", e.getResponseBodyAsString());
            throw new FineractException("Failed to reverse transaction", e);
        }
    }

    /**
     * Checks Fineract health status.
     */
    public boolean isHealthy() {
        try {
            String url = fineractConfig.getBaseUrl().replace("/api/v1", "") + "/actuator/health";
            ResponseEntity<Map> response = fineractRestTemplate.getForEntity(url, Map.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.warn("Fineract health check failed: {}", e.getMessage());
            return false;
        }
    }
}