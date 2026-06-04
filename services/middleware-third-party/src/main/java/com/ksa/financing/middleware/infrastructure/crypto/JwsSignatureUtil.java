package com.ksa.financing.middleware.infrastructure.crypto;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.RSASSASigner;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Generates detached RS256 JWS signatures for providers (e.g. Scotiabank EFT)
 * that require an {@code x-jws-signature} header over the request body.
 *
 * <p>The output is a detached-payload JWS in the form {@code BASE64URL(header)..BASE64URL(signature)}
 * (the payload section is empty because the body travels separately as the HTTP body).</p>
 *
 * <p>The signing key must be an RSA private key in PKCS#8 PEM form
 * ({@code -----BEGIN PRIVATE KEY-----}). PKCS#1 ({@code BEGIN RSA PRIVATE KEY})
 * must first be converted via {@code openssl pkcs8 -topk8}.</p>
 */
public final class JwsSignatureUtil {

    private JwsSignatureUtil() {
    }

    /**
     * Produce a detached RS256 JWS ({@code header..signature}) over {@code payload}.
     *
     * @param payload    the exact request body bytes that will be sent to the provider
     * @param pkcs8Pem   RSA private key, PKCS#8 PEM
     * @return the detached JWS compact serialization
     * @throws Exception if the key cannot be parsed or signing fails
     */
    public static String detachedRs256(String payload, String pkcs8Pem) throws Exception {
        RSAPrivateKey privateKey = parsePkcs8(pkcs8Pem);
        var header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .type(JOSEObjectType.JWT)
                .build();
        var jws = new JWSObject(header, new Payload(payload == null ? "" : payload));
        jws.sign(new RSASSASigner(privateKey));
        // serialize(true) omits the payload section -> "header..signature"
        return jws.serialize(true);
    }

    private static RSAPrivateKey parsePkcs8(String pem) throws Exception {
        String normalized = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] der = Base64.getDecoder().decode(normalized);
        var spec = new PKCS8EncodedKeySpec(der);
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
    }
}
