package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.EmploymentInfo;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmploymentInfoRepository {
    EmploymentInfo save(EmploymentInfo employmentInfo);
    Optional<EmploymentInfo> findCurrentByCustomerId(UUID customerId);
    List<EmploymentInfo> findAllByCustomerId(UUID customerId);
}
