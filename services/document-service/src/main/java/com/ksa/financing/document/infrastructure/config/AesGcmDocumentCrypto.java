package com.ksa.financing.document.infrastructure.config;

import com.ksa.financing.document.domain.port.out.DocumentCryptoPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * AES-GCM envelope crypto: each document gets a fresh 256-bit data-encryption
 * key (DEK) that encrypts the bytes; the DEK is then wrapped with the master
 * key (from {@code document.master-key}) and stored alongside metadata.
 *
 * <p>Master key supplied via env as Base64 (32-byte / 256-bit). For DEV the
 * default is a deterministic dev key — DO NOT use in PROD.</p>
 */
@Slf4j
@Component
public class AesGcmDocumentCrypto implements DocumentCryptoPort {

    private static final String DEK_ALGO = "AES";
    private static final String CIPHER_ALGO = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;
    private static final SecureRandom RNG = new SecureRandom();

    private final SecretKey masterKey;

    public AesGcmDocumentCrypto(@Value("${document.master-key:}") String masterKeyB64) {
        byte[] keyBytes;
        if (masterKeyB64 == null || masterKeyB64.isBlank()) {
            // Dev fallback — deterministic 256-bit key derived from a constant.
            // Production MUST inject document.master-key via env.
            log.warn("document.master-key not set — using DEV-ONLY derived key. Set MINIO/secret in PROD.");
            keyBytes = sha256("ksa-document-service-dev-master-key".getBytes(StandardCharsets.UTF_8));
        } else {
            keyBytes = Base64.getDecoder().decode(masterKeyB64.trim());
            if (keyBytes.length != 32) {
                throw new IllegalStateException(
                        "document.master-key must decode to 32 bytes (256-bit). Got " + keyBytes.length);
            }
        }
        this.masterKey = new SecretKeySpec(keyBytes, DEK_ALGO);
    }

    @Override
    public EnvelopeResult encrypt(byte[] plaintext) {
        try {
            SecretKey dek = KeyGenerator.getInstance(DEK_ALGO).generateKey(); // 128-bit by default
            // Force 256-bit DEK explicitly
            KeyGenerator kg = KeyGenerator.getInstance(DEK_ALGO);
            kg.init(256, RNG);
            dek = kg.generateKey();

            byte[] iv = new byte[IV_BYTES];
            RNG.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.ENCRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);

            byte[] wrappedKey = wrapDek(dek);
            return new EnvelopeResult(ciphertext, wrappedKey, iv, CIPHER_ALGO);
        } catch (Exception e) {
            throw new RuntimeException("Document encryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] decrypt(byte[] ciphertext, byte[] encryptedKey, byte[] iv) {
        try {
            SecretKey dek = unwrapDek(encryptedKey);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGO);
            cipher.init(Cipher.DECRYPT_MODE, dek, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } catch (Exception e) {
            throw new RuntimeException("Document decryption failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String sha256Hex(byte[] data) {
        return HexFormat.of().formatHex(sha256(data));
    }

    private byte[] wrapDek(SecretKey dek) throws Exception {
        // Wrap DEK with master key using AES-GCM (separate fresh IV prefixed)
        byte[] wrapIv = new byte[IV_BYTES];
        RNG.nextBytes(wrapIv);
        Cipher c = Cipher.getInstance(CIPHER_ALGO);
        c.init(Cipher.ENCRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, wrapIv));
        byte[] wrapped = c.doFinal(dek.getEncoded());
        byte[] combined = new byte[wrapIv.length + wrapped.length];
        System.arraycopy(wrapIv, 0, combined, 0, wrapIv.length);
        System.arraycopy(wrapped, 0, combined, wrapIv.length, wrapped.length);
        return combined;
    }

    private SecretKey unwrapDek(byte[] envelope) throws Exception {
        byte[] wrapIv = new byte[IV_BYTES];
        byte[] wrapped = new byte[envelope.length - IV_BYTES];
        System.arraycopy(envelope, 0, wrapIv, 0, IV_BYTES);
        System.arraycopy(envelope, IV_BYTES, wrapped, 0, wrapped.length);
        Cipher c = Cipher.getInstance(CIPHER_ALGO);
        c.init(Cipher.DECRYPT_MODE, masterKey, new GCMParameterSpec(GCM_TAG_BITS, wrapIv));
        byte[] raw = c.doFinal(wrapped);
        return new SecretKeySpec(raw, DEK_ALGO);
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
