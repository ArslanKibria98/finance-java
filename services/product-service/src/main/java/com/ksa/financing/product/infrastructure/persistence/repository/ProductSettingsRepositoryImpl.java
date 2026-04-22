package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.DurationSettings;
import com.ksa.financing.product.domain.port.out.ProductSettingsRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ProductSettingsRepositoryImpl implements ProductSettingsRepository {

    private final JpaFeeSettingsRepository jpaFeeSettingsRepository;
private final JpaTermsConditionsRepository jpaTermsConditionsRepository;
    private final JpaApplicationStepRepository jpaApplicationStepRepository;
    private final JpaAdminFeeSlabRepository jpaAdminFeeSlabRepository;
    private final JpaProductEnvironmentConfigRepository jpaProductEnvironmentConfigRepository;
    private final JpaApprovalWorkflowRepository jpaApprovalWorkflowRepository;
    private final JpaApprovalConditionRepository jpaApprovalConditionRepository;
    private final JpaApprovalActionRepository jpaApprovalActionRepository;
    private final JpaProductDurationSettingsRepository jpaProductDurationSettingsRepository;
    // --- Tab 1: Application Steps (replace-all) ---

    @Override
    public void saveApplicationSteps(UUID tenantId, UUID productId, List<ApplicationStepData> steps) {
        log.debug("Saving application steps for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jpaApplicationStepRepository.deleteByProductIdAndTenantId(productId, tenantId);
        jpaApplicationStepRepository.flush();

        for (var step : steps) {
            var entity = new ApplicationStepJpaEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setStepNumber(step.stepNumber());
            entity.setTitleEn(step.titleEn());
            entity.setTitleAr(step.titleAr());
            entity.setDescription(step.description());
            entity.setRequired(step.required());
            entity.setSortOrder(step.sortOrder());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            jpaApplicationStepRepository.save(entity);
        }
        log.debug("Application steps saved for productId={}", productId);
    }

    // --- Tab 2: Terms & Conditions (upsert) ---

    @Override
    public void saveTermsConditions(UUID tenantId, UUID productId, String termsEn, String termsAr) {
        log.debug("Saving terms & conditions for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = jpaTermsConditionsRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseGet(() -> {
                    var newEntity = new TermsConditionsJpaEntity();
                    newEntity.setTenantId(tenantId);
                    newEntity.setProductId(productId);
                    newEntity.setCreatedAt(now);
                    return newEntity;
                });

        entity.setTermsEn(termsEn);
        entity.setTermsAr(termsAr);
        entity.setUpdatedAt(now);

        jpaTermsConditionsRepository.save(entity);
        log.debug("Terms & conditions saved for productId={}", productId);
    }

    // --- Tab 3: Fee Settings (upsert) ---

    @Override
    public void saveFeeSettings(UUID tenantId, UUID productId,
                                BigDecimal revenueThreshold, BigDecimal maxDbrPct,
                                String dbrMethod, String dbrExceptions,
                                BigDecimal maxDti, Integer minAge, Integer maxAge,
                                BigDecimal gdbrPercentage) {
        log.debug("Saving fee settings for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = jpaFeeSettingsRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseGet(() -> {
                    var newEntity = new FeeSettingsJpaEntity();
                    newEntity.setTenantId(tenantId);
                    newEntity.setProductId(productId);
                    newEntity.setCreatedAt(now);
                    return newEntity;
                });

        entity.setRevenueEligibilityThreshold(revenueThreshold);
        entity.setMaxDbrPercentage(maxDbrPct);
        entity.setDbrCalculationMethod(dbrMethod);
        entity.setDbrExceptions(dbrExceptions);
        entity.setMaxDti(maxDti);
        entity.setMinAge(minAge);
        entity.setMaxAge(maxAge);
        entity.setGdbrPercentage(gdbrPercentage);
        entity.setUpdatedAt(now);

        jpaFeeSettingsRepository.save(entity);
        log.debug("Fee settings saved for productId={}", productId);
    }

    // --- Tab 4: Admin Fee Slabs (replace-all) ---

    @Override
    public void saveAdminFeeSlabs(UUID tenantId, UUID productId, List<AdminFeeSlabData> slabs) {
        log.debug("Saving admin fee slabs for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jpaAdminFeeSlabRepository.deleteByProductIdAndTenantId(productId, tenantId);
        jpaAdminFeeSlabRepository.flush();

        for (var slab : slabs) {
            var entity = new AdminFeeSlabJpaEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setMinAmount(slab.minAmount());
            entity.setMaxAmount(slab.maxAmount());
            entity.setProfitPercentage(slab.profitPercentage());
            entity.setProcessingFee(slab.processingFee());
            entity.setAdminFee(slab.adminFee());
            entity.setPartnerScope(slab.partnerScope() != null ? slab.partnerScope() : "ALL_PARTNERS");
            entity.setStatus(slab.status() != null ? slab.status() : "ACTIVE");
            entity.setMinTenure(slab.minTenure());
            entity.setMaxTenure(slab.maxTenure());
            entity.setSortOrder(slab.sortOrder());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            jpaAdminFeeSlabRepository.save(entity);
        }
        log.debug("Admin fee slabs saved for productId={}", productId);
    }

    // --- Tab 5: Environment Configs (replace-all pivot rows) ---

    @Override
    public void saveEnvironmentConfigs(UUID tenantId, UUID productId, List<EnvironmentConfigLinkData> configs) {
        log.debug("Saving environment configs for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        jpaProductEnvironmentConfigRepository.deleteByProductIdAndTenantId(productId, tenantId);
        jpaProductEnvironmentConfigRepository.flush();

        for (var config : configs) {
            var entity = new ProductEnvironmentConfigJpaEntity();
            entity.setTenantId(tenantId);
            entity.setProductId(productId);
            entity.setEnvironmentConfigId(config.environmentConfigId());
            entity.setActive(config.active());
            entity.setSortOrder(config.sortOrder());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            jpaProductEnvironmentConfigRepository.save(entity);
        }
        log.debug("Environment configs saved for productId={}", productId);
    }

    // --- Tab 6: Duration Settings (upsert) ---

    @Override
    public void saveDurationSettings(UUID tenantId, UUID productId,
                                     Integer requestDurationDays,
                                     Integer approvalDurationDays,
                                     Integer disbursementDurationHours,
                                     Integer repaymentDurationDays) {
        log.debug("Saving duration settings for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        var entity = jpaProductDurationSettingsRepository.findByProductIdAndTenantId(productId, tenantId)
                .orElseGet(() -> {
                    var newEntity = new ProductDurationSettingsJpaEntity();
                    newEntity.setTenantId(tenantId);
                    newEntity.setProductId(productId);
                    newEntity.setCreatedAt(now);
                    return newEntity;
                });

        entity.setRequestDurationDays(requestDurationDays != null ? requestDurationDays : 0);
        entity.setApprovalDurationDays(approvalDurationDays != null ? approvalDurationDays : 0);
        entity.setDisbursementDurationHours(disbursementDurationHours != null ? disbursementDurationHours : 0);
        entity.setRepaymentDurationDays(repaymentDurationDays != null ? repaymentDurationDays : 0);
        entity.setUpdatedAt(now);

        jpaProductDurationSettingsRepository.save(entity);
        log.debug("Duration settings saved for productId={}", productId);
    }

    @Override
    public Optional<DurationSettings> findDurationSettings(UUID tenantId, UUID productId) {
        return jpaProductDurationSettingsRepository
                .findByProductIdAndTenantId(productId, tenantId)
                .map(e -> new DurationSettings(
                        e.getId(),
                        e.getRequestDurationDays(),
                        e.getApprovalDurationDays(),
                        e.getDisbursementDurationHours(),
                        e.getRepaymentDurationDays()));
    }

    // --- Tab 7: Approval Workflows (replace-all with children) ---

    @Override
    public void saveApprovalWorkflows(UUID tenantId, UUID productId, List<ApprovalWorkflowData> workflows) {
        log.debug("Saving approval workflows for productId={}", productId);
        var now = OffsetDateTime.now(ZoneOffset.UTC);

        // Delete existing workflows (cascades to conditions and actions via DB FK ON DELETE CASCADE)
        var existingWorkflows = jpaApprovalWorkflowRepository.findByProductIdAndTenantIdOrderByPriority(productId, tenantId);
        for (var existing : existingWorkflows) {
            jpaApprovalConditionRepository.deleteByWorkflowId(existing.getId());
            jpaApprovalActionRepository.deleteByWorkflowId(existing.getId());
        }
        jpaApprovalWorkflowRepository.deleteByProductIdAndTenantId(productId, tenantId);
        jpaApprovalWorkflowRepository.flush();

        for (var workflow : workflows) {
            var wfEntity = new ApprovalWorkflowJpaEntity();
            wfEntity.setTenantId(tenantId);
            wfEntity.setProductId(productId);
            wfEntity.setWorkflowType(workflow.workflowType());
            wfEntity.setNameEn(workflow.nameEn());
            wfEntity.setNameAr(workflow.nameAr());
            wfEntity.setDescription(workflow.description());
            wfEntity.setTemplateSource(workflow.templateSource());
            wfEntity.setActive(workflow.active());
            wfEntity.setPriority(workflow.priority());
            wfEntity.setCreatedAt(now);
            wfEntity.setUpdatedAt(now);
            var savedWf = jpaApprovalWorkflowRepository.save(wfEntity);

            for (var condition : workflow.conditions()) {
                var condEntity = new ApprovalConditionJpaEntity();
                condEntity.setTenantId(tenantId);
                condEntity.setWorkflowId(savedWf.getId());
                condEntity.setField(condition.field());
                condEntity.setOperator(condition.operator());
                condEntity.setValue(condition.value());
                condEntity.setSortOrder(condition.sortOrder());
                condEntity.setCreatedAt(now);
                condEntity.setUpdatedAt(now);
                jpaApprovalConditionRepository.save(condEntity);
            }

            for (var action : workflow.actions()) {
                var actEntity = new ApprovalActionJpaEntity();
                actEntity.setTenantId(tenantId);
                actEntity.setWorkflowId(savedWf.getId());
                actEntity.setActionType(action.actionType());
                actEntity.setConfiguration(action.configuration());
                actEntity.setSortOrder(action.sortOrder());
                actEntity.setCreatedAt(now);
                actEntity.setUpdatedAt(now);
                jpaApprovalActionRepository.save(actEntity);
            }
        }
        log.debug("Approval workflows saved for productId={}", productId);
    }

}
