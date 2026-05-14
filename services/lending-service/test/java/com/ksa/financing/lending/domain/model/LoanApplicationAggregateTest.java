package com.ksa.financing.lending.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("LoanApplicationAggregate")
class LoanApplicationAggregateTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    private LoanApplicationAggregate createDraftApplication() {
        return LoanApplicationAggregate.create(
                TENANT_ID, "APP-00000001", CUSTOMER_ID, PRODUCT_ID,
                "MURABAHA_PERSONAL", ShariaStructure.MURABAHA,
                new BigDecimal("100000"), 60, USER_ID
        );
    }

    @Nested
    @DisplayName("Factory Method")
    class FactoryMethod {

        @Test
        @DisplayName("should create application in DRAFT status")
        void shouldCreateInDraft() {
            var app = createDraftApplication();

            assertThat(app.getId()).isNotNull();
            assertThat(app.getTenantId()).isEqualTo(TENANT_ID);
            assertThat(app.getApplicationNumber()).isEqualTo("APP-00000001");
            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
            assertThat(app.getRequestedAmount()).isEqualByComparingTo("100000");
            assertThat(app.getRequestedTenureMonths()).isEqualTo(60);
            assertThat(app.getShariaStructure()).isEqualTo(ShariaStructure.MURABAHA);
        }

        @Test
        @DisplayName("should emit LoanApplicationCreated event")
        void shouldEmitCreatedEvent() {
            var app = createDraftApplication();

            assertThat(app.getUncommittedEvents()).hasSize(1);
            assertThat(app.getUncommittedEvents().get(0))
                    .isInstanceOf(LoanApplicationAggregate.LoanApplicationCreated.class);
        }

        @Test
        @DisplayName("should reject null tenant ID")
        void shouldRejectNullTenant() {
            assertThatThrownBy(() -> LoanApplicationAggregate.create(
                    null, "APP-001", CUSTOMER_ID, PRODUCT_ID,
                    "MURABAHA", ShariaStructure.MURABAHA,
                    new BigDecimal("100000"), 60, USER_ID
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("Tenant ID");
        }

        @Test
        @DisplayName("should reject zero amount")
        void shouldRejectZeroAmount() {
            assertThatThrownBy(() -> LoanApplicationAggregate.create(
                    TENANT_ID, "APP-001", CUSTOMER_ID, PRODUCT_ID,
                    "MURABAHA", ShariaStructure.MURABAHA,
                    BigDecimal.ZERO, 60, USER_ID
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("amount must be positive");
        }

        @Test
        @DisplayName("should reject negative tenure")
        void shouldRejectNegativeTenure() {
            assertThatThrownBy(() -> LoanApplicationAggregate.create(
                    TENANT_ID, "APP-001", CUSTOMER_ID, PRODUCT_ID,
                    "MURABAHA", ShariaStructure.MURABAHA,
                    new BigDecimal("100000"), -1, USER_ID
            )).isInstanceOf(IllegalArgumentException.class)
              .hasMessageContaining("tenure must be positive");
        }
    }

    @Nested
    @DisplayName("State Transitions")
    class StateTransitions {

        @Test
        @DisplayName("should transition DRAFT -> SUBMITTED")
        void shouldSubmit() {
            var app = createDraftApplication();
            app.submit(USER_ID);

            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
            assertThat(app.getSubmittedAt()).isNotNull();
            assertThat(app.getUncommittedEvents()).hasSize(2); // Created + Submitted
        }

        @Test
        @DisplayName("should transition through full approval flow")
        void shouldApproveFullFlow() {
            var app = createDraftApplication();
            app.submit(USER_ID);
            app.moveToDocumentsPending(USER_ID);
            app.moveToUnderReview(USER_ID);
            app.moveToCreditCheck(USER_ID);
            app.moveToShariaValidation(USER_ID);
            app.moveToPendingApproval(USER_ID);
            app.approve(new BigDecimal("95000"), 60, new BigDecimal("0.05"),
                    new BigDecimal("23750"), new BigDecimal("118750"),
                    new BigDecimal("1979.17"), USER_ID);

            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
            assertThat(app.getApprovedAmount()).isEqualByComparingTo("95000");
            assertThat(app.getApprovedTenureMonths()).isEqualTo(60);
        }

        @Test
        @DisplayName("should reject invalid transition DRAFT -> APPROVED")
        void shouldRejectInvalidTransition() {
            var app = createDraftApplication();

            assertThatThrownBy(() -> app.approve(
                    new BigDecimal("100000"), 60, new BigDecimal("0.05"),
                    new BigDecimal("25000"), new BigDecimal("125000"),
                    new BigDecimal("2083.33"), USER_ID
            )).isInstanceOf(IllegalStateException.class)
              .hasMessageContaining("Cannot transition");
        }

        @Test
        @DisplayName("should allow cancellation from DRAFT")
        void shouldCancelFromDraft() {
            var app = createDraftApplication();
            app.cancel(USER_ID);
            assertThat(app.getStatus()).isEqualTo(ApplicationStatus.CANCELLED);
        }

        @Test
        @DisplayName("should record credit check result")
        void shouldRecordCreditCheck() {
            var app = createDraftApplication();
            app.submit(USER_ID);
            app.moveToDocumentsPending(USER_ID);
            app.moveToUnderReview(USER_ID);
            app.moveToCreditCheck(USER_ID);
            app.recordCreditCheckResult(new BigDecimal("0.35"), new BigDecimal("0.55"));

            assertThat(app.getDbrBefore()).isEqualByComparingTo("0.35");
            assertThat(app.getDbrAfter()).isEqualByComparingTo("0.55");
        }
    }

    @Nested
    @DisplayName("Event Management")
    class EventManagement {

        @Test
        @DisplayName("should clear events after commit")
        void shouldClearEventsAfterCommit() {
            var app = createDraftApplication();
            assertThat(app.getUncommittedEvents()).hasSize(1);

            app.markEventsAsCommitted();
            assertThat(app.getUncommittedEvents()).isEmpty();
        }

        @Test
        @DisplayName("should return unmodifiable events list")
        void shouldReturnUnmodifiableList() {
            var app = createDraftApplication();
            var events = app.getUncommittedEvents();

            assertThatThrownBy(() -> events.add(new Object()))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
