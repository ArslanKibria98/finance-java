package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface FraudHistoryCheck {

    FraudHistoryResult check(FraudHistoryInput input);

    record FraudHistoryInput(String nidHash, String mobileHash, String customerId) {}

    record FraudHistoryResult(CheckDecision decision, boolean confirmedFraud, boolean suspectedFraud,
                              int incidentCount, String failureReason) {}
}
