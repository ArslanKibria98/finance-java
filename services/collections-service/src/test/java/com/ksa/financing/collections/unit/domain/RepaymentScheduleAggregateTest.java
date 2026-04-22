package com.ksa.financing.collections.unit.domain;

import com.ksa.financing.collections.domain.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("RepaymentScheduleAggregate")
class RepaymentScheduleAggregateTest {

    private UUID tenantId;
    private UUID loanId;
    private UUID createdBy;
    private List<Installment> installments;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        loanId = UUID.randomUUID();
        createdBy = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();

        installments = List.of(
                Installment.create(tenantId, scheduleId, loanId, 1, LocalDate.now().plusMonths(1),
                        new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20")),
                Installment.create(tenantId, scheduleId, loanId, 2, LocalDate.now().plusMonths(2),
                        new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20")),
                Installment.create(tenantId, scheduleId, loanId, 3, LocalDate.now().plusMonths(3),
                        new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20"))
        );
    }

    @Nested
    @DisplayName("Schedule creation")
    class Creation {

        @Test
        @DisplayName("creates schedule with correct totals")
        void createsScheduleWithCorrectTotals() {
            var schedule = RepaymentScheduleAggregate.create(
                    tenantId, "SCH-001", loanId,
                    new BigDecimal("3000"), new BigDecimal("150"),
                    LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                    installments, createdBy);

            assertThat(schedule.getTotalPrincipal()).isEqualByComparingTo("3000");
            assertThat(schedule.getTotalProfit()).isEqualByComparingTo("150");
            assertThat(schedule.getTotalAmount()).isEqualByComparingTo("3150");
            assertThat(schedule.getTotalInstallments()).isEqualTo(3);
            assertThat(schedule.isActive()).isTrue();
            assertThat(schedule.isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("registers ScheduleCreated event on creation")
        void registersCreatedEvent() {
            var schedule = createTestSchedule();

            assertThat(schedule.getUncommittedEvents()).hasSize(1);
            assertThat(schedule.getUncommittedEvents().get(0))
                    .isInstanceOf(RepaymentScheduleAggregate.ScheduleCreated.class);
        }

        @Test
        @DisplayName("throws when tenant ID is null")
        void throwsWhenTenantIdNull() {
            assertThatThrownBy(() -> RepaymentScheduleAggregate.create(
                    null, "SCH-001", loanId,
                    new BigDecimal("3000"), new BigDecimal("150"),
                    LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                    installments, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Tenant ID");
        }

        @Test
        @DisplayName("throws when schedule number is blank")
        void throwsWhenScheduleNumberBlank() {
            assertThatThrownBy(() -> RepaymentScheduleAggregate.create(
                    tenantId, "", loanId,
                    new BigDecimal("3000"), new BigDecimal("150"),
                    LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                    installments, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Schedule number");
        }

        @Test
        @DisplayName("throws when principal is zero")
        void throwsWhenPrincipalZero() {
            assertThatThrownBy(() -> RepaymentScheduleAggregate.create(
                    tenantId, "SCH-001", loanId,
                    BigDecimal.ZERO, new BigDecimal("150"),
                    LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                    installments, createdBy))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("principal");
        }
    }

    @Nested
    @DisplayName("Payment waterfall allocation")
    class PaymentWaterfall {

        @Test
        @DisplayName("allocates fees first, then profit, then principal")
        void allocatesInCorrectOrder() {
            var schedule = createTestSchedule();
            var paymentId = UUID.randomUUID();

            // Each installment: principal=1000, profit=50, fee=20 → total=1070
            // Pay enough for one full installment's fees and profit only (20+50=70)
            var allocations = schedule.applyPayment(paymentId, new BigDecimal("70"));

            assertThat(allocations).isNotEmpty();
            // First allocation should be fee
            var feeAlloc = allocations.stream()
                    .filter(a -> a.feeAllocated().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst();
            assertThat(feeAlloc).isPresent();
            assertThat(feeAlloc.get().feeAllocated()).isEqualByComparingTo("20");

            // Then profit
            var profitAlloc = allocations.stream()
                    .filter(a -> a.profitAllocated().compareTo(BigDecimal.ZERO) > 0)
                    .findFirst();
            assertThat(profitAlloc).isPresent();
            assertThat(profitAlloc.get().profitAllocated()).isEqualByComparingTo("50");
        }

        @Test
        @DisplayName("fully pays one installment and marks it PAID")
        void fullyPaysOneInstallment() {
            var schedule = createTestSchedule();
            var paymentId = UUID.randomUUID();

            // Total for one installment: 1000 + 50 + 20 = 1070
            schedule.applyPayment(paymentId, new BigDecimal("1070"));

            var paidInstallments = schedule.getInstallments().stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PAID)
                    .toList();
            assertThat(paidInstallments).hasSize(1);
        }

        @Test
        @DisplayName("marks schedule as fully paid when all installments paid")
        void marksFullyPaidWhenAllPaid() {
            var schedule = createTestSchedule();
            var paymentId = UUID.randomUUID();

            // Total for all: 3 * (1000+50+20) = 3210
            schedule.applyPayment(paymentId, new BigDecimal("3210"));

            assertThat(schedule.isFullyPaid()).isTrue();
        }

        @Test
        @DisplayName("fires ScheduleFullyPaid event when all installments paid")
        void firesFullyPaidEvent() {
            var schedule = createTestSchedule();
            var paymentId = UUID.randomUUID();

            schedule.applyPayment(paymentId, new BigDecimal("3210"));

            var events = schedule.getUncommittedEvents();
            boolean hasFullyPaidEvent = events.stream()
                    .anyMatch(e -> e instanceof RepaymentScheduleAggregate.ScheduleFullyPaid);
            assertThat(hasFullyPaidEvent).isTrue();
        }

        @Test
        @DisplayName("handles partial payment correctly")
        void handlesPartialPayment() {
            var schedule = createTestSchedule();
            var paymentId = UUID.randomUUID();

            // Pay only 500 (less than one full installment)
            schedule.applyPayment(paymentId, new BigDecimal("500"));

            var partialInstallments = schedule.getInstallments().stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.PARTIALLY_PAID)
                    .toList();
            assertThat(partialInstallments).hasSize(1);
            assertThat(schedule.isFullyPaid()).isFalse();
        }

        @Test
        @DisplayName("throws when payment amount is zero")
        void throwsWhenAmountZero() {
            var schedule = createTestSchedule();
            assertThatThrownBy(() -> schedule.applyPayment(UUID.randomUUID(), BigDecimal.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when payment amount is negative")
        void throwsWhenAmountNegative() {
            var schedule = createTestSchedule();
            assertThatThrownBy(() -> schedule.applyPayment(UUID.randomUUID(), new BigDecimal("-100")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("throws when schedule is inactive")
        void throwsWhenScheduleInactive() {
            var schedule = createTestSchedule();
            schedule.deactivate();
            assertThatThrownBy(() -> schedule.applyPayment(UUID.randomUUID(), new BigDecimal("100")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inactive");
        }
    }

    @Nested
    @DisplayName("Outstanding calculations")
    class OutstandingCalculations {

        @Test
        @DisplayName("calculates total outstanding amount correctly")
        void calculatesTotalOutstanding() {
            var schedule = createTestSchedule();

            // Before any payment: total = 3 * (1000+50+20) = 3210
            assertThat(schedule.getTotalOutstanding()).isEqualByComparingTo("3210");
        }

        @Test
        @DisplayName("reduces outstanding after payment")
        void reducesOutstandingAfterPayment() {
            var schedule = createTestSchedule();
            schedule.applyPayment(UUID.randomUUID(), new BigDecimal("500"));

            assertThat(schedule.getTotalOutstanding()).isEqualByComparingTo("2710");
        }

        @Test
        @DisplayName("getPaidTotal reflects actual payments")
        void paidTotalReflectsPayments() {
            var schedule = createTestSchedule();
            schedule.applyPayment(UUID.randomUUID(), new BigDecimal("500"));

            assertThat(schedule.getPaidTotal()).isEqualByComparingTo("500");
        }
    }

    @Nested
    @DisplayName("Event management")
    class EventManagement {

        @Test
        @DisplayName("clears events after markEventsAsCommitted")
        void clearsEventsAfterCommit() {
            var schedule = createTestSchedule();
            assertThat(schedule.getUncommittedEvents()).isNotEmpty();

            schedule.markEventsAsCommitted();

            assertThat(schedule.getUncommittedEvents()).isEmpty();
        }

        @Test
        @DisplayName("fires PaymentApplied event after applying payment")
        void firesPaymentAppliedEvent() {
            var schedule = createTestSchedule();
            schedule.markEventsAsCommitted();

            var paymentId = UUID.randomUUID();
            schedule.applyPayment(paymentId, new BigDecimal("100"));

            var events = schedule.getUncommittedEvents();
            assertThat(events).anyMatch(e -> e instanceof RepaymentScheduleAggregate.PaymentApplied);
        }
    }

    @Nested
    @DisplayName("Due date transitions")
    class DueDateTransitions {

        @Test
        @DisplayName("marks SCHEDULED installments as DUE when due date passed")
        void marksInstallmentsAsDue() {
            UUID scheduleId = UUID.randomUUID();
            var pastDueInstallments = List.of(
                    Installment.create(tenantId, scheduleId, loanId, 1,
                            LocalDate.now().minusDays(1),
                            new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20")),
                    Installment.create(tenantId, scheduleId, loanId, 2,
                            LocalDate.now().plusMonths(1),
                            new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20"))
            );

            var schedule = RepaymentScheduleAggregate.create(
                    tenantId, "SCH-DUE-001", loanId,
                    new BigDecimal("2000"), new BigDecimal("100"),
                    LocalDate.now().minusDays(1), LocalDate.now().plusMonths(1),
                    pastDueInstallments, createdBy);

            schedule.markInstallmentsDue(LocalDate.now());

            var dueInstallments = schedule.getInstallments().stream()
                    .filter(i -> i.getStatus() == InstallmentStatus.DUE)
                    .toList();
            assertThat(dueInstallments).hasSize(1);
        }
    }

    // ==================== HELPERS ====================

    private RepaymentScheduleAggregate createTestSchedule() {
        return RepaymentScheduleAggregate.create(
                tenantId, "SCH-TEST-001", loanId,
                new BigDecimal("3000"), new BigDecimal("150"),
                LocalDate.now().plusMonths(1), LocalDate.now().plusMonths(3),
                installments, createdBy);
    }
}
