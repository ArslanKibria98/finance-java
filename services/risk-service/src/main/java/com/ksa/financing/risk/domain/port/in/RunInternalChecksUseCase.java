package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.InternalCheckRequest;
import com.ksa.financing.risk.domain.model.InternalCheckResult;

public interface RunInternalChecksUseCase {

    InternalCheckResult run(InternalCheckRequest request);
}
