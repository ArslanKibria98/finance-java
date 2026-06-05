package com.ksa.financing.piivault.domain.port.out;

import com.ksa.financing.piivault.domain.model.PiiAccessAudit;

public interface AuditRepository {
    PiiAccessAudit save(PiiAccessAudit audit);
}
