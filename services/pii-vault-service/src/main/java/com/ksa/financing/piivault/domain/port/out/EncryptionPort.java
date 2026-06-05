package com.ksa.financing.piivault.domain.port.out;

public interface EncryptionPort {
    byte[] encrypt(String plaintext);
    String decrypt(byte[] ciphertext);
    int getCurrentKeyVersion();
}
