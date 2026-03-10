package com.ksa.financing.lending.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoanAggregate")
class LoanAggregateTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final LoanApplicationId APP_ID = LoanApplicationId.generate();

    private LoanAggregate createLoan() {
        return LoanAggregate.create(
                TENANT_ID, "LN-00000001", APP_ID, CUSTOMER_ID,
                PRODUCT_ID, "MURABAHA_PERSONAL", ShariaStructure.MURABAHA,
                new BigDecimal("100000"), new BigDecimal("25000"),
                new BigDecimal("0.05"), 60, new BigDecimal("2083.33")
        );
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("should create loan in PENDING_DISBURSEMENT status")
        void shouldCreatePendingDisbursement() {
            var loan = createLoan();

            assertThat(loan.getId()).isNotNull();
            assertThat(loan.getStatus()).isEqualTo(LoanStatus.PENDING_DISBURSEMENT);
            assertThat(loan.getPrincipalAmount()).isEqualByComparingTo("100000");
            assertThat(loan.getProfitAmount()).isEqualByComparingTo("25000");
            assertThat(loan.getTotalAmount()).isEqualByComparingTo("125000");
            assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("100000");
            assertThat(loan.getTotalOutstanding()).isEqualByComparingTo("125000");
            assertThat(loan.getIfrs9Stage()).isEqualTo(1);
        }

        @Test
        @DisplayName("should emit LoanCreated event")
        void shouldEmitCreatedEvent() {
            var loan = createLoan();
            assertThat(loan.getUncommittedEvents()).hasSize(1);
            assertThat(loan.getUncommittedEvents().get(0))
                    .isInstanceOf(LoanAggregate.LoanCreated.class);
        }

        @Test
        @DisplayName("should reject null tenant ID")
        void shouldRejectNullTenant() {
            assertThatThrownBy(() -> LoanAggregate.create(
                    null, "LN-001", APP_ID, CUSTOMER_ID, PRODUCT_ID,
                    "MURABAHA", ShariaStructure.MURABAHA,
                    new BigDecimal("100000"), new BigDecimal("25000"),
                    new BigDecimal("0.05"), 60, new BigDecimal("2083")
            )).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("should reject negative profit amount")
        void shouldRejectNegativeProfit() {
            assertThatThrownBy(() -> LoanAggregate.create(
                    TENANT_ID, "LN-001", APP_ID, CUSTOMER_ID, PRODUCT_ID,
                    "MURABAHA", ShariaStructure.MURABAHA,
                    new BigDecimal("100000"), new BigDecimal("-1"),
                    new BigDecimal("0.05"), 60, new BigDecimal("2083")
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Profit amount cannot be negative");
        }
    }

    @Nested
    @DisplayName("Disbursement")
    class Disbursement {

        @Test
        @DisplayName("should disburse loan and transition to ACTIVE")
        void shouldDisburse() {
            var loan = createLoan();
            var disbDate = LocalDate.now();
            var firstDue = disbDate.plusMonths(1);
            var maturity = disbDate.plusMonths(60);

            loan.disburse(disbDate, firstDue, maturity);

            assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(loan.getDisbursementDate()).isEqualTo(disbDate);
            assertThat(loan.getFirstDueDate()).isEqualTo(firstDue);
            assertThat(loan.getMaturityDate()).isEqualTo(maturity);
            assertThat(loan.getUncommittedEvents()).hasSize(2); // Created + Disbursed
        }

        @Test
        @DisplayName("should reject disbursement of non-pending loan")
        void shouldRejectDisbursementOfActive() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));

            assertThatThrownBy(() -> loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING_DISBURSEMENT");
        }
    }

    @Nested
    @DisplayName("Delinquency and DPD")
    class Delinquency {

        @Test
        @DisplayName("should mark active loan as delinquent")
        void shouldMarkDelinquent() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));

            loan.markDelinquent(45);

            assertThat(loan.getStatus()).isEqualTo(LoanStatus.DELINQUENT);
            assertThat(loan.getCurrentDpd()).isEqualTo(45);
            assertThat(loan.getMaxDpd()).isEqualTo(45);
            assertThat(loan.getIfrs9Stage()).isEqualTo(2); // 31-90 = Stage 2
        }

        @Test
        @DisplayName("should set IFRS9 Stage 3 for DPD > 90")
        void shouldSetIfrs9Stage3() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));

            loan.markDelinquent(91);

            assertThat(loan.getIfrs9Stage()).isEqualTo(3);
        }

        @Test
        @DisplayName("should cure delinquency")
        void shouldCureDelinquency() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));
            loan.markDelinquent(15);

            loan.cureDelinquency();

            assertThat(loan.getStatus()).isEqualTo(LoanStatus.ACTIVE);
            assertThat(loan.getCurrentDpd()).isEqualTo(0);
            assertThat(loan.getMaxDpd()).isEqualTo(15); // Max DPD preserved
        }
    }

    @Nested
    @DisplayName("Settlement")
    class Settlement {

        @Test
        @DisplayName("should settle active loan and zero balances")
        void shouldSettleLoan() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));

            loan.settle(LocalDate.now());

            assertThat(loan.getStatus()).isEqualTo(LoanStatus.SETTLED);
            assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("0");
            assertThat(loan.getOutstandingProfit()).isEqualByComparingTo("0");
            assertThat(loan.getTotalOutstanding()).isEqualByComparingTo("0");
            assertThat(loan.getSettlementDate()).isNotNull();
        }

        @Test
        @DisplayName("should emit LoanSettled event")
        void shouldEmitSettledEvent() {
            var loan = createLoan();
            loan.disburse(LocalDate.now(), LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(60));
            loan.markEventsAsCommitted();

            loan.settle(LocalDate.now());

            assertThat(loan.getUncommittedEvents()).hasSize(1);
            assertThat(loan.getUncommittedEvents().get(0))
                    .isInstanceOf(LoanAggregate.LoanSettled.class);
        }
    }
}
