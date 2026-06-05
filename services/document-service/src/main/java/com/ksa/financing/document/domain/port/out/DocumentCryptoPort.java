package com.ksa.financing.document.domain.port.out;

/**
 * Output port for envelope encryption of document bytes.
 * Implementation lives in infrastructure (AES-GCM with master key from env).
 */
public interface DocumentCryptoPort {

    /** Encrypt plaintext returning the ciphertext + wrapped data-encryption key + IV. */
    EnvelopeResult encrypt(byte[] plaintext);

    /** Decrypt ciphertext using the same envelope returned at encryption time. */
    byte[] decrypt(byte[] ciphertext, byte[] encryptedKey, byte[] iv);

    /** Hex SHA-256 digest of bytes — used for integrity audits. */
    String sha256Hex(byte[] data);

    record EnvelopeResult(byte[] ciphertext, byte[] encryptedKey, byte[] iv, String algo) {}
}
