package com.demo.islamic.compliance.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * General compliance configuration for SAMA, VAT, and localization
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "compliance")
public class ComplianceConfig {

    /**
     * Institution/Organization ID for regulatory reporting
     */
    private String institutionId;

    /**
     * Current tenant ID for multi-tenant setup
     */
    private String tenantId;

    /**
     * Service name for audit logging
     */
    private String serviceName = "islamic-financing-platform";

    /**
     * Environment (development, staging, production)
     */
    private String environment = "development";

    /**
     * SAMA compliance settings
     */
    private SamaConfig sama = new SamaConfig();

    /**
     * VAT configuration
     */
    private VatConfig vat = new VatConfig();

    /**
     * Data residency configuration
     */
    private DataResidencyConfig dataResidency = new DataResidencyConfig();

    /**
     * Localization settings
     */
    private LocalizationConfig localization = new LocalizationConfig();

    /**
     * Audit configuration
     */
    private AuditConfig audit = new AuditConfig();

    @Data
    public static class SamaConfig {
        /**
         * SAMA API endpoint
         */
        private String apiUrl = "https://api.sama.gov.sa";

        /**
         * SAMA API credentials
         */
        private String apiKey;
        private String apiSecret;

        /**
         * Enable SAMA compliance checks
         */
        private boolean enabled = true;

        /**
         * Reporting frequency (daily, weekly, monthly)
         */
        private String reportingFrequency = "monthly";

        /**
         * NPL threshold percentage
         */
        private BigDecimal nplThreshold = new BigDecimal("5.0");

        /**
         * Provision coverage ratio requirement
         */
        private BigDecimal requiredProvisionCoverage = new BigDecimal("100.0");

        /**
         * Capital adequacy ratio requirement
         */
        private BigDecimal requiredCapitalAdequacy = new BigDecimal("12.5");
    }

    @Data
    public static class VatConfig {
        /**
         * Enable VAT calculation
         */
        private boolean enabled = true;

        /**
         * Custom VAT rate (if different from standard 15%)
         */
        private BigDecimal customVatRate;

        /**
         * Custom VAT exemptions
         */
        private Map<String, String> customExemptions;

        /**
         * VAT registration number
         */
        private String vatNumber;

        /**
         * Tax accounting method (accrual or cash)
         */
        private String accountingMethod = "accrual";
    }

    @Data
    public static class DataResidencyConfig {
        /**
         * Primary data center location
         */
        private String primaryDataCenter = "riyadh";

        /**
         * Backup data center location
         */
        private String backupDataCenter = "jeddah";

        /**
         * DR data center location
         */
        private String drDataCenter = "dammam";

        /**
         * Allowed processing IP addresses
         */
        private List<String> allowedProcessingIps;

        /**
         * Enable cross-border transfer validation
         */
        private boolean validateCrossBorder = true;

        /**
         * Approved countries for data transfer
         */
        private List<String> approvedCountries;
    }

    @Data
    public static class LocalizationConfig {
        /**
         * Default language (ar, en)
         */
        private String defaultLanguage = "ar";

        /**
         * Supported languages
         */
        private List<String> supportedLanguages = List.of("ar", "en");

        /**
         * Default currency
         */
        private String defaultCurrency = "SAR";

        /**
         * Date format for display
         */
        private String dateFormat = "dd/MM/yyyy";

        /**
         * Time zone
         */
        private String timeZone = "Asia/Riyadh";

        /**
         * Enable Hijri calendar
         */
        private boolean hijriCalendarEnabled = true;

        /**
         * Show both Hijri and Gregorian dates
         */
        private boolean showBothCalendars = true;

        /**
         * Number format locale
         */
        private String numberLocale = "ar-SA";
    }

    @Data
    public static class AuditConfig {
        /**
         * Audit log retention period in years
         */
        private int retentionYears = 7;

        /**
         * Hash salt for audit log integrity
         */
        private String auditHashSalt;

        /**
         * Enable immutable audit logging
         */
        private boolean immutableLogging = true;

        /**
         * Audit log storage type (database, file, both)
         */
        private String storageType = "database";

        /**
         * External audit log service URL (if applicable)
         */
        private String externalAuditUrl;

        /**
         * Enable real-time audit streaming
         */
        private boolean realtimeStreaming = false;

        /**
         * Kafka topic for audit events (if streaming enabled)
         */
        private String kafkaTopic = "audit-events";
    }

    // Helper methods

    public boolean isVatEnabled() {
        return vat != null && vat.isEnabled();
    }

    public BigDecimal getCustomVatRate() {
        return vat != null ? vat.getCustomVatRate() : null;
    }

    public Map<String, String> getCustomVatExemptions() {
        return vat != null ? vat.getCustomExemptions() : null;
    }

    public String getPrimaryDataCenter() {
        return dataResidency != null ? dataResidency.getPrimaryDataCenter() : null;
    }

    public String getBackupDataCenter() {
        return dataResidency != null ? dataResidency.getBackupDataCenter() : null;
    }

    public String getDrDataCenter() {
        return dataResidency != null ? dataResidency.getDrDataCenter() : null;
    }

    public List<String> getAllowedProcessingIps() {
        return dataResidency != null ? dataResidency.getAllowedProcessingIps() : null;
    }

    public String getAuditHashSalt() {
        return audit != null ? audit.getAuditHashSalt() : null;
    }
}