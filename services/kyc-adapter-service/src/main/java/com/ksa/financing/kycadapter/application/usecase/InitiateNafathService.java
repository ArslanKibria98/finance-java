package com.ksa.financing.kycadapter.application.usecase;

import com.ksa.financing.domain.valueobject.NationalId;

import com.ksa.financing.kycadapter.domain.model.*;
import com.ksa.financing.kycadapter.domain.port.in.InitiateNafathUseCase;
import com.ksa.financing.kycadapter.domain.port.out.ProviderCacheRepository;
import com.ksa.financing.kycadapter.domain.port.out.VerificationSessionRepository;
import com.ksa.financing.kycadapter.infrastructure.stub.NafathStubAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Use case implementation for initiating Nafath identity verification sessions.
 * Creates a verification session, calls the Nafath stub adapter, and persists the session
 * with PENDING_USER_ACTION status. The user must confirm a random number on their Nafath app.
 */
@Service
public class InitiateNafathService implements InitiateNafathUseCase {

    private static final Logger log = LoggerFactory.getLogger(InitiateNafathService.class);
    private static final int NAFATH_SESSION_EXPIRY_MINUTES = 30;
    private static final int NAFATH_CACHE_TTL_HOURS = 24;

    private final VerificationSessionRepository sessionRepository;
    private final ProviderCacheRepository providerCacheRepository;
    private final NafathStubAdapter nafathStubAdapter;

    public InitiateNafathService(VerificationSessionRepository sessionRepository,
                                 ProviderCacheRepository providerCacheRepository,
                                 NafathStubAdapter nafathStubAdapter) {
        this.sessionRepository = sessionRepository;
        this.providerCacheRepository = providerCacheRepository;
        this.nafathStubAdapter = nafathStubAdapter;
    }

    @Override
    @Transactional
    public NafathInitiationResult initiate(InitiateNafathCommand command) {
        log.info("Initiating Nafath session for tenant={}", command.tenantId());

        // Idempotency check
        var existing = sessionRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            log.info("Returning existing Nafath session for idempotencyKey={}", command.idempotencyKey());
            VerificationSession session = existing.get();
            // Retrieve cached Nafath user data
            String cacheKey = "nafath-userdata-" + command.nationalId();
            Map<String, Object> cachedVerificationData = providerCacheRepository.findCachedResponse(
                    command.tenantId(), cacheKey).orElse(null);
            return new NafathInitiationResult(session, 0, session.getProviderSessionId(), cachedVerificationData);
        }

        // Create session with INITIATED status
        VerificationSession session = new VerificationSession();
        session.setTenantId(command.tenantId());
        session.setSessionNumber("SES-" + System.currentTimeMillis());
        session.setNationalId(NationalId.of(command.nationalId()));
        session.setVerificationType(VerificationType.NATIONAL_ID);
        session.setProvider(KycProvider.NAFATH);
        session.setCountryCode("SAU");
        session.setStatus(SessionStatus.INITIATED);
        session.setIdempotencyKey(command.idempotencyKey());
        session.setInitiatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plusSeconds(NAFATH_SESSION_EXPIRY_MINUTES * 60L));
        session.setAttemptCount(1);
        session.setMaxAttempts(3);

        // Call Nafath stub adapter — propagate business context for cost attribution
        Map<String, Object> nafathResponse = nafathStubAdapter.initiateSession(
                command.nationalId(),
                command.customerId(),
                command.applicationId(),
                command.contextType() != null ? command.contextType() : "ONBOARDING");
        String providerSessionId = (String) nafathResponse.get("sessionId");
        // Nafath upstream returns `random` as a JSON string ("42") — parse defensively
        // because the mock and the live provider use different shapes.
        Object randomRaw = nafathResponse.get("randomNumber");
        int randomNumber;
        if (randomRaw instanceof Number n) {
            randomNumber = n.intValue();
        } else if (randomRaw != null && !randomRaw.toString().isBlank()) {
            try {
                randomNumber = Integer.parseInt(randomRaw.toString().trim());
            } catch (NumberFormatException e) {
                log.warn("Nafath returned non-numeric randomNumber: {}", randomRaw);
                randomNumber = 0;
            }
        } else {
            randomNumber = 0;
        }

        session.setProviderSessionId(providerSessionId);
        session.setStatus(SessionStatus.PENDING_USER_ACTION);

        // Fetch user data from Nafath (third API — identity inquiry)
        // Call third Nafath API — fetch verified person demographics
        Map<String, Object> verificationData = nafathStubAdapter.getVerificationResult(
                providerSessionId, command.nationalId(),
                command.customerId(), command.applicationId(),
                command.contextType() != null ? command.contextType() : "ONBOARDING");
        log.info("Nafath verification data fetched for sessionId={}", providerSessionId);

        // Cache the Nafath user data response
        String cacheKey = "nafath-userdata-" + command.nationalId();
        providerCacheRepository.cacheResponse(command.tenantId(), cacheKey, KycProvider.NAFATH,
                command.nationalId(), verificationData, NAFATH_CACHE_TTL_HOURS);

        VerificationSession saved = sessionRepository.save(session);
        log.info("Nafath session initiated: sessionId={}, providerSessionId={}, randomNumber={}",
                saved.getId(), providerSessionId, randomNumber);

        return new NafathInitiationResult(saved, randomNumber, providerSessionId, verificationData);
    }

    @Override
    @Transactional
    public VerificationSession checkStatus(UUID sessionId) {
        log.info("Checking Nafath session status for sessionId={}", sessionId);

        VerificationSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        // If already completed or failed, return as-is
        if (session.getStatus() == SessionStatus.COMPLETED || session.getStatus() == SessionStatus.FAILED) {
            log.info("Nafath session {} already in terminal state: {}", sessionId, session.getStatus());
            return session;
        }

        // Check if session has expired
        if (session.getExpiresAt() != null && Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus(SessionStatus.EXPIRED);
            session.setCompletedAt(Instant.now());
            return sessionRepository.save(session);
        }

        // Call Nafath stub to check provider status
        Map<String, Object> statusResponse = nafathStubAdapter.checkSessionStatus(session.getProviderSessionId());
        String providerStatus = (String) statusResponse.get("status");

        if ("COMPLETED".equals(providerStatus)) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setResult(VerificationResult.VERIFIED);
            session.setUserActionAt(Instant.now());
            session.setCompletedAt(Instant.now());
            log.info("Nafath session {} completed successfully", sessionId);
        }

        return sessionRepository.save(session);
    }
}
