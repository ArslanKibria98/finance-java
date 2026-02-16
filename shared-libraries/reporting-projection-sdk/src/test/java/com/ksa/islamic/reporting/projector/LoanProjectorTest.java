package com.ksa.islamic.reporting.projector;

import com.ksa.islamic.reporting.event.LoanEvents;
import com.ksa.islamic.reporting.readmodel.LoanSummaryReadModel;
import com.ksa.islamic.reporting.repository.LoanReadRepository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.trace.Tracer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LoanProjector.
 */
@ExtendWith(MockitoExtension.class)
class LoanProjectorTest {

    @Mock
    private ProjectionCheckpointRepository checkpointRepository;

    @Mock
    private LoanReadRepository loanReadRepository;

    @Mock
    private Tracer tracer;

    private LoanProjector loanProjector;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        loanProjector = new LoanProjector(
                checkpointRepository,
                meterRegistry,
                tracer,
                loanReadRepository
        );
    }

    @Test
    void handleLoanApproved_CreatesNewLoanSummary() {
        // Given
        LoanEvents.LoanApproved event = LoanEvents.LoanApproved.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-001")
                .tenantId("tenant-001")
                .loanAccountNumber("LN-2024-001")
                .customerId("CUST-001")
                .customerName("Ahmad Al-Rashid")
                .productType("MURABAHA")
                .principalAmount(new BigDecimal("100000.00"))
                .interestRate(new BigDecimal("5.5"))
                .termMonths(60)
                .approvalDate(LocalDate.now())
                .approverUserId("USER-001")
                .build();

        // When
        loanProjector.projectIncremental(event);

        // Then
        ArgumentCaptor<LoanSummaryReadModel> captor = ArgumentCaptor.forClass(LoanSummaryReadModel.class);
        verify(loanReadRepository).save(captor.capture());

        LoanSummaryReadModel savedLoan = captor.getValue();
        assertThat(savedLoan).isNotNull();
        assertThat(savedLoan.getLoanId()).isEqualTo("LOAN-001");
        assertThat(savedLoan.getLoanAccountNumber()).isEqualTo("LN-2024-001");
        assertThat(savedLoan.getCustomerId()).isEqualTo("CUST-001");
        assertThat(savedLoan.getCustomerName()).isEqualTo("Ahmad Al-Rashid");
        assertThat(savedLoan.getPrincipalAmount()).isEqualByComparingTo("100000.00");
        assertThat(savedLoan.getOutstandingPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(savedLoan.getInterestRate()).isEqualByComparingTo("5.5");
        assertThat(savedLoan.getStatus()).isEqualTo("APPROVED");
        assertThat(savedLoan.getNumberOfInstallments()).isEqualTo(60);
        assertThat(savedLoan.getInstallmentsPaid()).isEqualTo(0);
    }

    @Test
    void handleLoanDisbursed_UpdatesExistingLoan() {
        // Given
        LoanSummaryReadModel existingLoan = LoanSummaryReadModel.builder()
                .loanId("LOAN-001")
                .loanAccountNumber("LN-2024-001")
                .status("APPROVED")
                .numberOfInstallments(60)
                .build();

        when(loanReadRepository.findById("LOAN-001")).thenReturn(Optional.of(existingLoan));

        LoanEvents.LoanDisbursed event = LoanEvents.LoanDisbursed.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-001")
                .tenantId("tenant-001")
                .loanAccountNumber("LN-2024-001")
                .customerId("CUST-001")
                .disbursementAmount(new BigDecimal("100000.00"))
                .disbursementDate(LocalDate.now())
                .disbursementChannel("BANK_TRANSFER")
                .bankAccountNumber("ACC-12345")
                .build();

        // When
        loanProjector.projectIncremental(event);

        // Then
        verify(loanReadRepository).save(existingLoan);
        assertThat(existingLoan.getStatus()).isEqualTo("DISBURSED");
        assertThat(existingLoan.getDisbursementDate()).isEqualTo(LocalDate.now());
        assertThat(existingLoan.getOutstandingPrincipal()).isEqualByComparingTo("100000.00");
        assertThat(existingLoan.getMaturityDate()).isNotNull();
        assertThat(existingLoan.getNextPaymentDate()).isNotNull();
    }

    @Test
    void handlePaymentReceived_UpdatesOutstandingAmounts() {
        // Given
        LoanSummaryReadModel existingLoan = LoanSummaryReadModel.builder()
                .loanId("LOAN-001")
                .loanAccountNumber("LN-2024-001")
                .status("DISBURSED")
                .outstandingPrincipal(new BigDecimal("90000.00"))
                .paidPrincipal(new BigDecimal("10000.00"))
                .paidInterest(new BigDecimal("500.00"))
                .installmentsPaid(1)
                .earlyPayments(1)
                .latePayments(0)
                .lateFees(BigDecimal.ZERO)
                .daysOverdue(0)
                .build();

        when(loanReadRepository.findById("LOAN-001")).thenReturn(Optional.of(existingLoan));

        LoanEvents.PaymentReceived event = LoanEvents.PaymentReceived.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-001")
                .tenantId("tenant-001")
                .loanAccountNumber("LN-2024-001")
                .customerId("CUST-001")
                .paymentId("PAY-001")
                .paymentAmount(new BigDecimal("2000.00"))
                .principalPortion(new BigDecimal("1500.00"))
                .interestPortion(new BigDecimal("450.00"))
                .lateFee(new BigDecimal("50.00"))
                .paymentDate(LocalDate.now())
                .paymentChannel("SADAD")
                .outstandingAfter(new BigDecimal("88500.00"))
                .build();

        // When
        loanProjector.projectIncremental(event);

        // Then
        verify(loanReadRepository).save(existingLoan);
        assertThat(existingLoan.getOutstandingPrincipal()).isEqualByComparingTo("88500.00");
        assertThat(existingLoan.getPaidPrincipal()).isEqualByComparingTo("11500.00");
        assertThat(existingLoan.getPaidInterest()).isEqualByComparingTo("950.00");
        assertThat(existingLoan.getLateFees()).isEqualByComparingTo("50.00");
        assertThat(existingLoan.getInstallmentsPaid()).isEqualTo(2);
        assertThat(existingLoan.getLatePayments()).isEqualTo(1); // Due to late fee
        assertThat(existingLoan.getLastPaymentDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void handleLoanOverdue_UpdatesRiskAndCollectionStatus() {
        // Given
        LoanSummaryReadModel existingLoan = LoanSummaryReadModel.builder()
                .loanId("LOAN-001")
                .loanAccountNumber("LN-2024-001")
                .status("DISBURSED")
                .riskCategory("LOW")
                .paymentPerformanceScore(100)
                .collectionStatus("CURRENT")
                .build();

        when(loanReadRepository.findById("LOAN-001")).thenReturn(Optional.of(existingLoan));

        LoanEvents.LoanOverdue event = LoanEvents.LoanOverdue.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-001")
                .tenantId("tenant-001")
                .loanAccountNumber("LN-2024-001")
                .customerId("CUST-001")
                .daysOverdue(45)
                .overdueAmount(new BigDecimal("4500.00"))
                .overdueInstallments(2)
                .lastPaymentDate(LocalDate.now().minusDays(45))
                .build();

        // When
        loanProjector.projectIncremental(event);

        // Then
        verify(loanReadRepository).save(existingLoan);
        assertThat(existingLoan.getDaysOverdue()).isEqualTo(45);
        assertThat(existingLoan.getOverdueAmount()).isEqualByComparingTo("4500.00");
        assertThat(existingLoan.getOverdueInstallments()).isEqualTo(2);
        assertThat(existingLoan.getRiskCategory()).isEqualTo("MEDIUM");
        assertThat(existingLoan.getCollectionStatus()).isEqualTo("IN_COLLECTION");
        assertThat(existingLoan.getPaymentPerformanceScore()).isLessThan(100);
    }

    @Test
    void handleLoanClosed_ResetsOutstandingAmounts() {
        // Given
        LoanSummaryReadModel existingLoan = LoanSummaryReadModel.builder()
                .loanId("LOAN-001")
                .loanAccountNumber("LN-2024-001")
                .status("ACTIVE")
                .outstandingPrincipal(new BigDecimal("1000.00"))
                .overdueAmount(new BigDecimal("100.00"))
                .daysOverdue(5)
                .overdueInstallments(1)
                .collectionStatus("REMINDER_SENT")
                .build();

        when(loanReadRepository.findById("LOAN-001")).thenReturn(Optional.of(existingLoan));

        LoanEvents.LoanClosed event = LoanEvents.LoanClosed.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .aggregateId("LOAN-001")
                .tenantId("tenant-001")
                .loanAccountNumber("LN-2024-001")
                .customerId("CUST-001")
                .closureDate(LocalDate.now())
                .closureReason("FULLY_PAID")
                .finalPaymentAmount(new BigDecimal("1100.00"))
                .build();

        // When
        loanProjector.projectIncremental(event);

        // Then
        verify(loanReadRepository).save(existingLoan);
        assertThat(existingLoan.getStatus()).isEqualTo("CLOSED");
        assertThat(existingLoan.getOutstandingPrincipal()).isEqualByComparingTo("0");
        assertThat(existingLoan.getOverdueAmount()).isEqualByComparingTo("0");
        assertThat(existingLoan.getDaysOverdue()).isEqualTo(0);
        assertThat(existingLoan.getOverdueInstallments()).isEqualTo(0);
        assertThat(existingLoan.getCollectionStatus()).isEqualTo("CLOSED");
    }

    @Test
    void determineStrategy_ReturnsIncremental() {
        // Given
        BaseProjectableEvent event = LoanEvents.LoanApproved.builder()
                .eventId(UUID.randomUUID())
                .occurredOn(LocalDateTime.now())
                .build();

        // When
        ProjectionStrategy strategy = loanProjector.determineStrategy(event);

        // Then
        assertThat(strategy).isEqualTo(ProjectionStrategy.INCREMENTAL);
    }

    @Test
    void getTopics_ReturnsCorrectTopics() {
        // When
        String[] topics = loanProjector.getTopics();

        // Then
        assertThat(topics).containsExactly(
                "domain.loan.approved",
                "domain.loan.disbursed",
                "domain.loan.payment",
                "domain.loan.overdue",
                "domain.loan.closed",
                "domain.loan.restructured"
        );
    }

    @Test
    void getGroupId_ReturnsCorrectGroupId() {
        // When
        String groupId = loanProjector.getGroupId();

        // Then
        assertThat(groupId).isEqualTo("loan-projector-group");
    }
}