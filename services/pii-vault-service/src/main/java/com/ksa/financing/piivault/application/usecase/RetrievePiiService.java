package com.ksa.financing.piivault.application.usecase;

import com.ksa.financing.piivault.domain.model.AccessPurpose;
import com.ksa.financing.piivault.domain.model.PiiAccessAudit;
import com.ksa.financing.piivault.domain.model.PiiIndividual;
import com.ksa.financing.piivault.domain.port.in.RetrievePiiUseCase;
import com.ksa.financing.piivault.domain.port.out.AuditRepository;
import com.ksa.financing.piivault.domain.port.out.PiiRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RetrievePiiService implements RetrievePiiUseCase {

    private final PiiRepository piiRepository;
    private final AuditRepository auditRepository;

    public RetrievePiiService(PiiRepository piiRepository, AuditRepository auditRepository) {
        this.piiRepository = piiRepository;
        this.auditRepository = auditRepository;
    }

    @Override
    @Transactional
    public PiiIndividual retrieve(UUID globalUid, UUID accessorId, String accessorRole,
                                   String accessorIp, String accessPurpose, List<String> fields) {
        PiiIndividual pii = piiRepository.findByGlobalUid(globalUid)
            .orElseThrow(() -> new IllegalArgumentException("PII not found for global UID: " + globalUid));

        PiiAccessAudit audit = new PiiAccessAudit();
        audit.setPiiIndividualId(pii.getPiiId());
        audit.setGlobalUid(globalUid);
        audit.setAccessorId(accessorId);
        audit.setAccessorRole(accessorRole);
        audit.setAccessorIp(accessorIp);
        audit.setAccessedFields(fields);
        audit.setAccessOperation("READ");
        audit.setAccessPurpose(AccessPurpose.valueOf(accessPurpose));
        audit.setAccessGranted(true);
        audit.setAccessedAt(Instant.now());
        auditRepository.save(audit);

        return pii;
    }
}
