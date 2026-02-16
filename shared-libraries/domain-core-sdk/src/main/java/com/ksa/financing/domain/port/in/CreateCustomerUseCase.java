package com.ksa.financing.domain.port.in;

import com.ksa.financing.domain.enums.CustomerType;
import com.ksa.financing.domain.valueobject.CustomerId;
import com.ksa.financing.domain.valueobject.NationalId;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Input port (use case) for creating a new customer.
 * <p>
 * This use case handles the onboarding of new customers to the platform,
 * creating their profile with identity verification and initial KYC status.
 * </p>
 *
 * @author KSA Financing Platform
 * @since 1.0.0
 */
public interface CreateCustomerUseCase {

    /**
     * Execute the create customer use case.
     *
     * @param command the create customer command
     * @return the result containing the created customer ID or failure information
     */
    Result execute(CreateCustomerCommand command);

    /**
     * Command for creating a new customer.
     *
     * @param customerType the type of customer (INDIVIDUAL, SME, CORPORATE)
     * @param nationalId the national ID or commercial registration number
     * @param fullName the customer's full name or business name
     * @param emailAddress the customer's email address
     * @param phoneNumber the customer's phone number
     */
    record CreateCustomerCommand(
            @NotNull CustomerType customerType,
            @NotNull NationalId nationalId,
            @NotNull String fullName,
            String emailAddress,
            String phoneNumber
    ) {
        /**
         * Canonical constructor with validation.
         */
        public CreateCustomerCommand {
            Objects.requireNonNull(customerType, "CustomerType cannot be null");
            Objects.requireNonNull(nationalId, "NationalId cannot be null");
            Objects.requireNonNull(fullName, "FullName cannot be null");

            if (fullName.trim().isEmpty()) {
                throw new IllegalArgumentException("FullName cannot be empty");
            }
        }
    }

    /**
     * Result of the create customer use case.
     *
     * @param success true if customer was created successfully
     * @param customerId the created customer ID (present if successful)
     * @param errorMessage the error message (present if failed)
     */
    record Result(
            boolean success,
            CustomerId customerId,
            String errorMessage
    ) {
        /**
         * Create a successful result.
         *
         * @param customerId the created customer ID
         * @return the success result
         */
        public static Result success(CustomerId customerId) {
            Objects.requireNonNull(customerId, "CustomerId cannot be null");
            return new Result(true, customerId, null);
        }

        /**
         * Create a failure result.
         *
         * @param errorMessage the error message
         * @return the failure result
         */
        public static Result failure(String errorMessage) {
            Objects.requireNonNull(errorMessage, "ErrorMessage cannot be null");
            return new Result(false, null, errorMessage);
        }
    }
}
