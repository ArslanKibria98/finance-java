package com.ksa.islamic.finance.test.assertion;

import com.ksa.islamic.finance.domain.model.*;
import com.ksa.islamic.finance.domain.model.payment.Payment;
import com.ksa.islamic.finance.domain.model.payment.PaymentStatus;
import com.ksa.islamic.finance.event.domain.DomainEvent;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Currency;

/**
 * Custom AssertJ assertions for Islamic finance domain
 */
public class IslamicFinanceAssertions {

    /**
     * Entry point for Loan assertions
     */
    public static LoanAssert assertThat(Loan actual) {
        return new LoanAssert(actual);
    }

    /**
     * Entry point for Customer assertions
     */
    public static CustomerAssert assertThat(Customer actual) {
        return new CustomerAssert(actual);
    }

    /**
     * Entry point for Payment assertions
     */
    public static PaymentAssert assertThat(Payment actual) {
        return new PaymentAssert(actual);
    }

    /**
     * Entry point for Money assertions
     */
    public static MoneyAssert assertThat(Money actual) {
        return new MoneyAssert(actual);
    }

    /**
     * Entry point for DomainEvent assertions
     */
    public static DomainEventAssert assertThat(DomainEvent actual) {
        return new DomainEventAssert(actual);
    }

    /**
     * Loan assertions
     */
    public static class LoanAssert extends AbstractAssert<LoanAssert, Loan> {
        public LoanAssert(Loan actual) {
            super(actual, LoanAssert.class);
        }

        public LoanAssert hasStatus(LoanStatus status) {
            isNotNull();
            Assertions.assertThat(actual.getStatus())
                .overridingErrorMessage("Expected loan status to be <%s> but was <%s>",
                    status, actual.getStatus())
                .isEqualTo(status);
            return this;
        }

        public LoanAssert isActive() {
            return hasStatus(LoanStatus.ACTIVE);
        }

        public LoanAssert isPending() {
            return hasStatus(LoanStatus.PENDING);
        }

        public LoanAssert isDefaulted() {
            return hasStatus(LoanStatus.DEFAULTED);
        }

        public LoanAssert hasProductType(ProductType productType) {
            isNotNull();
            Assertions.assertThat(actual.getProductType())
                .overridingErrorMessage("Expected product type to be <%s> but was <%s>",
                    productType, actual.getProductType())
                .isEqualTo(productType);
            return this;
        }

        public LoanAssert hasOutstandingBalance(Money expectedBalance) {
            isNotNull();
            Assertions.assertThat(actual.getOutstandingBalance())
                .overridingErrorMessage("Expected outstanding balance to be <%s> but was <%s>",
                    expectedBalance, actual.getOutstandingBalance())
                .isEqualTo(expectedBalance);
            return this;
        }

        public LoanAssert hasPositiveBalance() {
            isNotNull();
            Assertions.assertThat(actual.getOutstandingBalance().getAmount())
                .overridingErrorMessage("Expected positive balance but was <%s>",
                    actual.getOutstandingBalance())
                .isGreaterThan(BigDecimal.ZERO);
            return this;
        }

        public LoanAssert isFullyPaid() {
            isNotNull();
            Assertions.assertThat(actual.getOutstandingBalance().getAmount())
                .overridingErrorMessage("Expected loan to be fully paid but has balance <%s>",
                    actual.getOutstandingBalance())
                .isEqualTo(BigDecimal.ZERO);
            return this;
        }

        public LoanAssert hasProfitRate(BigDecimal rate) {
            isNotNull();
            Assertions.assertThat(actual.getProfitRate())
                .overridingErrorMessage("Expected profit rate to be <%s> but was <%s>",
                    rate, actual.getProfitRate())
                .isEqualTo(rate);
            return this;
        }

        public LoanAssert isDisbursedBefore(LocalDate date) {
            isNotNull();
            Assertions.assertThat(actual.getDisbursementDate())
                .overridingErrorMessage("Expected disbursement date to be before <%s> but was <%s>",
                    date, actual.getDisbursementDate())
                .isBefore(date);
            return this;
        }

