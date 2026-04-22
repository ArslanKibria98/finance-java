package com.ksa.financing.ledger.infrastructure.messaging;

import com.ksa.financing.ledger.domain.model.JournalEntryAggregate;
import com.ksa.financing.ledger.domain.port.out.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Kafka implementation of EventPublisher for ledger domain events.
 * Topics are externalized via env vars — zero hardcoding.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaLedgerEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.entry-posted:financing.ledger.entry.posted}")
    private String entryPostedTopic;

    @Value("${kafka.topics.entry-synced:financing.ledger.entry.synced}")
    private String entrySyncedTopic;

    @Value("${kafka.topics.recon-discrepancy:financing.reconciliation.discrepancy}")
    private String reconDiscrepancyTopic;

    @Override
    public void publish(Object event) {
        try {
            String topic = resolveTopic(event);
            String key = resolveKey(event);

            log.debug("Publishing event type={} to topic={}", event.getClass().getSimpleName(), topic);

            kafkaTemplate.send(topic, key, event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Event published: type={} partition={} offset={}",
                                    event.getClass().getSimpleName(),
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        } else {
                            log.error("Failed to publish event: type={} error={}",
                                    event.getClass().getSimpleName(), ex.getMessage(), ex);
                        }
                    });
        } catch (Exception e) {
            log.error("Error publishing event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
        }
    }

    @Override
    public void publishAll(List<Object> events) {
        events.forEach(this::publish);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String resolveTopic(Object event) {
        return switch (event) {
            case JournalEntryAggregate.JournalEntryPosted ignored -> entryPostedTopic;
            case JournalEntryAggregate.JournalEntrySynced ignored -> entrySyncedTopic;
            case JournalEntryAggregate.SyncFailed ignored -> reconDiscrepancyTopic;
            case JournalEntryAggregate.ReconciliationDiscrepancy ignored -> reconDiscrepancyTopic;
            default -> {
                log.warn("Unknown event type: {} — using default topic", event.getClass().getSimpleName());
                yield entryPostedTopic;
            }
        };
    }

    private String resolveKey(Object event) {
        return switch (event) {
            case JournalEntryAggregate.JournalEntryPosted e -> e.journalEntryId().toString();
            case JournalEntryAggregate.JournalEntrySynced e -> e.journalEntryId().toString();
            case JournalEntryAggregate.SyncFailed e -> e.journalEntryId().toString();
            case JournalEntryAggregate.ReconciliationDiscrepancy e -> e.accountId().toString();
            default -> UUID.randomUUID().toString();
        };
    }
}
