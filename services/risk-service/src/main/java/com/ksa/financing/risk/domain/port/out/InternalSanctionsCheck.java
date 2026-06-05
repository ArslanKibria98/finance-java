package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CheckDecision;

public interface InternalSanctionsCheck {

    InternalSanctionsResult check(InternalSanctionsInput input);

    record InternalSanctionsInput(String nidHash, String tenantId) {}

    record InternalSanctionsResult(CheckDecision decision, boolean sanctionsMatch,
                                   String matchConfidence, String failureReason) {}
}
