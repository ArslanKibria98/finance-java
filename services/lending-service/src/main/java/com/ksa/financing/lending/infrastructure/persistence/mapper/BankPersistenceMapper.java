package com.ksa.financing.lending.infrastructure.persistence.mapper;

import com.ksa.financing.lending.domain.model.Bank;
import com.ksa.financing.lending.infrastructure.persistence.entity.BankJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BankPersistenceMapper {

    public Bank toDomain(BankJpaEntity entity) {
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

    public BankJpaEntity toEntity(Bank bank) {
        var entity = new BankJpaEntity();
        entity.setId(bank.id());
        entity.setTenantId(bank.tenantId());
        entity.setCode(bank.code());
        entity.setNameEn(bank.nameEn());
        entity.setNameAr(bank.nameAr());
        entity.setActive(bank.active());
        entity.setSortOrder(bank.sortOrder());
        var now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }
}
