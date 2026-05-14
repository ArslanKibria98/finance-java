package com.ksa.financing.collections.infrastructure.messaging;

import com.ksa.financing.collections.domain.port.in.ManageDunningPolicyUseCase;
import com.ksa.financing.collections.domain.port.out.DunningPolicyRepository;
import com.ksa.financing.collections.domain.model.DunningPolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Listens to product-related events from product-service and synchronizes collections configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProductEventListener {

    private final ManageDunningPolicyUseCase dunningPolicyUseCase;
    private final DunningPolicyRepository policyRepository;

    /**
     * Synchronizes penalty waiver settings from product-service to collections-service dunning policies.
     * Note: In collections-service, we use the product's UUID (productId) as the linking key in DunningPolicy.productCode.
     */
    @KafkaListener(
        topics = {"financing.product.fee-settings-updated", "financing.product.updated"},
        groupId = "collections-service-product-sync",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onProductEvent(@Payload Map<String, Object> event) {
        try {
            log.info("Received Product event: {}", event);
            Map<String, Object> payload = (Map<String, Object>) event.get("payload");
            
            if (payload == null) {
                log.warn("Event payload is null");
                return;
            }

            UUID tenantId = UUID.fromString((String) payload.get("tenantId"));
            String productId = (String) payload.get("productId");
            String humanReadableCode = (String) payload.get("productCode");
            
            // maxPenaltyWaiversAllowed might be Integer or Long
            Object maxObj = payload.get("maxPenaltyWaiversAllowed");
            Integer maxPenaltyWaiversAllowed = maxObj != null ? ((Number) maxObj).intValue() : null;
            Boolean penaltyWaiverAllowed = (Boolean) payload.get("penaltyWaiverAllowed");

            if (maxPenaltyWaiversAllowed == null && penaltyWaiverAllowed == null) {
                log.debug("No waiver settings in event payload. Skipping sync.");
                return;
            }

            log.info("Synchronizing waiver settings for productId: {} ({}). Limit: {}, Allowed: {}", 
                    productId, humanReadableCode, maxPenaltyWaiversAllowed, penaltyWaiverAllowed);

            // IMPORTANT: Collections uses the UUID string as the productCode for lookup
            String policyProductKey = productId;

            // 1. Try to find active policy specifically for this product
            var policyOpt = (policyProductKey != null && !policyProductKey.isBlank())
                    ? policyRepository.findActiveByProductCode(tenantId, policyProductKey)
                    : policyRepository.findActiveDefault(tenantId);

            if (policyOpt.isPresent()) {
                updateExistingPolicy(tenantId, policyOpt.get(), humanReadableCode != null ? humanReadableCode : policyProductKey, 
                        penaltyWaiverAllowed, maxPenaltyWaiversAllowed);
            } else if (policyProductKey != null && !policyProductKey.isBlank()) {
                // 2. If no product policy exists, create one based on the default policy
                createNewProductPolicy(tenantId, policyProductKey, humanReadableCode, penaltyWaiverAllowed, maxPenaltyWaiversAllowed);
            } else {
                log.warn("No active dunning policy found (and no productId). Sync skipped.");
            }

        } catch (Exception e) {
            log.error("Error processing Product event: {}", e.getMessage(), e);
        }
    }

    private void updateExistingPolicy(UUID tenantId, DunningPolicy policy, String displayName, 
                                      Boolean penaltyWaiverAllowed, Integer maxPenaltyWaiversAllowed) {
        
        // Only update if values have actually changed
        boolean sameAllowed = penaltyWaiverAllowed == null || policy.isPenaltyWaiverAllowed() == penaltyWaiverAllowed;
        boolean sameLimit = maxPenaltyWaiversAllowed == null || policy.getMaxPenaltyWaiversAllowed() == maxPenaltyWaiversAllowed;
        
        if (sameAllowed && sameLimit) {
            log.info("Dunning policy {} for product {} is already up to date.", policy.getId(), displayName);
            return;
        }

        log.info("Updating existing dunning policy {} for product {} with new waiver settings", 
                policy.getId(), displayName);
        
        var command = new ManageDunningPolicyUseCase.UpdatePolicyCommand(
                tenantId,
                policy.getId().getValue(),
                null, null, null, null, null, null, null,
                policy.isAutoAssignAgent(),
                policy.getAgentAssignmentDpd(),
                policy.getWalletFreezeDpd(),
                penaltyWaiverAllowed != null ? penaltyWaiverAllowed : policy.isPenaltyWaiverAllowed(),
                maxPenaltyWaiversAllowed != null ? maxPenaltyWaiversAllowed : policy.getMaxPenaltyWaiversAllowed(),
                UUID.randomUUID()
        );
        dunningPolicyUseCase.updatePolicy(command);
        log.info("✅ Dunning policy updated successfully via sync");
    }

    private void createNewProductPolicy(UUID tenantId, String productId, String humanReadableCode,
                                        Boolean penaltyWaiverAllowed, Integer maxPenaltyWaiversAllowed) {
        
        String displayName = humanReadableCode != null ? humanReadableCode : productId;
        log.info("Creating new product-specific policy for {} based on default policy", displayName);
        
        var defaultPolicyOpt = policyRepository.findActiveDefault(tenantId);
        if (defaultPolicyOpt.isPresent()) {
            var def = defaultPolicyOpt.get();
            var command = new ManageDunningPolicyUseCase.CreatePolicyCommand(
                    tenantId,
                    "Policy for " + displayName,
                    productId, // Use UUID as the key
                    "Automatically created via sync from Product Settings (" + displayName + ")",
                    false,
                    def.getThresholds(),
                    def.getLateFee(),
                    def.getSimah(),
                    def.getStageActions(),
                    def.isAutoAssignAgent(),
                    def.getAgentAssignmentDpd(),
                    def.getWalletFreezeDpd(),
                    penaltyWaiverAllowed != null ? penaltyWaiverAllowed : def.isPenaltyWaiverAllowed(),
                    maxPenaltyWaiversAllowed != null ? maxPenaltyWaiversAllowed : def.getMaxPenaltyWaiversAllowed(),
                    UUID.randomUUID()
            );
            dunningPolicyUseCase.createPolicy(command);
            log.info("✅ New dunning policy created for {} via sync", displayName);
        } else {
            log.warn("No default policy found to use as template for {}. Sync failed.", displayName);
        }
    }
}
