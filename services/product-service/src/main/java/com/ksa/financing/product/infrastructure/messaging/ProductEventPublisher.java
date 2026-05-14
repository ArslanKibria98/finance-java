package com.ksa.financing.product.infrastructure.messaging;

import com.ksa.financing.product.domain.model.Product;
import com.ksa.financing.product.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventPublisher implements EventPublisherPort {

    private static final String TOPIC_PRODUCT_CREATED = "financing.product.created";
    private static final String TOPIC_PRODUCT_ACTIVATED = "financing.product.activated";
    private static final String TOPIC_PRODUCT_UPDATED = "financing.product.updated";
    private static final String TOPIC_PRODUCT_FEE_SETTINGS_UPDATED = "financing.product.fee-settings-updated";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishProductCreated(Product product) {
        publish(TOPIC_PRODUCT_CREATED, "PRODUCT_CREATED", product);
    }

    @Override
    public void publishProductActivated(Product product) {
        publish(TOPIC_PRODUCT_ACTIVATED, "PRODUCT_ACTIVATED", product);
    }

    @Override
    public void publishProductUpdated(Product product) {
        publish(TOPIC_PRODUCT_UPDATED, "PRODUCT_UPDATED", product);
    }

    @Override
    public void publishFeeSettingsUpdated(UUID tenantId, UUID productId, String productCode, Integer maxPenaltyWaiversAllowed, Boolean penaltyWaiverAllowed) {
        log.info("Publishing FEE_SETTINGS_UPDATED event for productId: {}", productId);
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("productId", productId.toString());
            payload.put("tenantId", tenantId.toString());
            payload.put("productCode", productCode);
            payload.put("maxPenaltyWaiversAllowed", maxPenaltyWaiversAllowed);
            payload.put("penaltyWaiverAllowed", penaltyWaiverAllowed);

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", "FEE_SETTINGS_UPDATED");
            event.put("timestamp", Instant.now().toString());
            event.put("payload", payload);

            kafkaTemplate.send(TOPIC_PRODUCT_FEE_SETTINGS_UPDATED, productId.toString(), event);
        } catch (Exception e) {
            log.error("Failed to publish FEE_SETTINGS_UPDATED event: {}", e.getMessage());
        }
    }

    private void publish(String topic, String eventType, Product product) {
        log.info("Publishing {} event for productId: {}", eventType, product.getId());
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("productId", product.getId().toString());
            payload.put("tenantId", product.getTenantId().toString());
            payload.put("productCode", product.getProductCode());
            payload.put("nameEn", product.getNameEn());
            payload.put("status", product.getStatus() != null ? product.getStatus().name() : null);
            payload.put("penaltyWaiverAllowed", product.isPenaltyWaiverAllowed());
            payload.put("maxPenaltyWaiversAllowed", product.getMaxPenaltyWaiversAllowed());

            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("timestamp", Instant.now().toString());
            event.put("payload", payload);

            kafkaTemplate.send(topic, product.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event: {}", eventType, ex.getMessage());
                    } else {
                        log.info("{} event published successfully", eventType);
                    }
                });
        } catch (Exception e) {
            log.error("Failed to publish {} event (Kafka unavailable): {}", eventType, e.getMessage());
        }
    }
}