        public LoanAssert maturesAfter(LocalDate date) {
            isNotNull();
            Assertions.assertThat(actual.getMaturityDate())
                .overridingErrorMessage("Expected maturity date to be after <%s> but was <%s>",
                    date, actual.getMaturityDate())
                .isAfter(date);
            return this;
        }
    }

    /**
     * Customer assertions
     */
    public static class CustomerAssert extends AbstractAssert<CustomerAssert, Customer> {
        public CustomerAssert(Customer actual) {
            super(actual, CustomerAssert.class);
        }

        public CustomerAssert hasStatus(CustomerStatus status) {
            isNotNull();
            Assertions.assertThat(actual.getStatus())
                .overridingErrorMessage("Expected customer status to be <%s> but was <%s>",
                    status, actual.getStatus())
                .isEqualTo(status);
            return this;
        }

        public CustomerAssert isActive() {
            return hasStatus(CustomerStatus.ACTIVE);
        }

        public CustomerAssert isKycVerified() {
            isNotNull();
            Assertions.assertThat(actual.isKycVerified())
                .overridingErrorMessage("Expected customer to be KYC verified but was not")
                .isTrue();
            return this;
        }

        public CustomerAssert hasEmail(String email) {
            isNotNull();
            Assertions.assertThat(actual.getEmail())
                .overridingErrorMessage("Expected email to be <%s> but was <%s>",
                    email, actual.getEmail())
                .isEqualTo(email);
            return this;
        }

        public CustomerAssert hasFullName(String firstName, String lastName) {
            isNotNull();
            Assertions.assertThat(actual.getFirstName())
                .overridingErrorMessage("Expected first name to be <%s> but was <%s>",
                    firstName, actual.getFirstName())
                .isEqualTo(firstName);
            Assertions.assertThat(actual.getLastName())
                .overridingErrorMessage("Expected last name to be <%s> but was <%s>",
                    lastName, actual.getLastName())
                .isEqualTo(lastName);
            return this;
        }

        public CustomerAssert isAdult() {
            isNotNull();
            LocalDate eighteenYearsAgo = LocalDate.now().minusYears(18);
            Assertions.assertThat(actual.getDateOfBirth())
                .overridingErrorMessage("Expected customer to be adult (DOB before %s) but was born on %s",
                    eighteenYearsAgo, actual.getDateOfBirth())
                .isBefore(eighteenYearsAgo);
            return this;
        }
    }

    /**
     * Payment assertions
     */
    public static class PaymentAssert extends AbstractAssert<PaymentAssert, Payment> {
        public PaymentAssert(Payment actual) {
            super(actual, PaymentAssert.class);
        }

        public PaymentAssert hasStatus(PaymentStatus status) {
            isNotNull();
            Assertions.assertThat(actual.getStatus())
                .overridingErrorMessage("Expected payment status to be <%s> but was <%s>",
                    status, actual.getStatus())
                .isEqualTo(status);
            return this;
        }

        public PaymentAssert isCompleted() {
            return hasStatus(PaymentStatus.COMPLETED);
        }

        public PaymentAssert isPending() {
            return hasStatus(PaymentStatus.PENDING);
        }

        public PaymentAssert isFailed() {
            return hasStatus(PaymentStatus.FAILED);
        }

        public PaymentAssert hasAmount(Money amount) {
            isNotNull();
            Assertions.assertThat(actual.getAmount())
                .overridingErrorMessage("Expected payment amount to be <%s> but was <%s>",
                    amount, actual.getAmount())
                .isEqualTo(amount);
            return this;
        }

        public PaymentAssert isOnTime() {
            isNotNull();
            Assertions.assertThat(actual.getPaymentDate())
                .overridingErrorMessage("Expected payment to be on time (before %s) but was paid on %s",
                    actual.getDueDate(), actual.getPaymentDate())
                .isBeforeOrEqualTo(actual.getDueDate().atTime(23, 59, 59));
            return this;
        }

        public PaymentAssert isLate() {
            isNotNull();
            Assertions.assertThat(actual.getPaymentDate())
                .overridingErrorMessage("Expected payment to be late (after %s) but was paid on %s",
                    actual.getDueDate(), actual.getPaymentDate())
                .isAfter(actual.getDueDate().atTime(23, 59, 59));
            return this;
        }

