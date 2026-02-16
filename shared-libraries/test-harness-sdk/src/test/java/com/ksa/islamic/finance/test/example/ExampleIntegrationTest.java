package com.ksa.islamic.finance.test.example;

import com.ksa.islamic.finance.test.base.IntegrationTestBase;
import com.ksa.islamic.finance.test.fixture.TestFixtures;
import com.ksa.islamic.finance.domain.model.Customer;
import com.ksa.islamic.finance.domain.model.Loan;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static com.ksa.islamic.finance.test.assertion.IslamicFinanceAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Example integration test demonstrating test harness features
 */
public class ExampleIntegrationTest extends IntegrationTestBase {

    @Test
    void testCustomerCreation() {
        // Given
        Customer customer = TestFixtures.Customers.validCustomer();

        // When
        // Customer would be saved to database here

        // Then
        assertThat(customer)
            .isActive()
            .isKycVerified()
            .hasEmail("ahmad.rahman@example.com");
    }

    @Test
    void testLoanCreation() {
        // Given
        Loan loan = TestFixtures.Loans.murabahaLoan();

        // When
        // Loan would be created here

        // Then
        assertThat(loan)
            .isActive()
            .hasPositiveBalance()
            .maturesAfter(LocalDate.now());
    }

    @Test
    void testCompleteScenario() {
        // Given
        TestFixtures.TestScenario scenario = TestFixtures.Scenarios.loanWithPayments();

        // When
        // Scenario would be executed here

        // Then
        assertThat(scenario.getCustomer()).isActive();
        assertThat(scenario.getLoan()).hasPositiveBalance();
        assertThat(scenario.getPayments()).isNotEmpty();
    }
}