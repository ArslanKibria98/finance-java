package com.ksa.financing.collections.infrastructure.persistence.repository;

import com.ksa.financing.collections.infrastructure.persistence.entity.EarlySettlementConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaEarlySettlementConfigRepository extends JpaRepository<EarlySettlementConfigJpaEntity, UUID> {

    List<EarlySettlementConfigJpaEntity> findAllByDelinquencyId(UUID delinquencyId);

    @Modifying
    @Query("DELETE FROM EarlySettlementConfigJpaEntity c WHERE c.delinquencyId = :ruleId AND c.id = :configId")
    int deleteByRuleIdAndId(@Param("ruleId") UUID ruleId, @Param("configId") UUID configId);

    @Modifying
    @Query("DELETE FROM EarlySettlementConfigJpaEntity c WHERE c.delinquencyId = :ruleId")
    int deleteAllByRuleId(@Param("ruleId") UUID ruleId);
}
