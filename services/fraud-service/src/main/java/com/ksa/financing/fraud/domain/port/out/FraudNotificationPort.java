package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.fraud.FraudAlert;
import com.ksa.financing.fraud.domain.model.fraud.FraudEvaluationResult;

public interface FraudNotificationPort {

    void notifyAlert(FraudAlert alert);

    void notifyBlock(FraudEvaluationResult result);
}
