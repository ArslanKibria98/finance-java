package com.ksa.financing.risk.domain.model.assessment;

import java.math.BigDecimal;
import java.util.UUID;

public record ScoreBreakdown(
    UUID parameterId,
    String parameterQuestion,
    String category,
    String answerValue,
    String factorCode,
    BigDecimal factorWeight,
    BigDecimal categoryWeight,
    BigDecimal scoreContribution,
    boolean eddExcluded
) {}
