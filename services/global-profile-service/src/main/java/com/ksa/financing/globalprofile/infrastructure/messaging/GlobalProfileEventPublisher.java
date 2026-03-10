package com.ksa.financing.globalprofile.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import com.ksa.financing.globalprofile.domain.model.GlobalCustomer;
import com.ksa.financing.globalprofile.domain.model.RegionalProfile;
import com.ksa.financing.globalprofile.domain.port.out.EventPublisherPort;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

/**
 * Kafka event publisher for Global Profile domain events.
 * <p>
 * Publishes events to the following topics:
 * <ul>
 *   <li>{@code islamic-financing.global-profile.profile-created} — when a new global customer is created</li>
 *   <li>{@code islamic-financing.global-profile.regional-profile-linked} — when a regional profile is linked</li>
 *   <li>{@code islamic-financing.global-profile.kyc-status-changed} — when a regional KYC status changes</li>
 * </ul>
 * Events use the global UID as the Kafka key for partition ordering.
 */
@Component
@RequiredArgsConstructor
public class GlobalProfileEventPublisher implements EventPublisherPort {

    private static final Logger log = LoggerFactory.getLogger(GlobalProfileEventPublisher.class);

    private static final String TOPIC_PROFILE_CREATED = "islamic-financing.global-profile.profile-created";
    private static final String TOPIC_REGIONAL_PROFILE_LINKED = "islamic-financing.global-profile.regional-profile-linked";
    private static final String TOPIC_KYC_STATUS_CHANGED = "islamic-financing.global-profile.kyc-status-changed";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishGlobalCustomerCreated(GlobalCustomer customer) {
        Map<String, Object> event = Map.of(
                "eventType", "GlobalCustomerCreated",
                "globalUid", customer.getGlobalUid().toString(),
                "customerType", customer.getCustomerType() != null ? customer.getCustomerType().name() : "INDIVIDUAL",
                "primaryCountryCode", customer.getPrimaryCountryCode(),
                "occurredAt", Instant.now().toString()
        );

        publishEvent(TOPIC_PROFILE_CREATED, customer.getGlobalUid().toString(), event);
        log.info("Published GlobalCustomerCreated event for globalUid={}", customer.getGlobalUid());
    }

    @Override
    public void publishRegionalProfileLinked(RegionalProfile profile) {
        Map<String, Object> event = Map.of(
                "eventType", "RegionalProfileLinked",
                "regionalProfileId", profile.getRegionalProfileId().toString(),
                "globalUid", profile.getGlobalUid().toString(),
                "countryCode", profile.getCountryCode(),
                "regionalCifNumber", profile.getRegionalCifNumber(),
                "occurredAt", Instant.now().toString()
        );

        publishEvent(TOPIC_REGIONAL_PROFILE_LINKED, profile.getGlobalUid().toString(), event);
        log.info("Published RegionalProfileLinked event for globalUid={}, country={}",
                profile.getGlobalUid(), profile.getCountryCode());
    }

    @Override
    public void publishKycStatusChanged(RegionalProfile profile, String oldStatus, String newStatus) {
        Map<String, Object> event = Map.of(
                "eventType", "KycStatusChanged",
                "regionalProfileId", profile.getRegionalProfileId().toString(),
                "globalUid", profile.getGlobalUid().toString(),
                "countryCode", profile.getCountryCode(),
                "oldStatus", oldStatus,
                "newStatus", newStatus,
                "occurredAt", Instant.now().toString()
        );

        publishEvent(TOPIC_KYC_STATUS_CHANGED, profile.getGlobalUid().toString(), event);
        log.info("Published KycStatusChanged event for globalUid={}, {} -> {}",
                profile.getGlobalUid(), oldStatus, newStatus);
    }

    private void publishEvent(String topic, String key, Map<String, Object> event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, key, payload)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("Failed to publish event to topic={}, key={}: {}",
                                    topic, key, ex.getMessage(), ex);
                        } else {
                            log.debug("Event published to topic={}, partition={}, offset={}",
                                    topic,
                                    result.getRecordMetadata().partition(),
                                    result.getRecordMetadata().offset());
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize event for topic={}, key={}: {}", topic, key, e.getMessage(), e);
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Event serialization failed for topic: " + topic, e);
        }
    }
}
