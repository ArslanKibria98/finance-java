package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.CifStatus;

public interface CifLookupCheck {

    CifLookupResult lookup(CifLookupInput input);

    record CifLookupInput(String nidHash) {}

    record CifLookupResult(CifStatus status, String customerId, String failureReason) {}
}
