package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.CustomerBlock;

import java.util.List;
import java.util.UUID;

public interface CustomerBlockRepository {
    CustomerBlock save(CustomerBlock customerBlock);
    List<CustomerBlock> findAllActiveByCustomerId(UUID customerId);
    List<CustomerBlock> findAllByCustomerId(UUID customerId);
    void deactivateAllForCustomer(UUID customerId);
}
