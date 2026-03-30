package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.status.AccountStatus;
import com.ksa.financing.risk.domain.model.status.ComplianceStatus;
import com.ksa.financing.risk.domain.model.status.EntityStatusRecord;

import java.util.List;
import java.util.UUID;

public interface ManageEntityStatusUseCase {

    EntityStatusRecord getCurrentStatus(UUID tenantId, String entityReference);

    List<EntityStatusRecord> getStatusHistory(UUID tenantId, String entityReference);

    EntityStatusRecord updateAccountStatus(UUID tenantId, String entityReference,
                                           AccountStatus newStatus, String reason, UUID changedBy);

    EntityStatusRecord updateComplianceStatus(UUID tenantId, String entityReference,
                                              ComplianceStatus newStatus, String reason, UUID changedBy);
}
