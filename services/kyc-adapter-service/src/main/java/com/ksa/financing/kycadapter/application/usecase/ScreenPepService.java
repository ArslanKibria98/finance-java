package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;

import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.domain.port.in.ScreenPepUseCase;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.PepScreeningStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Use case implementation for PEP (Politically Exposed Person) screening.
 * Creates a verification session, calls the PEP stub adapter to screen
 * against global PEP, sanctions, and watchlist databases.
 */
@Service
public class ScreenPepService implements ScreenPepUseCase {

    private static final Logger log = LoggerFactory.getLogger(ScreenPepService.class);

    private final VerificationSessionRepository sessionRepository;
    private final PepScreeningStubAdapter pepScreeningStubAdapter;

    public ScreenPepService(VerificationSessionRepository sessionRepository,
                            PepScreeningStubAdapter pepScreeningStubAdapter) {
        this.sessionRepository = sessionRepository;
        this.pepScreeningStubAdapter = pepScreeningStubAdapter;
    }

    @Override
    @Transactional
    public PepScreeningResult screen(ScreenPepCommand command) {
        log.info("PEP screening requested for tenant={}", command.tenantId());

        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing PEP session for idempotencyKey={}", command.idempotencyKey());
            VerificationSession s = existing.get();
            boolean isPep = s.getResult() == VerificationResult.NOT_VERIFIED;
            double confidence = s.getConfidenceScore() != null ? s.getConfidenceScore().doubleValue() : 0.0;
            String decision = isPep ? "EDD_REQUIRED" : "CLEAR";
            return new PepScreeningResult(s, decision, isPep, confidence, isPep ? 1 : 0, null);
        }

        // Create session
        VerificationSession session = new VerificationSession();
        session.setTenantId(command.tenantId());
        session.setSessionNumber("PEP-" + System.currentTimeMillis());
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

        // Call PEP stub adapter
        Map<String, Object> screeningResult = pepScreeningStubAdapter.screenPep(
                command.fullName(), command.nationalId(), command.nationality(), command.dateOfBirth());

        String decision = (String) screeningResult.get("decision");
        boolean pepDetected = Boolean.TRUE.equals(screeningResult.get("pepDetected"));
        double confidenceScore = screeningResult.get("confidenceScore") != null
                ? ((Number) screeningResult.get("confidenceScore")).doubleValue() : 0.0;
        int matchCount = screeningResult.get("matchCount") != null
                ? ((Number) screeningResult.get("matchCount")).intValue() : 0;

        // Determine verification result
        boolean isNegative = "BLOCK".equals(decision) || "HOLD".equals(decision) || "EDD_REQUIRED".equals(decision);
        session.setStatus(SessionStatus.COMPLETED);
        session.setResult(isNegative ? VerificationResult.NOT_VERIFIED : VerificationResult.VERIFIED);
        session.setConfidenceScore(new BigDecimal(String.valueOf(confidenceScore)));
        session.setCompletedAt(Instant.now());

        VerificationSession saved = sessionRepository.save(session);
        log.info("PEP screening completed: sessionId={}, decision={}, pepDetected={}, confidence={}",
                saved.getId(), decision, pepDetected, confidenceScore);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> matchDetails = (List<Map<String, Object>>) screeningResult.get("matchDetails");
        String matchDetailsJson = matchDetails != null ? matchDetails.toString() : "[]";

        return new PepScreeningResult(saved, decision, pepDetected, confidenceScore, matchCount, matchDetailsJson);
    }
}
