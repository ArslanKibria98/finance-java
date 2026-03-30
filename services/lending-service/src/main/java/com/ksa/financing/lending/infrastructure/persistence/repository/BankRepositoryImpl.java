package com.ksa.financing.lending.infrastructure.persistence.repository;

import com.ksa.financing.lending.domain.model.Bank;
import com.ksa.financing.lending.domain.port.out.BankRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BankRepositoryImpl implements BankRepository {

    private static final UUID GLOBAL_TENANT = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final JpaBankRepository jpaRepo;

    @Override
    public List<Bank> findAllActive(UUID tenantId) {
        var banks = jpaRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId);
        if (banks.isEmpty()) {
            banks = jpaRepo.findByTenantIdAndActiveTrueOrderBySortOrder(GLOBAL_TENANT);
        }
        return banks.stream().map(this::toDomain).toList();
    }

    private Bank toDomain(com.ksa.financing.lending.infrastructure.persistence.entity.BankJpaEntity entity) {
        return new Bank(
                entity.getId(),
                entity.getTenantId(),
                entity.getCode(),
                entity.getNameEn(),
                entity.getNameAr(),
                entity.isActive(),
                entity.getSortOrder()
        );
    }
}
