package com.ksa.financing.wallet.infrastructure.messaging;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Kafka-based event publisher for wallet domain events.
 *
 * Publishes wallet lifecycle events to Kafka topics for downstream consumers:
 * - wallet-created: when a new wallet is provisioned
 * - top-up-completed: when a top-up is successfully processed
 */
@Component
@RequiredArgsConstructor
public class WalletEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(WalletEventPublisher.class);

    private static final String TOPIC_WALLET_CREATED = "islamic-financing.wallet.wallet-created";
    private static final String TOPIC_TOP_UP_COMPLETED = "islamic-financing.wallet.top-up-completed";
    private static final String TOPIC_TRANSFER_INITIATED = "financing.wallet.transfer.initiated";
    private static final String TOPIC_TRANSFER_COMPLETED = "financing.wallet.transfer.completed";
    private static final String TOPIC_TRANSFER_FAILED    = "financing.wallet.transfer.failed";
    private static final String TOPIC_TRANSFER_REVERSED  = "financing.wallet.transfer.reversed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void publishWalletCreated(Wallet wallet) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "WALLET_CREATED");
        event.put("walletId", wallet.getId().toString());
        event.put("tenantId", wallet.getTenantId().toString());
        event.put("customerId", wallet.getCustomerId().toString());
        event.put("walletNumber", wallet.getWalletNumber());
        event.put("currency", wallet.getCurrency());
        event.put("status", wallet.getStatus() != null ? wallet.getStatus().name() : null);
        event.put("timestamp", Instant.now().toString());

        log.info("Publishing wallet-created event for wallet: {} customer: {}",
                wallet.getId(), wallet.getCustomerId());

        kafkaTemplate.send(TOPIC_WALLET_CREATED, wallet.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish wallet-created event for wallet: {}", wallet.getId(), ex);
                    } else {
                        log.info("Successfully published wallet-created event for wallet: {} offset: {}",
                                wallet.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishWalletTopUp(Wallet wallet, BigDecimal amount) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", "TOP_UP_COMPLETED");
        event.put("walletId", wallet.getId().toString());
        event.put("tenantId", wallet.getTenantId().toString());
        event.put("customerId", wallet.getCustomerId().toString());
        event.put("amount", amount.toString());
        event.put("availableBalance", wallet.getAvailableBalance().toString());
        event.put("currency", wallet.getCurrency());
        event.put("timestamp", Instant.now().toString());

        log.info("Publishing top-up-completed event for wallet: {} amount: {}",
                wallet.getId(), amount);

        kafkaTemplate.send(TOPIC_TOP_UP_COMPLETED, wallet.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish top-up-completed event for wallet: {}", wallet.getId(), ex);
                    } else {
                        log.info("Successfully published top-up-completed event for wallet: {} offset: {}",
                                wallet.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    @Override
    public void publishTransferInitiated(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_INITIATED, "TRANSFER_INITIATED", t);
    }

    @Override
    public void publishTransferCompleted(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_COMPLETED, "TRANSFER_COMPLETED", t);
    }

    @Override
    public void publishTransferFailed(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_FAILED, "TRANSFER_FAILED", t);
    }

    @Override
    public void publishTransferReversed(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_REVERSED, "TRANSFER_REVERSED", t);
    }

    private void publishTransferEvent(String topic, String eventType, WalletTransfer t) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("transferId", t.getId().toString());
        event.put("transferNumber", t.getTransferNumber());
        event.put("tenantId", t.getTenantId() != null ? t.getTenantId().toString() : null);
        event.put("sourceWalletId", t.getSourceWalletId() != null ? t.getSourceWalletId().toString() : null);
        event.put("destinationWalletId", t.getDestinationWalletId() != null ? t.getDestinationWalletId().toString() : null);
        event.put("amount", t.getAmount() != null ? t.getAmount().toString() : null);
        event.put("feeAmount", t.getFeeAmount() != null ? t.getFeeAmount().toString() : null);
        event.put("currency", t.getCurrency());
        event.put("status", t.getStatus() != null ? t.getStatus().name() : null);
        event.put("errorCode", t.getErrorCode());
        event.put("timestamp", Instant.now().toString());

        log.info("Publishing {} event topic={} transferId={}", eventType, topic, t.getId());

        kafkaTemplate.send(topic, t.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event transferId={}", eventType, t.getId(), ex);
                    } else {
                        log.debug("Published {} event transferId={} offset={}",
                                eventType, t.getId(), result.getRecordMetadata().offset());
                    }
                });
    }
}
