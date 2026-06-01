package com.ksa.financing.wallet.infrastructure.messaging;

import com.ksa.financing.wallet.domain.model.Wallet;
import com.ksa.financing.wallet.domain.model.WalletTransfer;
import com.ksa.financing.wallet.domain.model.WalletWithdrawal;
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
    private static final String TOPIC_TRANSFER_RECEIVED  = "financing.wallet.transfer.received";
    private static final String TOPIC_TRANSFER_FAILED    = "financing.wallet.transfer.failed";
    private static final String TOPIC_TRANSFER_REVERSED  = "financing.wallet.transfer.reversed";
    private static final String TOPIC_WITHDRAWAL_INITIATED   = "financing.wallet.withdrawal.initiated";
    private static final String TOPIC_WITHDRAWAL_COMPLETED   = "financing.wallet.withdrawal.completed";
    private static final String TOPIC_WITHDRAWAL_FAILED      = "financing.wallet.withdrawal.failed";
    private static final String TOPIC_WITHDRAWAL_COMPENSATED = "financing.wallet.withdrawal.compensated";

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
        publishTransferEvent(TOPIC_TRANSFER_INITIATED, "TRANSFER_INITIATED", t,
                t.getSourceCustomerId());
    }

    @Override
    public void publishTransferCompleted(WalletTransfer t) {
        // Sender-side event: customerId = sourceCustomerId (drives FUNDS_SENT push)
        publishTransferEvent(TOPIC_TRANSFER_COMPLETED, "TRANSFER_COMPLETED", t,
                t.getSourceCustomerId());
        // Receiver-side event: customerId = destinationCustomerId (drives FUNDS_RECEIVED push)
        if (t.getDestinationCustomerId() != null) {
            publishTransferEvent(TOPIC_TRANSFER_RECEIVED, "TRANSFER_RECEIVED", t,
                    t.getDestinationCustomerId());
        }
    }

    @Override
    public void publishTransferFailed(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_FAILED, "TRANSFER_FAILED", t,
                t.getSourceCustomerId());
    }

    @Override
    public void publishTransferReversed(WalletTransfer t) {
        publishTransferEvent(TOPIC_TRANSFER_REVERSED, "TRANSFER_REVERSED", t,
                t.getSourceCustomerId());
    }

    @Override
    public void publishWithdrawalInitiated(WalletWithdrawal w) {
        publishWithdrawalEvent(TOPIC_WITHDRAWAL_INITIATED, "WITHDRAWAL_INITIATED", w);
    }

    @Override
    public void publishWithdrawalCompleted(WalletWithdrawal w) {
        publishWithdrawalEvent(TOPIC_WITHDRAWAL_COMPLETED, "WITHDRAWAL_COMPLETED", w);
    }

    @Override
    public void publishWithdrawalFailed(WalletWithdrawal w) {
        publishWithdrawalEvent(TOPIC_WITHDRAWAL_FAILED, "WITHDRAWAL_FAILED", w);
    }

    @Override
    public void publishWithdrawalCompensated(WalletWithdrawal w) {
        publishWithdrawalEvent(TOPIC_WITHDRAWAL_COMPENSATED, "WITHDRAWAL_COMPENSATED", w);
    }

    private void publishWithdrawalEvent(String topic, String eventType, WalletWithdrawal w) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("withdrawalId", w.getId().toString());
        event.put("withdrawalNumber", w.getWithdrawalNumber());
        event.put("tenantId", w.getTenantId() != null ? w.getTenantId().toString() : null);
        event.put("sourceWalletId", w.getSourceWalletId() != null ? w.getSourceWalletId().toString() : null);
        event.put("sourceCustomerId", w.getSourceCustomerId() != null ? w.getSourceCustomerId().toString() : null);
        event.put("destinationIban", maskIban(w.getDestinationIban()));
        event.put("destinationIbanHash", hashIban(w.getDestinationIban()));
        event.put("destinationBankCode", w.getDestinationBankCode());
        event.put("destinationCountry", w.getDestinationCountry());
        event.put("amount", w.getAmount() != null ? w.getAmount().toString() : null);
        event.put("feeAmount", w.getFeeAmount() != null ? w.getFeeAmount().toString() : null);
        event.put("totalDebit", w.getTotalDebit() != null ? w.getTotalDebit().toString() : null);
        event.put("currency", w.getCurrency());
        event.put("channel", w.getChannel() != null ? w.getChannel().name() : null);
        event.put("status", w.getStatus() != null ? w.getStatus().name() : null);
        // ISO 20022 fields (for downstream compliance + reconciliation)
        event.put("purposeCode", w.getPurposeCode() != null ? w.getPurposeCode().name() : null);
        event.put("chargeBearer", w.getChargeBearer() != null ? w.getChargeBearer().name() : null);
        event.put("serviceLevel", w.getServiceLevel() != null ? w.getServiceLevel().name() : null);
        event.put("endToEndId", w.getEndToEndId());
        event.put("uetr", w.getUetr() != null ? w.getUetr().toString() : null);
        event.put("instructionId", w.getInstructionId());
        // AML screening (for downstream STR/CTR engine)
        event.put("screeningRef", w.getScreeningRef());
        event.put("screeningDecision", w.getScreeningDecision());
        event.put("screeningScore", w.getScreeningScore());
        event.put("eddRequired", w.isEddRequired());
        // Bank settlement
        event.put("bankReference", w.getBankReference());
        event.put("sarieReference", w.getSarieReference());
        event.put("errorCode", w.getErrorCode());
        event.put("timestamp", Instant.now().toString());

        log.info("Publishing {} event topic={} withdrawalId={}", eventType, topic, w.getId());
        kafkaTemplate.send(topic, w.getId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish {} event withdrawalId={}", eventType, w.getId(), ex);
                    } else {
                        log.debug("Published {} event withdrawalId={} offset={}",
                                eventType, w.getId(), result.getRecordMetadata().offset());
                    }
                });
    }

    private String maskIban(String iban) {
        if (iban == null || iban.length() < 8) return null;
        return iban.substring(0, 4) + "..." + iban.substring(iban.length() - 4);
    }

    /** SHA-256 hash for compliance lookup without exposing raw IBAN (PDPL). */
    private String hashIban(String iban) {
        if (iban == null) return null;
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(iban.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception ex) {
            log.warn("Failed to hash IBAN: {}", ex.getMessage());
            return null;
        }
    }

    private void publishTransferEvent(String topic, String eventType, WalletTransfer t,
                                      java.util.UUID notificationCustomerId) {
        Map<String, Object> event = new HashMap<>();
        event.put("eventType", eventType);
        event.put("transferId", t.getId().toString());
        event.put("transferNumber", t.getTransferNumber());
        event.put("tenantId", t.getTenantId() != null ? t.getTenantId().toString() : null);
        // customerId is the notification recipient (sender for *.completed, receiver for *.received)
        event.put("customerId", notificationCustomerId != null ? notificationCustomerId.toString() : null);
        event.put("sourceCustomerId", t.getSourceCustomerId() != null ? t.getSourceCustomerId().toString() : null);
        event.put("destinationCustomerId", t.getDestinationCustomerId() != null ? t.getDestinationCustomerId().toString() : null);
        event.put("sourceWalletId", t.getSourceWalletId() != null ? t.getSourceWalletId().toString() : null);
        event.put("destinationWalletId", t.getDestinationWalletId() != null ? t.getDestinationWalletId().toString() : null);
        event.put("senderMaskedName", t.getSenderMaskedName());
        event.put("recipientMaskedName", t.getRecipientMaskedName());
        event.put("purposeNote", t.getPurposeNote());
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
