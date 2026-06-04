package com.ksa.financing.wallet.infrastructure.persistence.repository;

import com.ksa.financing.wallet.infrastructure.persistence.entity.IbftReconciliationLogJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaIbftReconciliationLogRepository extends JpaRepository<IbftReconciliationLogJpaEntity, UUID> {
}
