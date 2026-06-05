package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.Customer;

public interface EventPublisherPort {
    void publishCustomerCreated(Customer customer);
    void publishCustomerUpdated(Customer customer);
    void publishKycStatusChanged(Customer customer, String oldStatus, String newStatus);
    void publishLifecycleStageChanged(Customer customer, String oldStage, String newStage);
}
