package com.ksa.financing.globalprofile.application.usecase;

import com.ksa.financing.globalprofile.domain.model.PiiAccessToken;
import com.ksa.financing.globalprofile.domain.port.in.IssuePiiAccessTokenUseCase;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class IssuePiiAccessTokenService implements IssuePiiAccessTokenUseCase {

    private static final Logger log = LoggerFactory.getLogger(IssuePiiAccessTokenService.class);
    private static final int TOKEN_VALIDITY_SECONDS = 300;

    @Override
    public PiiAccessToken issue(IssuePiiAccessTokenCommand command) {
        log.info("Issuing PII access token for globalUid={}, purpose={}",
                command.globalUid(), command.accessPurpose());

        String rawToken = generateToken();
        String tokenHash = hashToken(rawToken);
        Instant now = Instant.now();

        PiiAccessToken token = new PiiAccessToken();
        token.setTokenId(UUID.randomUUID());
        token.setAccessToken(rawToken);
        token.setTokenHash(tokenHash);
        token.setGlobalUid(command.globalUid());
        token.setAllowedFields(command.allowedFields());
        token.setRequesterId(command.requesterId());
        token.setRequesterRole(command.requesterRole());
        token.setRequesterIp(command.requesterIp());
        token.setAccessPurpose(command.accessPurpose());
        token.setRelatedEntityType(command.relatedEntityType());
        token.setRelatedEntityId(command.relatedEntityId());
        token.setIssuedAt(now);
        token.setExpiresAt(now.plusSeconds(TOKEN_VALIDITY_SECONDS));
        token.setRevoked(false);
        token.setUsedCount(0);

        log.info("PII access token issued: tokenId={}", token.getTokenId());
        return token;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "Failed to hash PII access token", e);
        }
    }
}
