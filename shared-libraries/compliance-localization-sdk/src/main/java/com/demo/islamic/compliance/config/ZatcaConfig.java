package com.demo.islamic.compliance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

/**
 * ZATCA e-invoicing configuration
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "compliance.zatca")
public class ZatcaConfig {

    /**
     * ZATCA API base URL
     */
    private String apiUrl = "https://gw-apic-gov.gazt.gov.sa/e-invoicing/developer-portal";

    /**
     * ZATCA API authentication token
     */
    private String apiToken;

    /**
     * Organization's certificate for ZATCA
     */
    private String certificate;

    /**
     * Private key for digital signing
     */
    private String privateKey;

    /**
     * ZATCA public key for response verification
     */
    private String zatcaPublicKey;

    /**
     * Digital signature algorithm (ECDSA or RSA)
     */
    private String signatureAlgorithm = "ECDSA";

    /**
     * Organization VAT registration number
     */
    private String vatRegistrationNumber;

    /**
     * Organization commercial registration number
     */
    private String commercialRegistrationNumber;

    /**
     * Organization legal name in Arabic
     */
    private String legalNameArabic;

    /**
     * Organization legal name in English
     */
    private String legalNameEnglish;

    /**
     * Clearance threshold amount (SAR)
     * B2B invoices above this amount require real-time clearance
     */
    private BigDecimal clearanceThreshold = new BigDecimal("1000");

    /**
     * Enable/disable ZATCA integration
     */
    private boolean enabled = true;

    /**
     * Enable sandbox mode for testing
     */
    private boolean sandboxMode = false;

    /**
     * Sandbox API URL
     */
    private String sandboxApiUrl = "https://gw-apic-gov.gazt.gov.sa/e-invoicing/simulation";

    /**
     * Request timeout in seconds
     */
    private int requestTimeout = 30;

    /**
     * Max retry attempts for failed submissions
     */
    private int maxRetryAttempts = 3;

    /**
     * Retry delay in seconds
     */
    private int retryDelaySeconds = 5;

    /**
     * Enable automatic invoice submission
     */
    private boolean autoSubmit = true;

    /**
     * Batch size for reporting mode submissions
     */
    private int batchSize = 100;

    /**
     * Invoice counter starting value
     */
    private long invoiceCounterStart = 1;

    /**
     * QR code generation settings
     */
    private QrCodeConfig qrCode = new QrCodeConfig();

    @Data
    public static class QrCodeConfig {
        private boolean enabled = true;
        private int size = 300;
        private String format = "PNG";
        private int margin = 1;
    }

    /**
     * Get the effective API URL based on sandbox mode
     */
    public String getEffectiveApiUrl() {
        return sandboxMode ? sandboxApiUrl : apiUrl;
    }
}