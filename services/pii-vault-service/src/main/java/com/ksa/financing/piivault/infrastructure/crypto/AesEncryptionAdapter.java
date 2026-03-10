package com.ksa.financing.piivault.infrastructure.crypto;

import com.ksa.financing.piivault.domain.port.out.EncryptionPort;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.TechnicalException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption adapter implementing {@link EncryptionPort}.
 * <p>
 * Encryption format: [12-byte IV] + [ciphertext + 16-byte GCM auth tag]
 * <p>
 * Key management:
 * <ul>
 *   <li>Production: reads master key from environment variable {@code PII_VAULT_MASTER_KEY}</li>
 *   <li>Dev/Testing: uses a hardcoded default key if env var is not set</li>
 * </ul>
 * <p>
 * The master key must be exactly 32 bytes (256 bits) when Base64-decoded.
 * Each encryption operation uses a unique random 12-byte IV (nonce).
 */
@Component
public class AesEncryptionAdapter implements EncryptionPort {

    private static final Logger log = LoggerFactory.getLogger(AesEncryptionAdapter.class);

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits

    /**
     * Default Base64-encoded 256-bit key for development/testing ONLY.
     * In production, set PII_VAULT_MASTER_KEY environment variable.
     */
    private static final String DEFAULT_DEV_KEY_BASE64 =
            "ZGV2LW9ubHktMzItYnl0ZS1rZXktZG8tbm90LXVzZSE=";

    @Value("${pii.vault.master-key:#{null}}")
    private String masterKeyConfig;

    private SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();
    private int currentKeyVersion = 1;

    @PostConstruct
    void init() {
        String keySource = resolveMasterKey();
        byte[] keyBytes = Base64.getDecoder().decode(keySource);

        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                    "PII Vault master key must be exactly 32 bytes (256 bits). Got: " + keyBytes.length);
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");

        if (isUsingDefaultKey(keySource)) {
            log.warn("PII Vault is using the DEFAULT DEVELOPMENT KEY. "
                    + "Set PII_VAULT_MASTER_KEY environment variable for production!");
        } else {
            log.info("PII Vault encryption initialized with production master key (version={})", currentKeyVersion);
        }
    }

    @Override
    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            throw new IllegalArgumentException("Cannot encrypt null plaintext");
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Prepend IV to ciphertext: [IV (12 bytes)] + [ciphertext + auth tag]
            ByteBuffer buffer = ByteBuffer.allocate(GCM_IV_LENGTH + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);
            return buffer.array();

        } catch (Exception e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "AES-256-GCM encryption failed", e);
        }
    }

    @Override
    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null || ciphertext.length == 0) {
            throw new IllegalArgumentException("Cannot decrypt null or empty ciphertext");
        }

        try {
            ByteBuffer buffer = ByteBuffer.wrap(ciphertext);

            // Extract IV from the first 12 bytes
            byte[] iv = new byte[GCM_IV_LENGTH];
            buffer.get(iv);

            // Remaining bytes are the ciphertext + auth tag
            byte[] encryptedData = new byte[buffer.remaining()];
            buffer.get(encryptedData);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decrypted = cipher.doFinal(encryptedData);
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            throw new TechnicalException(
                    ErrorCodes.TECHNICAL_ERROR,
                    "AES-256-GCM decryption failed", e);
        }
    }

    @Override
    public int getCurrentKeyVersion() {
        return currentKeyVersion;
    }

    /**
     * Resolves the master key from environment variable, Spring config, or default.
     * Priority: env var > Spring config property > default dev key.
     */
    private String resolveMasterKey() {
        // 1. Environment variable takes highest priority
        String envKey = System.getenv("PII_VAULT_MASTER_KEY");
        if (envKey != null && !envKey.isBlank()) {
            log.debug("Using PII_VAULT_MASTER_KEY from environment variable");
            return envKey;
        }

        // 2. Spring configuration property
        if (masterKeyConfig != null && !masterKeyConfig.isBlank()) {
            log.debug("Using master key from application configuration");
            return masterKeyConfig;
        }

        // 3. Default development key
        log.debug("No master key configured, using default development key");
        return DEFAULT_DEV_KEY_BASE64;
    }

    private boolean isUsingDefaultKey(String keySource) {
        return DEFAULT_DEV_KEY_BASE64.equals(keySource);
    }
}
