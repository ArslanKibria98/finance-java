package com.ksa.financing.customer.domain.port.out;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Output port for fetching loan data from Lending Service.
 */
public interface LendingServicePort {

    /**
     * Get all loan applications for a customer.
     */
    List<Map<String, Object>> getLoanApplicationsByCustomerId(UUID customerId, String accessToken);

    /**
     * Get all active loans for a customer.
     */
    List<Map<String, Object>> getLoansByCustomerId(UUID customerId, String accessToken);

    /**
     * Get customer loan overview (total outstanding, next payment, etc.)
     */
    Map<String, Object> getCustomerLoanOverview(UUID customerId, String accessToken);
}
