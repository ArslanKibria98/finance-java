package com.ksa.financing.collections.unit.domain;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Installment")
class InstallmentTest {

    @Test
    @DisplayName("creates installment with correct outstanding amount")
    void createsWithCorrectOutstanding() {
        var inst = makeInstallment("1000", "50", "20");

        assertThat(inst.getTotalAmount()).isEqualByComparingTo("1070");
        assertThat(inst.getOutstandingAmount()).isEqualByComparingTo("1070");
        assertThat(inst.getPaidTotal()).isEqualByComparingTo("0");
        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.SCHEDULED);
    }

    @Test
    @DisplayName("applyAllocation updates paid amounts and marks PARTIALLY_PAID")
    void appliesPartialAllocation() {
        var inst = makeInstallment("1000", "50", "20");

        inst.applyAllocation(new BigDecimal("20"), new BigDecimal("50"), BigDecimal.ZERO);

        assertThat(inst.getPaidFee()).isEqualByComparingTo("20");
        assertThat(inst.getPaidProfit()).isEqualByComparingTo("50");
        assertThat(inst.getPaidPrincipal()).isEqualByComparingTo("0");
        assertThat(inst.getPaidTotal()).isEqualByComparingTo("70");
        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.PARTIALLY_PAID);
    }

    @Test
    @DisplayName("marks PAID when fully allocated")
    void marksFullyPaid() {
        var inst = makeInstallment("1000", "50", "20");

        inst.applyAllocation(new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("1000"));

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(inst.getOutstandingAmount()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("throws when applying to PAID installment")
    void throwsWhenApplyingToPaid() {
        var inst = makeInstallment("1000", "50", "20");
        inst.applyAllocation(new BigDecimal("20"), new BigDecimal("50"), new BigDecimal("1000"));

        assertThatThrownBy(() -> inst.applyAllocation(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ONE))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("markDue transitions SCHEDULED to DUE")
    void markDueTransition() {
        var inst = makeInstallment("1000", "50", "20");

        inst.markDue();

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.DUE);
    }

    @Test
    @DisplayName("markOverdue sets DPD")
    void markOverdueSetsDpd() {
        var inst = makeInstallment("1000", "50", "20");
        inst.markDue();
        inst.markOverdue(5);

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.OVERDUE);
        assertThat(inst.getDpd()).isEqualTo(5);
    }

    @Test
    @DisplayName("waive sets status to WAIVED")
    void waiveInstallment() {
        var inst = makeInstallment("1000", "50", "20");

        inst.waive();

        assertThat(inst.getStatus()).isEqualTo(InstallmentStatus.WAIVED);
    }

    @Test
    @DisplayName("throws when creating installment with negative principal")
    void throwsNegativePrincipal() {
        assertThatThrownBy(() -> makeInstallment("-100", "50", "20"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Principal");
    }

    @Test
    @DisplayName("throws when creating installment with null due date")
    void throwsNullDueDate() {
        assertThatThrownBy(() -> Installment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, null, new BigDecimal("1000"), new BigDecimal("50"), new BigDecimal("20")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("due date");
    }

    @Test
    @DisplayName("outstanding fee, profit, principal calculated correctly")
    void outstandingAmountsCorrect() {
        var inst = makeInstallment("1000", "50", "20");
        inst.applyAllocation(new BigDecimal("20"), BigDecimal.ZERO, BigDecimal.ZERO);

        assertThat(inst.getOutstandingFee()).isEqualByComparingTo("0");
        assertThat(inst.getOutstandingProfit()).isEqualByComparingTo("50");
        assertThat(inst.getOutstandingPrincipal()).isEqualByComparingTo("1000");
    }

    private Installment makeInstallment(String principal, String profit, String fee) {
        return Installment.create(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                1, LocalDate.now().plusMonths(1),
                new BigDecimal(principal),
                new BigDecimal(profit),
                new BigDecimal(fee));
    }
}
