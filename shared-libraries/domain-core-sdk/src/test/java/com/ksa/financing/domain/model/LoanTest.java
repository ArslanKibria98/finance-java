package com.ksa.financing.domain.model;

import com.ksa.financing.domain.enums.LoanStatus;
import com.ksa.financing.domain.enums.ShariaStructure;
import com.ksa.financing.domain.event.DomainEvent;
import com.ksa.financing.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for Loan aggregate root.
 * <p>
 * Tests cover:
 * - Loan creation with factory method
 * - approve() transitions to PENDING_DISBURSEMENT
 * - reject() transitions to final state
 * - disburse() transitions to ACTIVE
 * - Cannot disburse if not approved (invariant)
 * - markOverdue() with different DPD values
 * - settle() transitions to SETTLED
 * - writeOff() transitions to WRITTEN_OFF
 * - recordPayment() updates outstanding balances
 * - recordPayment() prevents negative outstanding amounts
 * - Domain events are published on state transitions
 * </p>
 */
@DisplayName("Loan Tests")
class LoanTest {

    private ContractId contractId;
    private CustomerId customerId;
    private ProductId productId;
    private SarMoney principal;
    private ProfitRate profitRate;
    private Tenure tenure;
    private ShariaStructure shariaStructure;
    private LocalDate bookingDate;

    @BeforeEach
    void setUp() {
        contractId = ContractId.generate();
        customerId = CustomerId.generate();
        productId = ProductId.generate();
        principal = SarMoney.of(100000);
        profitRate = ProfitRate.ofPercentage(5.0);
        tenure = Tenure.ofMonths(12);
        shariaStructure = ShariaStructure.MURABAHA;
        bookingDate = LocalDate.of(2024, 1, 1);
    }

