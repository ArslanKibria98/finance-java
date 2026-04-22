package com.ksa.financing.collections.domain.port.in;

import com.ksa.financing.collections.domain.model.DunningPolicy;
import com.ksa.financing.collections.domain.model.DunningStage;
import com.ksa.financing.collections.domain.model.DunningThresholds;
import com.ksa.financing.collections.domain.model.LateFeeConfig;
import com.ksa.financing.collections.domain.model.SimahReportingConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Admin-facing use case for managing tenant/product-wise dunning policies.
 * Every operation is tenant-scoped and requires a JWT-derived tenantId.
 */
public interface ManageDunningPolicyUseCase {

    record CreatePolicyCommand(
            UUID tenantId,
            String policyName,
            String productCode,           // NULL => tenant-wide default
            String description,
            boolean defaultPolicy,
            DunningThresholds thresholds,
            LateFeeConfig lateFee,
            SimahReportingConfig simah,
            Map<DunningStage, Map<String, Object>> stageActions,
            boolean autoAssignAgent,
            Integer agentAssignmentDpd,
            Integer walletFreezeDpd,
            UUID createdBy
    ) {}

    record UpdatePolicyCommand(
            UUID tenantId,
            UUID policyId,
            String policyName,
            String description,
            String productCode,
            DunningThresholds thresholds,
            LateFeeConfig lateFee,
            SimahReportingConfig simah,
            Map<DunningStage, Map<String, Object>> stageActions,
            boolean autoAssignAgent,
            Integer agentAssignmentDpd,
            Integer walletFreezeDpd,
            UUID updatedBy
    ) {}

    DunningPolicy createPolicy(CreatePolicyCommand command);

    DunningPolicy updatePolicy(UpdatePolicyCommand command);

    DunningPolicy activatePolicy(UUID tenantId, UUID policyId, UUID updatedBy);

    DunningPolicy deactivatePolicy(UUID tenantId, UUID policyId, UUID updatedBy);

    DunningPolicy markAsDefault(UUID tenantId, UUID policyId, UUID updatedBy);

    void deletePolicy(UUID tenantId, UUID policyId);

    DunningPolicy getPolicy(UUID tenantId, UUID policyId);

    List<DunningPolicy> listPolicies(UUID tenantId, boolean activeOnly);
}
