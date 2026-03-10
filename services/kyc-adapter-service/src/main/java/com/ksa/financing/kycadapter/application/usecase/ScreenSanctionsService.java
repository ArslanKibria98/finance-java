package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;

import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.domain.port.in.ScreenSanctionsUseCase;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.SanctionsStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Use case implementation for AML/sanctions screening.
 * Creates a verification session, calls the sanctions stub adapter to screen
 * against UN Sanctions, SAMA, and local PEP databases.
 */
@Service
public class ScreenSanctionsService implements ScreenSanctionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScreenSanctionsService.class);

    private final VerificationSessionRepository sessionRepository;
    private final SanctionsStubAdapter sanctionsStubAdapter;

    public ScreenSanctionsService(VerificationSessionRepository sessionRepository,
                                  SanctionsStubAdapter sanctionsStubAdapter) {
        this.sessionRepository = sessionRepository;
        this.sanctionsStubAdapter = sanctionsStubAdapter;
    }

    @Override
    @Transactional
    public SanctionsScreeningResult screen(ScreenSanctionsCommand command) {
        log.info("Sanctions screening requested for tenant={}", command.tenantId());

        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing sanctions session for idempotencyKey={}", command.idempotencyKey());
            VerificationSession s = existing.get();
            boolean isHit = s.getResult() == VerificationResult.NOT_VERIFIED;
            return new SanctionsScreeningResult(s, isHit ? "HIT" : "CLEAR", isHit);
        }

        // Create session
        VerificationSession session = new VerificationSession();
        session.setTenantId(command.tenantId());
        session.setSessionNumber("SES-" + System.currentTimeMillis());
        session.setNationalId(NationalId.of(command.nationalId()));
        session.setFullNameEn(command.fullName());
        session.setVerificationType(VerificationType.NATIONAL_ID);
        session.setProvider(KycProvider.MANUAL);
        session.setCountryCode("SAU");
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setIdempotencyKey(command.idempotencyKey());
        session.setInitiatedAt(Instant.now());
        session.setAttemptCount(1);
        session.setMaxAttempts(1);

        // Call sanctions stub adapter
        Map<String, Object> screeningResult = sanctionsStubAdapter.screenIndividual(
                command.fullName(), command.nationalId(), command.nationality());

        String resultStatus = (String) screeningResult.get("result");
        boolean isHit = "HIT".equals(resultStatus);

        // Update session with results
        session.setStatus(SessionStatus.COMPLETED);
        session.setResult(isHit ? VerificationResult.NOT_VERIFIED : VerificationResult.VERIFIED);
        session.setConfidenceScore(isHit ? new BigDecimal("0.9500") : new BigDecimal("1.0000"));
        session.setCompletedAt(Instant.now());

        VerificationSession saved = sessionRepository.save(session);
        log.info("Sanctions screening completed: sessionId={}, result={}, hit={}",
                saved.getId(), resultStatus, isHit);

        return new SanctionsScreeningResult(saved, resultStatus, isHit);
    }
}
