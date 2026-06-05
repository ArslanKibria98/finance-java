package com.ksa.financing.globalprofile.domain.port.out;

import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import java.util.Optional;
import java.util.UUID;

public interface GlobalCustomerRepository {
    GlobalCustomer save(GlobalCustomer globalCustomer);
    Optional<GlobalCustomer> findByGlobalUid(UUID globalUid);
    Optional<GlobalCustomer> findByEmailHash(String emailHash);
    Optional<GlobalCustomer> findByMobileHash(String mobileHash);
}
