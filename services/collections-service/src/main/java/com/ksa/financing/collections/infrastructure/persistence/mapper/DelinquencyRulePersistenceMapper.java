package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyRuleId;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.EarlySettlementConfig;
import com.ksa.financing.collections.infrastructure.persistence.entity.DelinquencyRuleJpaEntity;
import com.ksa.financing.collections.infrastructure.persistence.entity.EarlySettlementConfigJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Maps between {@link DelinquencyRule} aggregate (and child {@link EarlySettlementConfig}) and JPA entities. */
@Component
public class DelinquencyRulePersistenceMapper {

    public DelinquencyRuleJpaEntity toEntity(DelinquencyRule r) {
        var e = new DelinquencyRuleJpaEntity();
        e.setId(r.getId().getValue());
        e.setTenantId(r.getTenantId());
        e.setProductId(r.getProductId());
        e.setDelinquencyType((short) r.getDelinquencyType().code());
        e.setPercentage(r.isPercentage());
        e.setPenaltyPercentage(r.getPenaltyPercentage());
        e.setPenaltyAmount(r.getPenaltyAmount());
        e.setFromDay(r.getFromDay());
        e.setTillDay(r.getTillDay());
        e.setPenaltyType((short) r.getPenaltyType());
        e.setPromisesPerYear(r.getPromisesPerYear());
        e.setPromisesPerLoan(r.getPromisesPerLoan());
        e.setCustom(r.isCustom());
        e.setSettlementStrategy((short) r.getSettlementStrategy().code());
        e.setSettlementDiscountType(r.getSettlementDiscountType());
        e.setSettlementMonths(r.getSettlementMonths());
        e.setSettlementAmountPerMonth(r.getSettlementAmountPerMonth());
        e.setCharityFundAccount(r.getCharityFundAccount());
        e.setChannel(r.getChannel());
        e.setRecordState((short) r.getRecordState());
        e.setVersion(r.getVersion());
        e.setCreatedAt(r.getCreated() != null ? r.getCreated() : LocalDateTime.now());
        e.setUpdatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt() : LocalDateTime.now());
        return e;
    }

    public DelinquencyRule toDomain(DelinquencyRuleJpaEntity e) {
        return DelinquencyRule.hydrate(
                DelinquencyRuleId.of(e.getId()),
                e.getTenantId(),
                e.getProductId(),
                DelinquencyType.fromCode(e.getDelinquencyType()),
                e.isPercentage(),
                e.getPenaltyPercentage(),
                e.getPenaltyAmount(),
                e.getFromDay(),
                e.getTillDay(),
                e.getPenaltyType(),
                e.getPromisesPerYear(),
                e.getPromisesPerLoan(),
                e.isCustom(),
                com.ksa.financing.collections.domain.model.EarlySettlementStrategy.fromCode(e.getSettlementStrategy()),
                e.getSettlementDiscountType(),
                e.getSettlementMonths(),
                e.getSettlementAmountPerMonth(),
                e.getCharityFundAccount(),
                e.getChannel(),
                e.getRecordState(),
                e.getVersion(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public EarlySettlementConfigJpaEntity toEntity(EarlySettlementConfig c) {
        var e = new EarlySettlementConfigJpaEntity();
        e.setId(c.getId());
        e.setDelinquencyId(c.getDelinquencyId());
        e.setInvoiceOrder(c.getInvoiceOrder());
        e.setFromDay(c.getFromDay());
        e.setTillDay(c.getTillDay());
        e.setPercentage(c.isPercentage());
        e.setDiscountPercentage(c.getDiscountPercentage());
        e.setDiscountAmount(c.getDiscountAmount());
        e.setRange(c.isRange());
        e.setRangeNo(c.getRangeNo());
        e.setMinInvoiceOrder(c.getMinInvoiceOrder());
        e.setMaxInvoiceOrder(c.getMaxInvoiceOrder());
        e.setChannel(c.getChannel() != null ? c.getChannel() : "LMS");
        e.setRecordState((short) c.getRecordState());
        e.setVersion(c.getVersion());
        e.setCreatedAt(c.getCreated() != null ? c.getCreated() : LocalDateTime.now());
        e.setUpdatedAt(c.getUpdatedAt() != null ? c.getUpdatedAt() : LocalDateTime.now());
        return e;
    }

    public EarlySettlementConfig toDomain(EarlySettlementConfigJpaEntity e) {
        return EarlySettlementConfig.builder()
                .id(e.getId())
                .delinquencyId(e.getDelinquencyId())
                .invoiceOrder(e.getInvoiceOrder())
                .fromDay(e.getFromDay())
                .tillDay(e.getTillDay())
                .isPercentage(e.isPercentage())
                .discountPercentage(e.getDiscountPercentage())
                .discountAmount(e.getDiscountAmount())
                .isRange(e.isRange())
                .rangeNo(e.getRangeNo())
                .minInvoiceOrder(e.getMinInvoiceOrder())
                .maxInvoiceOrder(e.getMaxInvoiceOrder())
                .channel(e.getChannel())
                .recordState(e.getRecordState())
                .version(e.getVersion())
                .created(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
