package com.ksa.financing.globalprofile.domain.port.out;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;

public interface EventPublisherPort {
    void publishGlobalCustomerCreated(GlobalCustomer customer);
    void publishRegionalProfileLinked(RegionalProfile profile);
    void publishKycStatusChanged(RegionalProfile profile, String oldStatus, String newStatus);
}
