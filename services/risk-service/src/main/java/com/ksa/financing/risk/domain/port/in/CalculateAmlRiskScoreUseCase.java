package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.aml.AmlRiskScore;
import com.ksa.financing.risk.domain.model.aml.AmlScoringInput;

/**
 * Input port for calculating AML risk scores.
 * Called by REST controller and Temporal activity adapter.
 */
public interface CalculateAmlRiskScoreUseCase {

    AmlRiskScore calculate(AmlScoringInput input);
}
