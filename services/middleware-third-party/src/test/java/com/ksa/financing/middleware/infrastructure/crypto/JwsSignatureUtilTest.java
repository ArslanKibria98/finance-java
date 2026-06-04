package com.ksa.financing.middleware.infrastructure.crypto;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;

class JwsSignatureUtilTest {

    @Test
    void detachedSignature_isVerifiableWithMatchingPublicKey() throws Exception {
        KeyPair keyPair = generateRsaKeyPair();
        String pkcs8Pem = toPkcs8Pem((RSAPrivateKey) keyPair.getPrivate());
        String body = "{\"data\":{\"submission_id\":\"1000000001\"}}";

        String detached = JwsSignatureUtil.detachedRs256(body, pkcs8Pem);

        // Detached form is "header..signature" (empty payload section).
        assertThat(detached.split("\\.", -1)).hasSize(3);
        assertThat(detached).contains("..");

        // Verify the detached signature by supplying the external payload to the parser.
        JWSObject parsed = JWSObject.parse(detached, new Payload(body));
        assertThat(parsed.verify(new RSASSAVerifier((RSAPublicKey) keyPair.getPublic()))).isTrue();
    }

    private KeyPair generateRsaKeyPair() throws Exception {
        var gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        return gen.generateKeyPair();
    }

    private String toPkcs8Pem(RSAPrivateKey key) {
        // getEncoded() already returns PKCS#8 DER for a JCA RSA private key.
        String base64 = Base64.getEncoder().encodeToString(key.getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + base64 + "\n-----END PRIVATE KEY-----";
    }
}
