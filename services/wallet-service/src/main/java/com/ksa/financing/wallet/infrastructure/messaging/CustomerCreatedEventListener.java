package com.ksa.financing.wallet.infrastructure.messaging;

import com.ksa.financing.wallet.domain.port.in.CreateWalletUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

/**
 * Kafka consumer that listens for CustomerCreated event and auto-creates a wallet.
 *
 * When the customer-service publishes a customer-created event, this listener
 * receives it and triggers wallet creation. The operation is idempotent: if a
 * wallet already exists for the customer, the creation is skipped.
 */
@Component
@RequiredArgsConstructor
public class CustomerCreatedEventListener {

    private static final Logger log = LoggerFactory.getLogger(CustomerCreatedEventListener.class);

    private final CreateWalletUseCase createWalletUseCase;

    @KafkaListener(
            topics = "islamic-financing.customer.customer-created",
            groupId = "wallet-service",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    public void onCustomerCreated(Map<String, Object> event) {
        try {
            log.info("Received customer-created event: {}", event);

            String customerIdStr = extractString(event, "customerId");
            String tenantIdStr = extractString(event, "tenantId");
            String currency = extractString(event, "currency");

            if (customerIdStr == null || tenantIdStr == null) {
                log.error("Invalid customer-created event: missing customerId or tenantId. Event: {}", event);
                return;
            }

            UUID customerId = UUID.fromString(customerIdStr);
            UUID tenantId;
            try {
                tenantId = UUID.fromString(tenantIdStr);
            } catch (IllegalArgumentException e) {
                tenantId = UUID.nameUUIDFromBytes(tenantIdStr.getBytes());
            }

            log.info("Processing wallet creation for customer: {} tenant: {}", customerId, tenantId);

            createWalletUseCase.create(new CreateWalletUseCase.CreateWalletCommand(
                    tenantId,
                    customerId,
                    currency != null ? currency : "SAR"
            ));

            log.info("Successfully processed wallet creation for customer: {}", customerId);

        } catch (IllegalArgumentException e) {
            // Idempotent: wallet already exists, skip
            log.info("Wallet already exists or invalid data, skipping: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error processing customer-created event: {}", event, e);
            throw e; // Re-throw to trigger retry/DLQ
        }
    }

    private String extractString(Map<String, Object> event, String key) {
        Object value = event.get(key);
        return value != null ? value.toString() : null;
    }
}
