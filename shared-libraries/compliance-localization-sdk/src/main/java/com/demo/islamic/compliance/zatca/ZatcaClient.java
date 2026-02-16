package com.demo.islamic.compliance.zatca;

import com.demo.islamic.compliance.config.ZatcaConfig;
import com.demo.islamic.compliance.exception.ZatcaException;
import com.demo.islamic.compliance.model.ZatcaInvoice;
import com.demo.islamic.compliance.model.ZatcaInvoiceStatus;
import com.demo.islamic.compliance.model.ZatcaClearanceResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * ZATCA API client for e-invoicing integration
 * Handles invoice submission, clearance, and status checking
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ZatcaClient {

    private final ZatcaConfig config;
    private final ObjectMapper objectMapper;
    private final CloseableHttpClient httpClient;

    static {
        // Add Bouncy Castle provider for cryptographic operations
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * Generate SHA-256 hash for invoice
     * Required for invoice integrity verification
     */
    public String generateInvoiceHash(ZatcaInvoice invoice) {
        try {
            String invoiceData = objectMapper.writeValueAsString(invoice);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(invoiceData.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new ZatcaException("Failed to generate invoice hash", e);
        }
    }

    /**
     * Sign invoice with digital certificate
     * Uses ECDSA or RSA-2048 based on configuration
     */
    public String signInvoice(ZatcaInvoice invoice) {
        try {
            String algorithm = config.getSignatureAlgorithm(); // ECDSA or RSA
            String invoiceHash = generateInvoiceHash(invoice);

            // Load private key from configuration
            byte[] keyBytes = Base64.getDecoder().decode(config.getPrivateKey());
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm.equals("ECDSA") ? "EC" : "RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Sign the invoice hash
            Signature signature = Signature.getInstance(
                algorithm.equals("ECDSA") ? "SHA256withECDSA" : "SHA256withRSA"
            );
            signature.initSign(privateKey);
            signature.update(invoiceHash.getBytes(StandardCharsets.UTF_8));

            byte[] signedData = signature.sign();
            String encodedSignature = Base64.getEncoder().encodeToString(signedData);

            // Set signature on invoice
            invoice.setDigitalSignature(encodedSignature);
            invoice.setInvoiceHash(invoiceHash);

            log.info("Invoice signed successfully with {}: invoiceNumber={}",
                algorithm, invoice.getInvoiceNumber());

            return encodedSignature;
        } catch (Exception e) {
            throw new ZatcaException("Failed to sign invoice", e);
        }
    }

    /**
     * Submit invoice to ZATCA for clearance or reporting
     * B2B invoices (> 1000 SAR) require real-time clearance
     * B2C invoices use batch reporting
     */
    public ZatcaClearanceResponse submitInvoice(ZatcaInvoice invoice) {
        try {
            // Sign the invoice before submission
            signInvoice(invoice);

            // Determine submission type based on invoice amount and type
            boolean requiresClearance = invoice.getTotalAmount().compareTo(config.getClearanceThreshold()) > 0
                && invoice.getInvoiceType().equals("B2B");

            String endpoint = requiresClearance ?
                config.getApiUrl() + "/clearance" :
                config.getApiUrl() + "/reporting";

            HttpPost request = new HttpPost(endpoint);

            // Set headers
            request.setHeader("Accept", "application/json");
            request.setHeader("Content-Type", "application/json");
            request.setHeader("Accept-Version", "V2");
            request.setHeader("Authorization", "Bearer " + config.getApiToken());
            request.setHeader("Clearance-Status", requiresClearance ? "1" : "0");

            // Add certificate headers
            request.setHeader("Certificate", config.getCertificate());
            request.setHeader("Certificate-Hash", generateCertificateHash());

            // Convert invoice to JSON and set as request body
            String invoiceJson = objectMapper.writeValueAsString(invoice);
            request.setEntity(new StringEntity(invoiceJson, ContentType.APPLICATION_JSON));

            log.info("Submitting invoice to ZATCA: invoiceNumber={}, type={}, endpoint={}",
                invoice.getInvoiceNumber(), requiresClearance ? "CLEARANCE" : "REPORTING", endpoint);

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                int statusCode = response.getCode();

                if (statusCode == 200 || statusCode == 202) {
                    ZatcaClearanceResponse clearanceResponse =
                        objectMapper.readValue(responseBody, ZatcaClearanceResponse.class);

                    log.info("Invoice submitted successfully: clearanceStatus={}, uuid={}",
                        clearanceResponse.getClearanceStatus(), clearanceResponse.getInvoiceUuid());

                    return clearanceResponse;
                } else {
                    log.error("ZATCA submission failed: statusCode={}, response={}",
                        statusCode, responseBody);
                    throw new ZatcaException("ZATCA submission failed with status: " + statusCode);
                }
            }
        } catch (ZatcaException e) {
            throw e;
        } catch (Exception e) {
            throw new ZatcaException("Failed to submit invoice to ZATCA", e);
        }
    }

    /**
     * Get invoice clearance status from ZATCA
     */
    public ZatcaInvoiceStatus getInvoiceStatus(String invoiceUuid) {
        try {
            String endpoint = config.getApiUrl() + "/invoices/" + invoiceUuid + "/status";

            HttpGet request = new HttpGet(endpoint);
            request.setHeader("Accept", "application/json");
            request.setHeader("Authorization", "Bearer " + config.getApiToken());

            log.info("Checking invoice status from ZATCA: uuid={}", invoiceUuid);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                int statusCode = response.getCode();

                if (statusCode == 200) {
                    ZatcaInvoiceStatus status =
                        objectMapper.readValue(responseBody, ZatcaInvoiceStatus.class);

                    log.info("Invoice status retrieved: uuid={}, status={}",
                        invoiceUuid, status.getStatus());

                    return status;
                } else {
                    log.error("Failed to get invoice status: statusCode={}, response={}",
                        statusCode, responseBody);
                    throw new ZatcaException("Failed to get invoice status: " + statusCode);
                }
            }
        } catch (ZatcaException e) {
            throw e;
        } catch (Exception e) {
            throw new ZatcaException("Failed to get invoice status from ZATCA", e);
        }
    }

    /**
     * Generate certificate hash for authentication
     */
    private String generateCertificateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(config.getCertificate().getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new ZatcaException("Failed to generate certificate hash", e);
        }
    }

    /**
     * Validate ZATCA response signature
     */
    public boolean validateResponseSignature(String response, String signature) {
        try {
            // Load ZATCA public key
            byte[] keyBytes = Base64.getDecoder().decode(config.getZatcaPublicKey());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);

            // Verify signature
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(response.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = Base64.getDecoder().decode(signature);
            boolean isValid = sig.verify(signatureBytes);

            log.info("ZATCA response signature validation: {}", isValid ? "VALID" : "INVALID");

            return isValid;
        } catch (Exception e) {
            log.error("Failed to validate ZATCA response signature", e);
            return false;
        }
    }
}