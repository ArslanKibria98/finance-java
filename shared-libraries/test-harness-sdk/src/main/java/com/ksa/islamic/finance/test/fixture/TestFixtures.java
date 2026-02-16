package com.ksa.islamic.finance.test.fixture;

import com.ksa.islamic.finance.domain.model.*;
import com.ksa.islamic.finance.domain.model.payment.Payment;
import com.ksa.islamic.finance.domain.model.payment.PaymentMethod;
import com.ksa.islamic.finance.domain.model.payment.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Test fixtures factory for domain objects
 */
public class TestFixtures {
    private static final AtomicLong idCounter = new AtomicLong(1000);
    private static final Random random = ThreadLocalRandom.current();

    private TestFixtures() {}

    /**
     * Customer fixtures
     */
    public static class Customers {
        public static Customer validCustomer() {
            String id = generateId();
            return Customer.builder()
                .id(UUID.randomUUID())
                .customerNumber("CUST-" + id)
                .firstName("Ahmad")
                .lastName("Al-Rahman")
                .email("ahmad.rahman@example.com")
                .phone("+966501234567")
                .nationalId("1234567890")
                .dateOfBirth(LocalDate.of(1985, 6, 15))
                .status(CustomerStatus.ACTIVE)
                .kycVerified(true)
                .createdAt(LocalDateTime.now())
                .build();
        }

        public static Customer pendingKycCustomer() {
            Customer customer = validCustomer();
            customer.setStatus(CustomerStatus.PENDING);
            customer.setKycVerified(false);
            return customer;
        }

        public static Customer blacklistedCustomer() {
            Customer customer = validCustomer();
            customer.setStatus(CustomerStatus.BLACKLISTED);
            customer.setFirstName("Blocked");
            customer.setLastName("User");
            return customer;
        }

