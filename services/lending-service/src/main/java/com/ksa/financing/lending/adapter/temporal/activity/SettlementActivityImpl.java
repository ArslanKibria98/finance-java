package com.ksa.financing.lending.adapter.temporal.activity;

import com.ksa.financing.lms.port.LmsPort;
import com.ksa.financing.lms.intent.SettlementIntent;
import com.ksa.financing.lms.dto.SettlementResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class SettlementActivityImpl {

    private final LmsPort lmsPort;

    public SettlementResult processLmsSettlement(UUID tenantId, UUID loanId, SettlementIntent intent) {
        log.info("Processing LMS settlement for loan: {}", loanId);
        return lmsPort.processEarlySettlement(intent);
    }
}
