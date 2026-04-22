package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyRuleId;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.application.service.DelinquencyRulesResolver;
import com.ksa.financing.collections.domain.port.in.ManageDelinquencyRuleUseCase;
import com.ksa.financing.collections.domain.port.out.DelinquencyRuleRepository;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageDelinquencyRuleUseCaseImpl implements ManageDelinquencyRuleUseCase {

    private final DelinquencyRuleRepository ruleRepository;
    private final EventPublisher eventPublisher;
    private final DelinquencyRulesResolver rulesResolver;

    @Override
    @Transactional
    public DelinquencyRule upsertRule(UpsertRuleCommand cmd) {
        log.info("Upsert delinquency rule tenant={} product={} type={} configs={}",
                cmd.tenantId(), cmd.productId(), cmd.delinquencyType(),
                cmd.configs() != null ? cmd.configs().size() : 0);

        var rule = ruleRepository.findActive(cmd.tenantId(), cmd.productId(), cmd.delinquencyType())
                .orElseGet(() -> DelinquencyRule.create(cmd.tenantId(), cmd.productId(), cmd.delinquencyType()));

        rule.updatePenalty(cmd.isPercentage(), cmd.penaltyPercentage(), cmd.penaltyAmount());
        rule.updateDayRange(cmd.fromDay(), cmd.tillDay());

        switch (cmd.delinquencyType()) {
            case BROKEN_PROMISES -> rule.updateBrokenPromises(cmd.promisesPerYear(), cmd.promisesPerLoan());
            case EARLY_SETTLEMENT -> rule.toggleCustom(cmd.isCustom());
            default -> { /* other types — no extra config */ }
        }

        if (cmd.charityFundAccount() != null) {
            rule.setCharityFundAccount(cmd.charityFundAccount());
        }

        applyConfigs(rule, cmd);

        var saved = ruleRepository.save(rule);
        rulesResolver.evictTenant(cmd.tenantId());
        publishEvents(saved);
        return saved;
    }

    private void applyConfigs(DelinquencyRule rule, UpsertRuleCommand cmd) {
        var configs = cmd.configs();
        boolean eligible = cmd.delinquencyType() == DelinquencyType.EARLY_SETTLEMENT && cmd.isCustom();

        if (!eligible) {
            if (configs != null && !configs.isEmpty()) {
                throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                        "configs are only allowed for EARLY_SETTLEMENT with isCustom=true");
            }
            rule.clearEarlySettlementConfigs();
            return;
        }

        rule.clearEarlySettlementConfigs();
        if (configs == null) return;

        for (var item : configs) {
            switch (item.kind()) {
                case SINGLE -> {
                    if (item.invoiceOrder() == null) {
                        throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                                "invoiceOrder required for SINGLE config");
                    }
                    rule.addSingleConfig(item.invoiceOrder(), item.fromDay(), item.tillDay(),
                            item.isPercentage(), item.discountPercentage(), item.discountAmount());
                }
                case RANGE -> {
                    if (item.rangeNo() == null || item.minInvoiceOrder() == null || item.maxInvoiceOrder() == null) {
                        throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                                "rangeNo, minInvoiceOrder, maxInvoiceOrder required for RANGE config");
                    }
                    rule.addRangeConfigs(item.rangeNo(), item.minInvoiceOrder(), item.maxInvoiceOrder(),
                            item.fromDay(), item.tillDay(), item.isPercentage(),
                            item.discountPercentage(), item.discountAmount());
                }
            }
        }
    }

    @Override
    @Transactional
    public void softDeleteRule(UUID tenantId, UUID ruleId) {
        var rule = loadOrThrow(tenantId, ruleId);
        rule.softDelete();
        ruleRepository.softDelete(tenantId, rule.getId());
        rulesResolver.evictTenant(tenantId);
        publishEvents(rule);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DelinquencyRule> listForProduct(UUID tenantId, UUID productId) {
        return ruleRepository.findAllForProduct(tenantId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public DelinquencyRule getById(UUID tenantId, UUID ruleId) {
        return loadOrThrow(tenantId, ruleId);
    }

    private DelinquencyRule loadOrThrow(UUID tenantId, UUID ruleId) {
        return ruleRepository.findById(tenantId, DelinquencyRuleId.of(ruleId))
                .orElseThrow(() -> NotFoundException.forEntity("DelinquencyRule", ruleId.toString()));
    }

    private void publishEvents(DelinquencyRule rule) {
        try {
            eventPublisher.publishAll(rule.getUncommittedEvents());
            rule.markEventsAsCommitted();
        } catch (RuntimeException ex) {
            throw new BusinessException(ErrorCodes.INTERNAL_ERROR,
                    "Failed to publish delinquency rule events: " + ex.getMessage(), ex.getMessage());
        }
    }
}
