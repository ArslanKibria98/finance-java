package com.ksa.financing.lending.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ksa.financing.lending.domain.port.out.LoanRepository;
import com.ksa.financing.lending.infrastructure.client.LedgerServiceClient;
import com.ksa.financing.lms.port.LmsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Stub — the previous implementation referenced LoanAggregate#getLmsReferenceId and
 * SettlementIntent builder fields that do not exist in the current SDK/domain model.
 * Kept registered so the Kafka consumer group still exists; body is a log-only no-op
 * until the Fineract settlement integration is rewired.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementEventListener {

    private final LoanRepository loanRepository;
    private final LmsPort lmsPort;
    private final LedgerServiceClient ledgerServiceClient;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.settlement-confirmed:financing.settlement.confirmed}",
                   groupId = "${spring.application.name}")
    public void onSettlementConfirmed(Map<String, Object> event) {
        log.info("SettlementConfirmed received (listener stub, no-op): {}", event);
    }
}