        public PaymentAssert hasReferenceNumber(String referenceNumber) {
            isNotNull();
            Assertions.assertThat(actual.getReferenceNumber())
                .overridingErrorMessage("Expected reference number to be <%s> but was <%s>",
                    referenceNumber, actual.getReferenceNumber())
                .isEqualTo(referenceNumber);
            return this;
        }
    }

    /**
     * Money assertions
     */
    public static class MoneyAssert extends AbstractAssert<MoneyAssert, Money> {
        public MoneyAssert(Money actual) {
            super(actual, MoneyAssert.class);
        }

        public MoneyAssert hasAmount(BigDecimal amount) {
            isNotNull();
            Assertions.assertThat(actual.getAmount())
                .overridingErrorMessage("Expected amount to be <%s> but was <%s>",
                    amount, actual.getAmount())
                .isEqualTo(amount);
            return this;
        }

        public MoneyAssert hasCurrency(Currency currency) {
            isNotNull();
            Assertions.assertThat(actual.getCurrency())
                .overridingErrorMessage("Expected currency to be <%s> but was <%s>",
                    currency, actual.getCurrency())
                .isEqualTo(currency);
            return this;
        }

        public MoneyAssert hasCurrency(String currencyCode) {
            return hasCurrency(Currency.getInstance(currencyCode));
        }

        public MoneyAssert isPositive() {
            isNotNull();
            Assertions.assertThat(actual.getAmount())
                .overridingErrorMessage("Expected positive amount but was <%s>",
                    actual.getAmount())
                .isGreaterThan(BigDecimal.ZERO);
            return this;
        }

        public MoneyAssert isZero() {
            isNotNull();
            Assertions.assertThat(actual.getAmount())
                .overridingErrorMessage("Expected zero amount but was <%s>",
                    actual.getAmount())
                .isEqualTo(BigDecimal.ZERO);
            return this;
        }

        public MoneyAssert isGreaterThan(Money other) {
            isNotNull();
            Assertions.assertThat(actual.compareTo(other))
                .overridingErrorMessage("Expected <%s> to be greater than <%s>",
                    actual, other)
                .isGreaterThan(0);
            return this;
        }

        public MoneyAssert isLessThan(Money other) {
            isNotNull();
            Assertions.assertThat(actual.compareTo(other))
                .overridingErrorMessage("Expected <%s> to be less than <%s>",
                    actual, other)
                .isLessThan(0);
            return this;
        }

        public MoneyAssert isEqualTo(Money other) {
            isNotNull();
            Assertions.assertThat(actual)
                .overridingErrorMessage("Expected <%s> to be equal to <%s>",
                    actual, other)
                .isEqualTo(other);
            return this;
        }
    }

    /**
     * Domain Event assertions
     */
    public static class DomainEventAssert extends AbstractAssert<DomainEventAssert, DomainEvent> {
        public DomainEventAssert(DomainEvent actual) {
            super(actual, DomainEventAssert.class);
        }

        public DomainEventAssert hasEventType(String eventType) {
            isNotNull();
            Assertions.assertThat(actual.getEventType())
                .overridingErrorMessage("Expected event type to be <%s> but was <%s>",
                    eventType, actual.getEventType())
                .isEqualTo(eventType);
            return this;
        }

        public DomainEventAssert hasAggregateId(String aggregateId) {
            isNotNull();
            Assertions.assertThat(actual.getAggregateId())
                .overridingErrorMessage("Expected aggregate ID to be <%s> but was <%s>",
                    aggregateId, actual.getAggregateId())
                .isEqualTo(aggregateId);
            return this;
        }

        public DomainEventAssert occurredAfter(LocalDateTime dateTime) {
            isNotNull();
            Assertions.assertThat(actual.getOccurredAt())
                .overridingErrorMessage("Expected event to occur after <%s> but occurred at <%s>",
                    dateTime, actual.getOccurredAt())
                .isAfter(dateTime);
            return this;
        }

        public DomainEventAssert occurredBefore(LocalDateTime dateTime) {
            isNotNull();
            Assertions.assertThat(actual.getOccurredAt())
                .overridingErrorMessage("Expected event to occur before <%s> but occurred at <%s>",
                    dateTime, actual.getOccurredAt())
                .isBefore(dateTime);
            return this;
        }
    }
}