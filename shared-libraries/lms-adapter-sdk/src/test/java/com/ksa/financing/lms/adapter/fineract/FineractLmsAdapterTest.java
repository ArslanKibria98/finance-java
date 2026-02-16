package com.ksa.financing.lms.adapter.fineract;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.ksa.financing.lms.adapter.fineract.dto.*;
import com.ksa.financing.lms.config.FineractConfig;
import com.ksa.financing.lms.dto.*;
import com.ksa.financing.lms.intent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for FineractLmsAdapter using WireMock.
 */
@ExtendWith(MockitoExtension.class)
@WireMockTest(httpPort = 8089)
class FineractLmsAdapterTest {

    private FineractLmsAdapter adapter;

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private IdempotencyStore idempotencyStore;

    private FineractClient fineractClient;
    private FineractMapper fineractMapper;
    private FineractConfig fineractConfig;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wmRuntimeInfo) {
        // Setup config
        fineractConfig = new FineractConfig();
        fineractConfig.setBaseUrl("http://localhost:" + wmRuntimeInfo.getHttpPort() + "/fineract-provider/api/v1");
        fineractConfig.setUsername("mifos");
        fineractConfig.setPassword("password");
        fineractConfig.setTenantId("default");

        // Create RestTemplate
        RestTemplate restTemplate = new RestTemplate();

        // Create client and mapper
        fineractClient = new FineractClient(restTemplate, fineractConfig);
        fineractMapper = new FineractMapper();

        // Create adapter
        adapter = new FineractLmsAdapter(fineractClient, fineractMapper, redissonClient, idempotencyStore);
    }

    @Test
    void testCreateLoanAccount() {
        // Given
        LoanIntent intent = LoanIntent.builder()
            .customerId("1001")
            .customerName("Ahmed Al-Saudi")
            .nationalId("1234567890")
            .productCode("PERSONAL")
            .principalAmount(BigDecimal.valueOf(50000))
            .profitAmount(BigDecimal.valueOf(5000))
            .tenureInMonths(12)
            .expectedDisbursementDate(LocalDate.now().plusDays(7))
            .shariaStructure("TAWARRUQ")
            .commodityId("COMM-123")
            .commodityCost(BigDecimal.valueOf(50000))
            .commoditySalePrice(BigDecimal.valueOf(55000))
            .repaymentFrequency("MONTHLY")
            .installmentAmount(BigDecimal.valueOf(4583.33))
            .firstInstallmentDate(LocalDate.now().plusMonths(1))
            .purpose("Personal Finance")
            .branchCode("BR001")
            .officerId("OFF001")
            .requestId("REQ-123")
            .tenantId("default")
            .build();

        // Mock WireMock response
        stubFor(post(urlEqualTo("/fineract-provider/api/v1/loans"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "loanId": 12345,
                        "resourceId": 12345,
                        "clientId": 1001,
                        "officeId": 1,
                        "externalId": "REQ-123"
                    }
                    """)));

        when(idempotencyStore.exists(anyString())).thenReturn(false);

        // When
        LoanAccountId result = adapter.createLoanAccount(intent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getValue()).isEqualTo("FIN-12345");

        // Verify WireMock was called
        verify(postRequestedFor(urlEqualTo("/fineract-provider/api/v1/loans"))
            .withHeader("Fineract-Platform-TenantId", equalTo("default")));
    }

    @Test
    void testApproveLoan() {
        // Given
        ApprovalIntent intent = ApprovalIntent.builder()
            .loanAccountId("FIN-12345")
            .approverId("APP001")
            .approverName("Manager")
            .approverRole("MANAGER")
            .decision(ApprovalIntent.ApprovalDecision.APPROVED)
            .approvalNotes("Approved based on credit check")
            .approvalTimestamp(LocalDateTime.now())
            .riskScore("750")
            .riskCategory("LOW")
            .amlCheckPassed(true)
            .creditCheckPassed(true)
            .shariaComplianceVerified(true)
            .approvalReference("APPR-001")
            .workflowInstanceId("WF-001")
            .build();

        // Mock WireMock response
        stubFor(post(urlEqualTo("/fineract-provider/api/v1/loans/12345?command=approve"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "resourceId": 12345,
                        "loanId": 12345,
                        "changes": {
                            "status": "APPROVED"
                        }
                    }
                    """)));

        // When
        ApprovalResult result = adapter.approveLoan(intent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isApproved()).isTrue();
        assertThat(result.getNewStatus()).isEqualTo(LoanStatus.APPROVED);
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void testDisburseLoan() throws InterruptedException {
        // Given
        DisbursementIntent intent = DisbursementIntent.builder()
            .loanAccountId("FIN-12345")
            .disbursementAmount(BigDecimal.valueOf(50000))
            .disbursementDate(LocalDate.now())
            .method(DisbursementIntent.DisbursementMethod.BANK_TRANSFER)
            .beneficiaryAccountNumber("SA1234567890")
            .beneficiaryBankCode("RIBL")
            .beneficiaryName("Ahmed Al-Saudi")
            .isPartialDisbursement(false)
            .disbursementSequence(1)
            .totalApprovedAmount(BigDecimal.valueOf(50000))
            .commodityPurchaseReference("PURCH-123")
            .commoditySaleReference("SALE-123")
            .commodityTransferCompleted(true)
            .idempotencyKey("DISB-KEY-123")
            .transactionReference("TXN-REF-123")
            .disbursedBy("OFF001")
            .approvalReference("APPR-001")
            .build();

        // Mock distributed lock
        when(redissonClient.getLock(anyString())).thenReturn(lock);
        when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(idempotencyStore.exists(anyString())).thenReturn(false);

        // Mock WireMock response
        stubFor(post(urlEqualTo("/fineract-provider/api/v1/loans/12345?command=disburse"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "resourceId": 12345,
                        "subResourceId": 67890,
                        "loanId": 12345,
                        "changes": {
                            "status": "ACTIVE",
                            "disbursedAmount": 50000
                        }
                    }
                    """)));

        // When
        DisbursementResult result = adapter.disburseLoan(intent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getDisbursedAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(result.getStatus()).isEqualTo(DisbursementResult.DisbursementStatus.COMPLETED);
        assertThat(result.getTransactionId()).isEqualTo("TXN-67890");
    }

    @Test
    void testRecordRepayment() {
        // Given
        RepaymentIntent intent = RepaymentIntent.builder()
            .loanAccountId("FIN-12345")
            .paymentAmount(BigDecimal.valueOf(4583.33))
            .paymentDateTime(LocalDateTime.now())
            .paymentMethod(RepaymentIntent.PaymentMethod.SADAD)
            .paymentReference("SADAD-123")
            .transactionId("TXN-789")
            .paymentType(RepaymentIntent.PaymentType.REGULAR_INSTALLMENT)
            .principalAmount(BigDecimal.valueOf(4166.67))
            .profitAmount(BigDecimal.valueOf(416.66))
            .penaltyAmount(BigDecimal.ZERO)
            .charityAmount(BigDecimal.ZERO)
            .isPartialPayment(false)
            .installmentNumber(1)
            .payerAccountNumber("SA9876543210")
            .payerBankCode("RIBL")
            .receiptNumber("REC-123")
            .idempotencyKey("PAY-KEY-123")
            .collectedBy("SADAD")
            .collectionChannel("ONLINE")
            .build();

        when(idempotencyStore.exists(anyString())).thenReturn(false);

        // Mock WireMock response
        stubFor(post(urlEqualTo("/fineract-provider/api/v1/loans/12345/transactions?command=repayment"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "resourceId": 99999,
                        "transactionId": 99999,
                        "loanId": 12345,
                        "transactionDate": "2024-02-12",
                        "transactionAmount": 4583.33
                    }
                    """)));

        // When
        RepaymentResult result = adapter.recordRepayment(intent);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getPaymentAmount()).isEqualTo(BigDecimal.valueOf(4583.33));
        assertThat(result.getTransactionId()).isEqualTo("PAY-99999");
        assertThat(result.getStatus()).isEqualTo(RepaymentResult.PaymentStatus.POSTED);
    }

    @Test
    void testGetLoanDetails() {
        // Given
        LoanAccountId loanAccountId = LoanAccountId.of("FIN-12345");

        // Mock WireMock response
        stubFor(get(urlEqualTo("/fineract-provider/api/v1/loans/12345?associations=all"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBodyFile("loan-details-response.json"))); // Would need to create this file

        // Simplified response for test
        stubFor(get(urlEqualTo("/fineract-provider/api/v1/loans/12345?associations=all"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "id": 12345,
                        "accountNo": "000012345",
                        "clientId": 1001,
                        "clientName": "Ahmed Al-Saudi",
                        "loanProductId": 1,
                        "loanProductName": "Personal Finance",
                        "principal": 50000,
                        "numberOfRepayments": 12,
                        "repaymentFrequencyType": 2,
                        "status": {
                            "id": 300,
                            "code": "loanStatusType.active",
                            "value": "Active"
                        },
                        "disbursedAmount": 50000,
                        "principalOutstanding": 45833.33,
                        "interestOutstanding": 4583.33,
                        "totalOutstanding": 50416.66
                    }
                    """)));

        // When
        LoanDetails result = adapter.getLoanDetails(loanAccountId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getLoanAccountId()).isEqualTo(loanAccountId);
        assertThat(result.getCustomerId()).isEqualTo("1001");
        assertThat(result.getCustomerName()).isEqualTo("Ahmed Al-Saudi");
        assertThat(result.getPrincipalAmount()).isEqualTo(BigDecimal.valueOf(50000));
        assertThat(result.getStatus()).isEqualTo(LoanStatus.ACTIVE);
    }

    @Test
    void testHealthCheck() {
        // Mock health endpoint
        stubFor(get(urlEqualTo("/fineract-provider/actuator/health"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("""
                    {
                        "status": "UP"
                    }
                    """)));

        // When
        boolean isHealthy = adapter.isHealthy();

        // Then
        assertThat(isHealthy).isTrue();
    }
}