    @Test
    @DisplayName("Should create new loan with factory method")
    void shouldCreateNewLoan() {
        // When
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // Then
        assertThat(loan).isNotNull();
        assertThat(loan.getLoanId()).isNotNull();
        assertThat(loan.getContractId()).isEqualTo(contractId);
        assertThat(loan.getCustomerId()).isEqualTo(customerId);
        assertThat(loan.getProductId()).isEqualTo(productId);
        assertThat(loan.getPrincipal()).isEqualTo(principal);
        assertThat(loan.getProfitRate()).isEqualTo(profitRate);
        assertThat(loan.getTenure()).isEqualTo(tenure);
        assertThat(loan.getShariaStructure()).isEqualTo(shariaStructure);
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_DISBURSEMENT);
        assertThat(loan.getOutstandingPrincipal()).isEqualTo(principal);
        assertThat(loan.getOutstandingProfit()).isEqualTo(SarMoney.of(5000.00));
        assertThat(loan.getCurrentDPD()).isZero();
        assertThat(loan.getMaxDPD()).isZero();
    }

    @Test
    @DisplayName("Should calculate profit amount and total amount on creation")
    void shouldCalculateProfitAndTotalAmountOnCreation() {
        // When
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // Then
        assertThat(loan.getProfitAmount()).isEqualTo(SarMoney.of(5000.00));
        assertThat(loan.getTotalAmount()).isEqualTo(SarMoney.of(105000.00));
    }

    @Test
    @DisplayName("Should publish LoanCreatedEvent on creation")
    void shouldPublishLoanCreatedEventOnCreation() {
        // When
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // Then
        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanCreatedEvent.class);

        Loan.LoanCreatedEvent event = (Loan.LoanCreatedEvent) events.get(0);
        assertThat(event.loanId()).isEqualTo(loan.getLoanId());
        assertThat(event.customerId()).isEqualTo(customerId);
        assertThat(event.principal()).isEqualTo(principal);
    }

    @Test
    @DisplayName("Should approve loan and publish approval event")
    void shouldApproveLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.clearDomainEvents();

        // When
        loan.approve();

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_DISBURSEMENT);
        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanApprovedEvent.class);
    }

    @Test
    @DisplayName("Should reject loan and transition to CLOSED status")
    void shouldRejectLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.clearDomainEvents();

        // When
        loan.reject();

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanRejectedEvent.class);
    }

    @Test
    @DisplayName("Should disburse loan and transition to ACTIVE status")
    void shouldDisburseLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        LocalDate disbursementDate = bookingDate.plusDays(7);
        loan.clearDomainEvents();

        // When
        loan.disburse(disbursementDate);

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getDisbursementDate()).isEqualTo(disbursementDate);
        assertThat(loan.getMaturityDate()).isEqualTo(disbursementDate.plusMonths(12));

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanDisbursedEvent.class);
    }

    @Test
    @DisplayName("Should throw exception when disbursing loan not in PENDING_DISBURSEMENT")
    void shouldThrowExceptionWhenDisbursingNonApprovedLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        LocalDate disbursementDate = bookingDate.plusDays(7);
        loan.disburse(disbursementDate);

        // When/Then
        assertThatThrownBy(() -> loan.disburse(disbursementDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot disburse loan in status ACTIVE");
    }

    @Test
    @DisplayName("Should throw exception when disbursement date is before booking date")
    void shouldThrowExceptionWhenDisbursementDateIsBeforeBookingDate() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        LocalDate invalidDate = bookingDate.minusDays(1);

        // When/Then
        assertThatThrownBy(() -> loan.disburse(invalidDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Disbursement date cannot be before booking date");
    }

    @Test
    @DisplayName("Should mark loan as overdue and transition to DELINQUENT")
    void shouldMarkLoanOverdue() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        loan.clearDomainEvents();

        // When
        loan.markOverdue(30);

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DELINQUENT);
        assertThat(loan.getCurrentDPD()).isEqualTo(30);
        assertThat(loan.getMaxDPD()).isEqualTo(30);

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanMarkedOverdueEvent.class);
    }

    @Test
    @DisplayName("Should mark loan as DEFAULT when DPD exceeds 90")
    void shouldMarkLoanAsDefaultWhenDpdExceeds90() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));

        // When
        loan.markOverdue(91);

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.DEFAULT);
        assertThat(loan.getCurrentDPD()).isEqualTo(91);
    }

    @Test
    @DisplayName("Should update max DPD when current DPD is higher")
    void shouldUpdateMaxDpdWhenHigher() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        loan.markOverdue(30);

        // When
        loan.markOverdue(45);

        // Then
        assertThat(loan.getCurrentDPD()).isEqualTo(45);
        assertThat(loan.getMaxDPD()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should not update max DPD when current DPD is lower")
    void shouldNotUpdateMaxDpdWhenLower() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        loan.markOverdue(60);
        loan.markActive();

        // When
        loan.markOverdue(30);

        // Then
        assertThat(loan.getCurrentDPD()).isEqualTo(30);
        assertThat(loan.getMaxDPD()).isEqualTo(60); // Max DPD remains at highest value
    }

    @Test
    @DisplayName("Should settle loan and set outstanding amounts to zero")
    void shouldSettleLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        LocalDate settlementDate = bookingDate.plusMonths(12);
        loan.clearDomainEvents();

        // When
        loan.settle(settlementDate);

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.SETTLED);
        assertThat(loan.getOutstandingPrincipal()).isEqualTo(SarMoney.zero());
        assertThat(loan.getOutstandingProfit()).isEqualTo(SarMoney.zero());
        assertThat(loan.getCurrentDPD()).isZero();

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanSettledEvent.class);
    }

    @Test
    @DisplayName("Should throw exception when settling loan not in active status")
    void shouldThrowExceptionWhenSettlingNonActiveLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        LocalDate settlementDate = bookingDate.plusMonths(12);

        // When/Then
        assertThatThrownBy(() -> loan.settle(settlementDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot settle loan in status PENDING_DISBURSEMENT");
    }

    @Test
    @DisplayName("Should write off loan and transition to WRITTEN_OFF")
    void shouldWriteOffLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        loan.clearDomainEvents();

        // When
        loan.writeOff();

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.WRITTEN_OFF);

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanWrittenOffEvent.class);

        Loan.LoanWrittenOffEvent event = (Loan.LoanWrittenOffEvent) events.get(0);
        assertThat(event.outstandingPrincipal()).isEqualTo(loan.getOutstandingPrincipal());
        assertThat(event.outstandingProfit()).isEqualTo(loan.getOutstandingProfit());
    }

    @Test
    @DisplayName("Should record payment and update outstanding balances")
    void shouldRecordPaymentAndUpdateOutstanding() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        SarMoney principalPayment = SarMoney.of(8333.33);
        SarMoney profitPayment = SarMoney.of(416.67);
        loan.clearDomainEvents();

        // When
        loan.recordPayment(principalPayment, profitPayment);

        // Then
        assertThat(loan.getOutstandingPrincipal()).isEqualTo(SarMoney.of(91666.67));
        assertThat(loan.getOutstandingProfit()).isEqualTo(SarMoney.of(4583.33));

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.PaymentRecordedEvent.class);
    }

    @Test
    @DisplayName("Should throw exception when payment causes negative outstanding principal")
    void shouldThrowExceptionWhenPaymentCausesNegativeOutstandingPrincipal() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        SarMoney excessivePayment = SarMoney.of(150000); // More than outstanding

        // When/Then
        assertThatThrownBy(() -> loan.recordPayment(excessivePayment, SarMoney.zero()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outstanding amounts cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception when payment causes negative outstanding profit")
    void shouldThrowExceptionWhenPaymentCausesNegativeOutstandingProfit() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        SarMoney excessiveProfit = SarMoney.of(10000); // More than outstanding profit

        // When/Then
        assertThatThrownBy(() -> loan.recordPayment(SarMoney.zero(), excessiveProfit))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Outstanding amounts cannot be negative");
    }

    @Test
    @DisplayName("Should throw exception when recording payment on non-active loan")
    void shouldThrowExceptionWhenRecordingPaymentOnNonActiveLoan() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // When/Then
        assertThatThrownBy(() -> loan.recordPayment(SarMoney.of(1000), SarMoney.of(100)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Cannot record payment for loan in status PENDING_DISBURSEMENT");
    }

    @Test
    @DisplayName("Should clear domain events")
    void shouldClearDomainEvents() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        assertThat(loan.getDomainEvents()).isNotEmpty();

        // When
        loan.clearDomainEvents();

        // Then
        assertThat(loan.getDomainEvents()).isEmpty();
    }

    @Test
    @DisplayName("Should return unmodifiable list of domain events")
    void shouldReturnUnmodifiableListOfDomainEvents() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // When
        List<DomainEvent> events = loan.getDomainEvents();

        // Then
        assertThatThrownBy(() -> events.clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should throw exception when creating loan with null principal")
    void shouldThrowExceptionWhenCreatingLoanWithNullPrincipal() {
        // When/Then
        assertThatThrownBy(() -> Loan.createNew(contractId, customerId, productId, null, profitRate, tenure, shariaStructure, bookingDate))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("Principal cannot be null");
    }

    @Test
    @DisplayName("Should throw exception when creating loan with negative principal")
    void shouldThrowExceptionWhenCreatingLoanWithNegativePrincipal() {
        // Given
        SarMoney negativePrincipal = SarMoney.of(-100000);

        // When/Then
        assertThatThrownBy(() -> Loan.createNew(contractId, customerId, productId, negativePrincipal, profitRate, tenure, shariaStructure, bookingDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal must be positive");
    }

    @Test
    @DisplayName("Should throw exception when creating loan with zero principal")
    void shouldThrowExceptionWhenCreatingLoanWithZeroPrincipal() {
        // Given
        SarMoney zeroPrincipal = SarMoney.zero();

        // When/Then
        assertThatThrownBy(() -> Loan.createNew(contractId, customerId, productId, zeroPrincipal, profitRate, tenure, shariaStructure, bookingDate))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal must be positive");
    }

    @Test
    @DisplayName("Should throw exception when marking overdue with negative DPD")
    void shouldThrowExceptionWhenMarkingOverdueWithNegativeDpd() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));

        // When/Then
        assertThatThrownBy(() -> loan.markOverdue(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DPD cannot be negative");
    }

    @Test
    @DisplayName("Should mark loan active and reset current DPD")
    void shouldMarkLoanActiveAndResetCurrentDpd() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));
        loan.markOverdue(30);
        loan.clearDomainEvents();

        // When
        loan.markActive();

        // Then
        assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
        assertThat(loan.getCurrentDPD()).isZero();
        assertThat(loan.getMaxDPD()).isEqualTo(30); // Max DPD preserved

        List<DomainEvent> events = loan.getDomainEvents();
        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(Loan.LoanMarkedActiveEvent.class);
    }

    @Test
    @DisplayName("Should allow multiple payments reducing outstanding to zero")
    void shouldAllowMultiplePaymentsReducingOutstandingToZero() {
        // Given
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);
        loan.disburse(bookingDate.plusDays(1));

        // When - Make 12 monthly payments
        for (int i = 0; i < 12; i++) {
            loan.recordPayment(SarMoney.of(8333.33), SarMoney.of(416.67));
        }

        // Then - Outstanding should be approximately zero (with rounding)
        assertThat(loan.getOutstandingPrincipal().getValue()).isLessThan(SarMoney.of(1).getValue());
        assertThat(loan.getOutstandingProfit().getValue()).isLessThan(SarMoney.of(1).getValue());
    }

    @Test
    @DisplayName("Should maintain all loan attributes correctly")
    void shouldMaintainAllLoanAttributesCorrectly() {
        // Given/When
        Loan loan = Loan.createNew(contractId, customerId, productId, principal, profitRate, tenure, shariaStructure, bookingDate);

        // Then
        assertThat(loan.getLoanId()).isNotNull();
        assertThat(loan.getContractId()).isEqualTo(contractId);
        assertThat(loan.getCustomerId()).isEqualTo(customerId);
        assertThat(loan.getProductId()).isEqualTo(productId);
        assertThat(loan.getPrincipal()).isEqualTo(principal);
        assertThat(loan.getProfitRate()).isEqualTo(profitRate);
        assertThat(loan.getProfitAmount()).isNotNull();
        assertThat(loan.getTotalAmount()).isNotNull();
        assertThat(loan.getTenure()).isEqualTo(tenure);
        assertThat(loan.getShariaStructure()).isEqualTo(shariaStructure);
        assertThat(loan.getBookingDate()).isEqualTo(bookingDate);
        assertThat(loan.getDisbursementDate()).isNull();
        assertThat(loan.getMaturityDate()).isNull();
    }
}