        public static List<Customer> multipleCustomers(int count) {
            List<Customer> customers = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Customer customer = validCustomer();
                customer.setCustomerNumber("CUST-" + generateId());
                customer.setEmail("customer" + i + "@example.com");
                customers.add(customer);
            }
            return customers;
        }
    }

    /**
     * Loan fixtures
     */
    public static class Loans {
        public static Loan murabahaLoan() {
            return Loan.builder()
                .id(UUID.randomUUID())
                .loanNumber("LOAN-" + generateId())
                .customerId(UUID.randomUUID())
                .productType(ProductType.MURABAHA)
                .principalAmount(new Money(BigDecimal.valueOf(100000), Currency.getInstance("SAR")))
                .profitRate(BigDecimal.valueOf(0.035))
                .termMonths(36)
                .monthlyPayment(new Money(BigDecimal.valueOf(3200), Currency.getInstance("SAR")))
                .totalProfit(new Money(BigDecimal.valueOf(15200), Currency.getInstance("SAR")))
                .outstandingBalance(new Money(BigDecimal.valueOf(100000), Currency.getInstance("SAR")))
                .status(LoanStatus.ACTIVE)
                .disbursementDate(LocalDate.now().minusMonths(1))
                .maturityDate(LocalDate.now().plusMonths(35))
                .createdAt(LocalDateTime.now().minusMonths(1))
                .build();
        }

        public static Loan ijarahLoan() {
            return Loan.builder()
                .id(UUID.randomUUID())
                .loanNumber("LOAN-" + generateId())
                .customerId(UUID.randomUUID())
                .productType(ProductType.IJARAH)
                .principalAmount(new Money(BigDecimal.valueOf(500000), Currency.getInstance("SAR")))
                .profitRate(BigDecimal.valueOf(0.045))
                .termMonths(60)
                .monthlyPayment(new Money(BigDecimal.valueOf(9500), Currency.getInstance("SAR")))
                .totalProfit(new Money(BigDecimal.valueOf(70000), Currency.getInstance("SAR")))
                .outstandingBalance(new Money(BigDecimal.valueOf(500000), Currency.getInstance("SAR")))
                .status(LoanStatus.ACTIVE)
                .disbursementDate(LocalDate.now().minusMonths(6))
                .maturityDate(LocalDate.now().plusMonths(54))
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build();
        }

        public static Loan mudarabahLoan() {
            return Loan.builder()
                .id(UUID.randomUUID())
                .loanNumber("LOAN-" + generateId())
                .customerId(UUID.randomUUID())
                .productType(ProductType.MUDARABAH)
                .principalAmount(new Money(BigDecimal.valueOf(250000), Currency.getInstance("SAR")))
                .profitRate(BigDecimal.valueOf(0.055))
                .termMonths(48)
                .monthlyPayment(new Money(BigDecimal.valueOf(6000), Currency.getInstance("SAR")))
                .totalProfit(new Money(BigDecimal.valueOf(38000), Currency.getInstance("SAR")))
                .outstandingBalance(new Money(BigDecimal.valueOf(250000), Currency.getInstance("SAR")))
                .status(LoanStatus.ACTIVE)
                .disbursementDate(LocalDate.now().minusMonths(3))
                .maturityDate(LocalDate.now().plusMonths(45))
                .createdAt(LocalDateTime.now().minusMonths(3))
                .build();
        }

        public static Loan pendingLoan() {
            Loan loan = murabahaLoan();
            loan.setStatus(LoanStatus.PENDING);
            loan.setDisbursementDate(null);
            return loan;
        }

        public static Loan defaultedLoan() {
            Loan loan = murabahaLoan();
            loan.setStatus(LoanStatus.DEFAULTED);
            loan.setDisbursementDate(LocalDate.now().minusMonths(12));
            loan.setLastPaymentDate(LocalDate.now().minusMonths(3));
            return loan;
        }

        public static Loan closedLoan() {
            Loan loan = murabahaLoan();
            loan.setStatus(LoanStatus.CLOSED);
            loan.setOutstandingBalance(Money.ZERO(Currency.getInstance("SAR")));
            loan.setClosedDate(LocalDate.now());
            return loan;
        }
    }

    /**
     * Payment fixtures
     */
    public static class Payments {
        public static Payment successfulPayment() {
            return Payment.builder()
                .id(UUID.randomUUID())
                .paymentNumber("PAY-" + generateId())
                .loanId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .amount(new Money(BigDecimal.valueOf(3200), Currency.getInstance("SAR")))
                .principalAmount(new Money(BigDecimal.valueOf(2700), Currency.getInstance("SAR")))
                .profitAmount(new Money(BigDecimal.valueOf(500), Currency.getInstance("SAR")))
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .status(PaymentStatus.COMPLETED)
                .paymentDate(LocalDateTime.now())
                .dueDate(LocalDate.now())
                .referenceNumber("REF-" + generateId())
                .createdAt(LocalDateTime.now())
                .build();
        }

        public static Payment pendingPayment() {
            Payment payment = successfulPayment();
            payment.setStatus(PaymentStatus.PENDING);
            payment.setPaymentDate(null);
            return payment;
        }

        public static Payment failedPayment() {
            Payment payment = successfulPayment();
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Insufficient funds");
            return payment;
        }

        public static Payment reversedPayment() {
            Payment payment = successfulPayment();
            payment.setStatus(PaymentStatus.REVERSED);
            payment.setReversalReason("Customer dispute");
            payment.setReversedAt(LocalDateTime.now());
            return payment;
        }

        public static List<Payment> paymentSchedule(Loan loan, int months) {
            List<Payment> payments = new ArrayList<>();
            LocalDate dueDate = loan.getDisbursementDate().plusMonths(1);

            for (int i = 1; i <= months; i++) {
                Payment payment = Payment.builder()
                    .id(UUID.randomUUID())
                    .paymentNumber("PAY-" + generateId())
                    .loanId(loan.getId())
                    .customerId(loan.getCustomerId())
                    .amount(loan.getMonthlyPayment())
                    .dueDate(dueDate)
                    .installmentNumber(i)
                    .status(i <= 3 ? PaymentStatus.COMPLETED : PaymentStatus.SCHEDULED)
                    .createdAt(LocalDateTime.now())
                    .build();

                if (payment.getStatus() == PaymentStatus.COMPLETED) {
                    payment.setPaymentDate(dueDate.atStartOfDay());
                    payment.setPaymentMethod(PaymentMethod.BANK_TRANSFER);
                    payment.setReferenceNumber("REF-" + generateId());
                }

                payments.add(payment);
                dueDate = dueDate.plusMonths(1);
            }

            return payments;
        }
    }

    /**
     * Money fixtures
     */
    public static class Moneys {
        public static Money sarAmount(double amount) {
            return new Money(BigDecimal.valueOf(amount), Currency.getInstance("SAR"));
        }

        public static Money usdAmount(double amount) {
            return new Money(BigDecimal.valueOf(amount), Currency.getInstance("USD"));
        }

        public static Money zeroSar() {
            return Money.ZERO(Currency.getInstance("SAR"));
        }

        public static Money randomAmount(double min, double max) {
            double amount = min + (random.nextDouble() * (max - min));
            return sarAmount(Math.round(amount * 100.0) / 100.0);
        }
    }

    /**
     * Account fixtures
     */
    public static class Accounts {
        public static Account savingsAccount() {
            return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-" + generateId())
                .customerId(UUID.randomUUID())
                .accountType(AccountType.SAVINGS)
                .balance(new Money(BigDecimal.valueOf(25000), Currency.getInstance("SAR")))
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now().minusYears(2))
                .createdAt(LocalDateTime.now().minusYears(2))
                .build();
        }

        public static Account currentAccount() {
            return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-" + generateId())
                .customerId(UUID.randomUUID())
                .accountType(AccountType.CURRENT)
                .balance(new Money(BigDecimal.valueOf(10000), Currency.getInstance("SAR")))
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now().minusYears(1))
                .createdAt(LocalDateTime.now().minusYears(1))
                .build();
        }

        public static Account investmentAccount() {
            return Account.builder()
                .id(UUID.randomUUID())
                .accountNumber("ACC-" + generateId())
                .customerId(UUID.randomUUID())
                .accountType(AccountType.INVESTMENT)
                .balance(new Money(BigDecimal.valueOf(100000), Currency.getInstance("SAR")))
                .status(AccountStatus.ACTIVE)
                .openedDate(LocalDate.now().minusMonths(6))
                .createdAt(LocalDateTime.now().minusMonths(6))
                .build();
        }
    }

    /**
     * Test data builders for complex scenarios
     */
    public static class Scenarios {
        public static TestScenario loanWithPayments() {
            Customer customer = Customers.validCustomer();
            Loan loan = Loans.murabahaLoan();
            loan.setCustomerId(customer.getId());

            List<Payment> payments = Payments.paymentSchedule(loan, 12);
            Account account = Accounts.savingsAccount();
            account.setCustomerId(customer.getId());

            return TestScenario.builder()
                .customer(customer)
                .loan(loan)
                .payments(payments)
                .account(account)
                .build();
        }

        public static TestScenario multipleLoansScenario() {
            Customer customer = Customers.validCustomer();

            Loan murabaha = Loans.murabahaLoan();
            murabaha.setCustomerId(customer.getId());

            Loan ijarah = Loans.ijarahLoan();
            ijarah.setCustomerId(customer.getId());

            Account current = Accounts.currentAccount();
            current.setCustomerId(customer.getId());

            Account savings = Accounts.savingsAccount();
            savings.setCustomerId(customer.getId());

            return TestScenario.builder()
                .customer(customer)
                .loans(Arrays.asList(murabaha, ijarah))
                .accounts(Arrays.asList(current, savings))
                .build();
        }
    }

    private static String generateId() {
        return String.format("%08d", idCounter.getAndIncrement());
    }

    /**
     * Test scenario container
     */
    @lombok.Builder
    @lombok.Data
    public static class TestScenario {
        private Customer customer;
        private Loan loan;
        private List<Loan> loans;
        private List<Payment> payments;
        private Account account;
        private List<Account> accounts;
        private Map<String, Object> metadata;
    }
}