package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.application.service.DunningPolicyResolver;
import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningPolicyId;
import com.ksa.financing.collections.domain.port.in.ManageDunningPolicyUseCase;
import com.ksa.financing.collections.domain.port.out.DunningPolicyRepository;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageDunningPolicyUseCaseImpl implements ManageDunningPolicyUseCase {

    private final DunningPolicyRepository policyRepository;
    private final EventPublisher eventPublisher;
    private final DunningPolicyResolver policyResolver;

    @Override
    @Transactional
    public DunningPolicy createPolicy(CreatePolicyCommand c) {
        log.info("Creating dunning policy '{}' product={} tenant={}",
                c.policyName(), c.productCode(), c.tenantId());

        if (policyRepository.existsByName(c.tenantId(), c.policyName())) {
            throw new IllegalStateException("Policy name already exists: " + c.policyName());
        }

        var policy = DunningPolicy.create(
                c.tenantId(), c.policyName(), c.productCode(), c.description(),
                c.defaultPolicy(), c.thresholds(), c.lateFee(), c.simah(),
                c.stageActions(),
                c.autoAssignAgent(), c.agentAssignmentDpd(), c.walletFreezeDpd(),
                c.penaltyWaiverAllowed(), c.maxPenaltyWaiversAllowed(),
                c.createdBy());

        if (c.defaultPolicy()) {
            policyRepository.clearDefaultExcept(c.tenantId(), policy.getId());
        }

        var saved = policyRepository.save(policy);
        publishEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public DunningPolicy updatePolicy(UpdatePolicyCommand c) {
        var policy = loadOrThrow(c.tenantId(), c.policyId());

        if (c.policyName() != null || c.description() != null) {
            policy.rename(
                    c.policyName() != null ? c.policyName() : policy.getPolicyName(),
                    c.description(),
                    c.updatedBy());
        }
        if (c.productCode() != null || policy.getProductCode() != null) {
            policy.bindToProduct(c.productCode(), c.updatedBy());
        }
        if (c.thresholds() != null) {
            policy.updateThresholds(c.thresholds(), c.updatedBy());
        }
        if (c.lateFee() != null) {
            policy.updateLateFee(c.lateFee(), c.updatedBy());
        }
        if (c.simah() != null) {
            policy.updateSimah(c.simah(), c.updatedBy());
        }
        if (c.stageActions() != null) {
            c.stageActions().forEach((stage, actions) ->
                    policy.setStageActions(stage, actions, c.updatedBy()));
        }
        policy.updateEscalation(
                c.autoAssignAgent(), c.agentAssignmentDpd(), c.walletFreezeDpd(), c.updatedBy());
        if (c.penaltyWaiverAllowed() != null || c.maxPenaltyWaiversAllowed() != null) {
            policy.updateWaiverSettings(
                    c.penaltyWaiverAllowed() != null ? c.penaltyWaiverAllowed() : policy.isPenaltyWaiverAllowed(),
                    c.maxPenaltyWaiversAllowed() != null ? c.maxPenaltyWaiversAllowed() : policy.getMaxPenaltyWaiversAllowed(),
                    c.updatedBy());
        }

        var saved = policyRepository.save(policy);
        publishEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public DunningPolicy activatePolicy(UUID tenantId, UUID policyId, UUID updatedBy) {
        var policy = loadOrThrow(tenantId, policyId);
        policy.activate(updatedBy);
        var saved = policyRepository.save(policy);
        publishEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public DunningPolicy deactivatePolicy(UUID tenantId, UUID policyId, UUID updatedBy) {
        var policy = loadOrThrow(tenantId, policyId);
        policy.deactivate(updatedBy);
        var saved = policyRepository.save(policy);
        publishEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public DunningPolicy markAsDefault(UUID tenantId, UUID policyId, UUID updatedBy) {
        var policy = loadOrThrow(tenantId, policyId);
        policyRepository.clearDefaultExcept(tenantId, policy.getId());
        policy.markAsDefault(updatedBy);
        var saved = policyRepository.save(policy);
        publishEvents(saved);
        return saved;
    }

    @Override
    @Transactional
    public void deletePolicy(UUID tenantId, UUID policyId) {
        policyRepository.deleteById(tenantId, DunningPolicyId.of(policyId));
        policyResolver.evictTenantCache(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public DunningPolicy getPolicy(UUID tenantId, UUID policyId) {
        return loadOrThrow(tenantId, policyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DunningPolicy> listPolicies(UUID tenantId, boolean activeOnly) {
        return activeOnly
                ? policyRepository.findAllActive(tenantId)
                : policyRepository.findAll(tenantId);
    }

    private DunningPolicy loadOrThrow(UUID tenantId, UUID policyId) {
        return policyRepository.findById(tenantId, DunningPolicyId.of(policyId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Dunning policy not found: " + policyId));
    }

    private void publishEvents(DunningPolicy policy) {
        eventPublisher.publishAll(policy.getUncommittedEvents());
        policy.markEventsAsCommitted();
        policyResolver.evictTenantCache(policy.getTenantId());
    }
}
