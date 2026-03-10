package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;

import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.domain.port.in.VerifyMobileUseCase;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.TahakukStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Use case implementation for mobile ownership verification via Tahakuk.
 * Creates a verification session, calls the Tahakuk stub adapter,
 * and returns the verification result.
 */
@Service
public class VerifyMobileService implements VerifyMobileUseCase {

    private static final Logger log = LoggerFactory.getLogger(VerifyMobileService.class);

    private final VerificationSessionRepository sessionRepository;
    private final TahakukStubAdapter tahakukStubAdapter;

    public VerifyMobileService(VerificationSessionRepository sessionRepository,
                               TahakukStubAdapter tahakukStubAdapter) {
        this.sessionRepository = sessionRepository;
        this.tahakukStubAdapter = tahakukStubAdapter;
    }

    @Override
    @Transactional
    public VerificationSession verify(VerifyMobileCommand command) {
        log.info("Tahakuk mobile verification requested for tenant={}", command.tenantId());

        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing Tahakuk session for idempotencyKey={}", command.idempotencyKey());
            return existing.get();
        }

        // Create session
        VerificationSession session = new VerificationSession();
        session.setTenantId(command.tenantId());
        session.setSessionNumber("SES-" + System.currentTimeMillis());
        session.setNationalId(NationalId.of(command.nationalId()));
        session.setVerificationType(VerificationType.NATIONAL_ID);
        session.setProvider(KycProvider.YAKEEN);
        session.setCountryCode("SAU");
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setIdempotencyKey(command.idempotencyKey());
        session.setInitiatedAt(Instant.now());
        session.setAttemptCount(1);
        session.setMaxAttempts(3);

        // Call Tahakuk stub adapter
        Map<String, Object> tahakukResult = tahakukStubAdapter.verifyMobileOwnership(
                command.nationalId(), command.mobileNumber());

        boolean verified = Boolean.TRUE.equals(tahakukResult.get("verified"));
        session.setStatus(SessionStatus.COMPLETED);
        session.setResult(verified ? VerificationResult.VERIFIED : VerificationResult.NOT_VERIFIED);
        session.setConfidenceScore(verified ? new BigDecimal("1.0000") : BigDecimal.ZERO);
        session.setCompletedAt(Instant.now());

        VerificationSession saved = sessionRepository.save(session);
        log.info("Tahakuk mobile verification completed: sessionId={}, result={}",
                saved.getId(), saved.getResult());
        return saved;
    }
}
