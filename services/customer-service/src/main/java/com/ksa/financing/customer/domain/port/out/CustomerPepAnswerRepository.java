package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.CustomerPepAnswer;

import java.util.Optional;
import java.util.UUID;

public interface CustomerPepAnswerRepository {

    CustomerPepAnswer save(CustomerPepAnswer answer);

    Optional<CustomerPepAnswer> findByCustomer(UUID tenantId, UUID customerId);
}
