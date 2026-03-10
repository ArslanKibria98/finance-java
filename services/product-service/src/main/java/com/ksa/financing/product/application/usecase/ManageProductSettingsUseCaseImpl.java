package com.ksa.financing.product.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.product.domain.port.in.ManageProductSettingsUseCase;
import com.ksa.financing.product.domain.port.out.ProductRepository;
import com.ksa.financing.product.domain.port.out.ProductSettingsRepository;
import com.ksa.financing.product.domain.port.out.ProductSettingsRepository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageProductSettingsUseCaseImpl implements ManageProductSettingsUseCase {

    private final ProductRepository productRepository;
    private final ProductSettingsRepository settingsRepository;

    @Override
    @Transactional
    public void updateApplicationSteps(UUID tenantId, UUID productId, List<ApplicationStepCommand> steps) {
        log.info("Updating application steps for product: {}", productId);
        ensureProductExists(tenantId, productId);

        var data = steps.stream()
            .map(s -> new ApplicationStepData(s.stepNumber(), s.titleEn(), s.titleAr(),
                s.description(), s.required(), s.sortOrder()))
            .toList();

        settingsRepository.saveApplicationSteps(tenantId, productId, data);
        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateTermsConditions(UUID tenantId, UUID productId, UpdateTermsConditionsCommand command) {
        log.info("Updating terms & conditions for product: {}", productId);
        ensureProductExists(tenantId, productId);

        settingsRepository.saveTermsConditions(tenantId, productId, command.termsEn(), command.termsAr());
        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateFeeSettings(UUID tenantId, UUID productId, UpdateFeeSettingsCommand command) {
        log.info("Updating fee settings for product: {}", productId);
        ensureProductExists(tenantId, productId);

        settingsRepository.saveFeeSettings(
            tenantId, productId,
            command.minFinancingAmount(), command.maxFinancingAmount(),
            command.vatPercentage(), command.revenueEligibilityThreshold(),
            command.maxDbrPercentage(), command.dbrCalculationMethod(),
            command.dbrExceptions()
        );

        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateAdminFeeSlabs(UUID tenantId, UUID productId, List<AdminFeeSlabCommand> slabs) {
        log.info("Updating admin fee slabs for product: {}", productId);
        ensureProductExists(tenantId, productId);

        var data = slabs.stream()
            .map(s -> new AdminFeeSlabData(s.minAmount(), s.maxAmount(), s.profitPercentage(),
                s.processingFee(), s.adminFee(), s.partnerScope(), s.status(), s.sortOrder(),
                s.minTenure(), s.maxTenure()))
            .toList();

        settingsRepository.saveAdminFeeSlabs(tenantId, productId, data);
        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateEnvironmentConfigs(UUID tenantId, UUID productId, List<EnvironmentConfigLinkCommand> configs) {
        log.info("Updating environment configs for product: {}", productId);
        ensureProductExists(tenantId, productId);

        var data = configs.stream()
            .map(c -> new EnvironmentConfigLinkData(c.environmentConfigId(), c.active(), c.sortOrder()))
            .toList();

        settingsRepository.saveEnvironmentConfigs(tenantId, productId, data);
        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateDurationSettings(UUID tenantId, UUID productId, UpdateDurationSettingsCommand command) {
        log.info("Updating duration settings for product: {}", productId);
        ensureProductExists(tenantId, productId);

        settingsRepository.saveDurationSettings(
            tenantId, productId,
            command.requestDurationDays(), command.approvalDurationDays(),
            command.disbursementDurationDays(), command.repaymentDurationDays()
        );

        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    @Override
    @Transactional
    public void updateApprovalWorkflows(UUID tenantId, UUID productId, List<ApprovalWorkflowCommand> workflows) {
        log.info("Updating approval workflows for product: {}", productId);
        ensureProductExists(tenantId, productId);

        var data = workflows.stream()
            .map(w -> new ApprovalWorkflowData(
                w.workflowType(), w.nameEn(), w.nameAr(), w.description(),
                w.templateSource(), w.active(), w.priority(),
                w.conditions() == null ? List.of() : w.conditions().stream()
                    .map(c -> new ApprovalConditionData(c.field(), c.operator(), c.value(), c.sortOrder()))
                    .toList(),
                w.actions() == null ? List.of() : w.actions().stream()
                    .map(a -> new ApprovalActionData(a.actionType(), a.configuration(), a.sortOrder()))
                    .toList()
            ))
            .toList();

        settingsRepository.saveApprovalWorkflows(tenantId, productId, data);
        advanceWizardIfNeeded(tenantId, productId, 3);
    }

    private void ensureProductExists(UUID tenantId, UUID productId) {
        productRepository.findById(tenantId, productId)
            .orElseThrow(() -> NotFoundException.forEntity("Product", productId.toString()));
    }

    private void advanceWizardIfNeeded(UUID tenantId, UUID productId, int step) {
        productRepository.findById(tenantId, productId).ifPresent(product -> {
            if (product.getWizardStep() < step) {
                product.advanceWizardStep(step);
                productRepository.save(product);
            }
        });
    }
}
