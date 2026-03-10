package com.ksa.financing.customer.infrastructure.messaging;

import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka-based event publisher for Customer Service domain events.
 * <p>
 * CRITICAL: The customer-created event triggers wallet auto-creation in Wallet Service.
 * The payload must include: customerId, tenantId, cifNumber, fullName, email.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerEventPublisher implements EventPublisherPort {

    private static final String TOPIC_CUSTOMER_CREATED = "islamic-financing.customer.customer-created";
    private static final String TOPIC_CUSTOMER_UPDATED = "islamic-financing.customer.customer-updated";
    private static final String TOPIC_KYC_STATUS_CHANGED = "islamic-financing.customer.kyc-status-changed";
    private static final String TOPIC_LIFECYCLE_STAGE_CHANGED = "islamic-financing.customer.lifecycle-stage-changed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishCustomerCreated(Customer customer) {
        log.info("Publishing customer-created event for customerId: {}, CIF: {}",
                customer.getId(), customer.getCifNumber());

        // CRITICAL: This payload triggers wallet auto-creation in Wallet Service
        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customer.getId().toString());
        payload.put("tenantId", customer.getTenantId().toString());
        payload.put("cifNumber", customer.getCifNumber());
        payload.put("fullName", customer.getFullName());
        payload.put("email", customer.getEmail());
        payload.put("customerType", customer.getCustomerType() != null ? customer.getCustomerType().name() : null);
        payload.put("globalUid", customer.getGlobalUid() != null ? customer.getGlobalUid().toString() : null);
        payload.put("lifecycleStage", customer.getLifecycleStage() != null ? customer.getLifecycleStage().name() : null);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CUSTOMER_CREATED");
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);

        kafkaTemplate.send(TOPIC_CUSTOMER_CREATED, customer.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish customer-created event for customerId: {}. Error: {}",
                                customer.getId(), ex.getMessage());
                    } else {
                        log.info("Customer-created event published successfully for customerId: {}, offset: {}",
                                customer.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishCustomerUpdated(Customer customer) {
        log.info("Publishing customer-updated event for customerId: {}", customer.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customer.getId().toString());
        payload.put("tenantId", customer.getTenantId().toString());
        payload.put("cifNumber", customer.getCifNumber());
        payload.put("fullName", customer.getFullName());
        payload.put("email", customer.getEmail());

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "CUSTOMER_UPDATED");
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);

        kafkaTemplate.send(TOPIC_CUSTOMER_UPDATED, customer.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish customer-updated event for customerId: {}. Error: {}",
                                customer.getId(), ex.getMessage());
                    } else {
                        log.debug("Customer-updated event published successfully for customerId: {}, offset: {}",
                                customer.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishKycStatusChanged(Customer customer, String oldStatus, String newStatus) {
        log.info("Publishing kyc-status-changed event for customerId: {}, {} -> {}",
                customer.getId(), oldStatus, newStatus);

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customer.getId().toString());
        payload.put("tenantId", customer.getTenantId().toString());
        payload.put("cifNumber", customer.getCifNumber());
        payload.put("oldStatus", oldStatus);
        payload.put("newStatus", newStatus);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "KYC_STATUS_CHANGED");
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);

        kafkaTemplate.send(TOPIC_KYC_STATUS_CHANGED, customer.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish kyc-status-changed event for customerId: {}. Error: {}",
                                customer.getId(), ex.getMessage());
                    } else {
                        log.debug("KYC-status-changed event published successfully for customerId: {}, offset: {}",
                                customer.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishLifecycleStageChanged(Customer customer, String oldStage, String newStage) {
        log.info("Publishing lifecycle-stage-changed event for customerId: {}, {} -> {}",
                customer.getId(), oldStage, newStage);

        Map<String, Object> payload = new HashMap<>();
        payload.put("customerId", customer.getId().toString());
        payload.put("tenantId", customer.getTenantId().toString());
        payload.put("cifNumber", customer.getCifNumber());
        payload.put("oldStage", oldStage);
        payload.put("newStage", newStage);

        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "LIFECYCLE_STAGE_CHANGED");
        event.put("timestamp", Instant.now().toString());
        event.put("payload", payload);

        kafkaTemplate.send(TOPIC_LIFECYCLE_STAGE_CHANGED, customer.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish lifecycle-stage-changed event for customerId: {}. Error: {}",
                                customer.getId(), ex.getMessage());
                    } else {
                        log.debug("Lifecycle-stage-changed event published successfully for customerId: {}, offset: {}",
                                customer.getId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
