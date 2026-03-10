package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.EmploymentInfo;
import com.ksa.financing.customer.domain.port.out.EmploymentInfoRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.EmploymentInfoJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class EmploymentInfoRepositoryImpl implements EmploymentInfoRepository {

    private final JpaEmploymentInfoRepository jpaRepository;

    @Override
    public EmploymentInfo save(EmploymentInfo employmentInfo) {
        log.debug("Saving employment info for customer: {}", employmentInfo.getCustomerId());
        EmploymentInfoJpaEntity entity = CustomerPersistenceMapper.toEntity(employmentInfo);
        EmploymentInfoJpaEntity saved = jpaRepository.save(entity);
        return CustomerPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<EmploymentInfo> findCurrentByCustomerId(UUID customerId) {
        log.debug("Finding current employment info for customer: {}", customerId);
        return jpaRepository.findByCustomerIdAndCurrentTrue(customerId)
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public List<EmploymentInfo> findAllByCustomerId(UUID customerId) {
        log.debug("Finding all employment info for customer: {}", customerId);
        return jpaRepository.findAllByCustomerId(customerId)
                .stream()
                .map(CustomerPersistenceMapper::toDomain)
                .toList();
    }
}
