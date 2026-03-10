package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;

import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.domain.port.in.VerifyIdentityUseCase;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.YakeenStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Use case implementation for identity verification via Yakeen.
 * Creates a verification session, calls the Yakeen stub adapter for demographics lookup,
 * and returns the verified identity information.
 */
@Service
public class VerifyIdentityService implements VerifyIdentityUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyIdentityService.class);

    private final VerificationSessionRepository sessionRepository;
    private final YakeenStubAdapter yakeenStubAdapter;

    public VerifyIdentityService(VerificationSessionRepository sessionRepository,
                                 YakeenStubAdapter yakeenStubAdapter) {
        this.sessionRepository = sessionRepository;
        this.yakeenStubAdapter = yakeenStubAdapter;
    }

    @Override
    @Transactional
    public VerifyIdentityResult verify(VerifyIdentityCommand command) {
        log.info("Yakeen identity verification requested for tenant={}", command.tenantId());

        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing Yakeen session for idempotencyKey={}", command.idempotencyKey());
            return new VerifyIdentityResult(existing.get(), Map.of());
        }

        // Create session
        VerificationSession session = new VerificationSession();
        session.setTenantId(command.tenantId());
        session.setSessionNumber("SES-" + System.currentTimeMillis());
        session.setNationalId(NationalId.of(command.nationalId()));
        session.setDateOfBirth(command.dateOfBirth());
        session.setVerificationType(VerificationType.NATIONAL_ID);
        session.setProvider(KycProvider.YAKEEN);
        session.setCountryCode("SAU");
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setIdempotencyKey(command.idempotencyKey());
        session.setInitiatedAt(Instant.now());
        session.setAttemptCount(1);
        session.setMaxAttempts(3);

        // Call Yakeen stub adapter
        Map<String, Object> demographics = yakeenStubAdapter.verifyIdentity(
                command.nationalId(), command.dateOfBirth());

        // Update session with results
        boolean verified = Boolean.TRUE.equals(demographics.get("verified"));
        session.setStatus(SessionStatus.COMPLETED);
        session.setResult(verified ? VerificationResult.VERIFIED : VerificationResult.NOT_VERIFIED);
        session.setConfidenceScore(verified ? new BigDecimal("1.0000") : BigDecimal.ZERO);
        session.setCompletedAt(Instant.now());

        // Populate name fields from demographics
        if (demographics.containsKey("fullNameAr")) {
            session.setFullNameAr((String) demographics.get("fullNameAr"));
        }
        if (demographics.containsKey("fullNameEn")) {
            session.setFullNameEn((String) demographics.get("fullNameEn"));
        }

        VerificationSession saved = sessionRepository.save(session);
        log.info("Yakeen identity verification completed: sessionId={}, result={}",
                saved.getId(), saved.getResult());

        return new VerifyIdentityResult(saved, demographics);
    }
}
