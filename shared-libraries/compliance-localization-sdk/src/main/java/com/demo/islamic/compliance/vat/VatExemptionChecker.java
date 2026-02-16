package com.demo.islamic.compliance.vat;

import com.demo.islamic.compliance.config.ComplianceConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Checks if transactions or products are VAT-exempt
 * Based on KSA VAT regulations and Islamic finance principles
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VatExemptionChecker {

    private final ComplianceConfig complianceConfig;

    // VAT-exempt product types
    private static final Set<String> EXEMPT_PRODUCT_TYPES = Set.of(
        "EDUCATION_FINANCE",      // Educational loans
        "HEALTHCARE_FINANCE",     // Medical financing
        "EXPORT_FINANCE",        // Export-related financing
        "GOVERNMENT_FINANCE"     // Government sector financing
    );

    // VAT-exempt customer types
    private static final Set<String> EXEMPT_CUSTOMER_TYPES = Set.of(
        "DIPLOMATIC",            // Diplomatic entities
        "GOVERNMENT",           // Government entities
        "CHARITY",              // Registered charities
        "EDUCATIONAL",          // Educational institutions
        "HEALTHCARE"            // Healthcare providers
    );

    // VAT-exempt transaction types
    private static final Set<String> EXEMPT_TRANSACTION_TYPES = Set.of(
        "ZAKAT_PAYMENT",         // Zakat payments
        "CHARITY_DONATION",      // Charity donations
        "INSURANCE_CLAIM",       // Takaful claims
        "EXPORT_TRANSACTION",    // Export transactions
        "INTERNATIONAL_TRANSFER" // International transfers (0-rated)
    );

    private static final Map<String, String> EXEMPTION_REASONS = new HashMap<>();

    static {
        // Initialize exemption reasons
        EXEMPTION_REASONS.put("EDUCATION_FINANCE", "Educational services are VAT-exempt under KSA regulations");
        EXEMPTION_REASONS.put("HEALTHCARE_FINANCE", "Healthcare services are VAT-exempt under KSA regulations");
        EXEMPTION_REASONS.put("EXPORT_FINANCE", "Export transactions are zero-rated for VAT");
        EXEMPTION_REASONS.put("GOVERNMENT_FINANCE", "Government transactions are VAT-exempt");
        EXEMPTION_REASONS.put("DIPLOMATIC", "Diplomatic entities are exempt from VAT");
        EXEMPTION_REASONS.put("CHARITY", "Registered charities are VAT-exempt");
        EXEMPTION_REASONS.put("ZAKAT_PAYMENT", "Zakat is exempt from VAT as religious obligation");
        EXEMPTION_REASONS.put("INTERNATIONAL_TRANSFER", "International financial services are zero-rated");
    }

    /**
     * Check if a product type is VAT-exempt
     */
    public boolean isExempt(String productType) {
        if (productType == null) {
            return false;
        }

        boolean isExempt = EXEMPT_PRODUCT_TYPES.contains(productType.toUpperCase());

        if (isExempt) {
            log.debug("Product type is VAT-exempt: {}", productType);
        }

        return isExempt;
    }

    /**
     * Check if a transaction is VAT-exempt based on multiple criteria
     */
    public boolean isTransactionExempt(
        String productType, String customerType, String transactionType) {

        // Check product exemption
        if (isExempt(productType)) {
            log.info("Transaction exempt due to product type: {}", productType);
            return true;
        }

        // Check customer exemption
        if (isCustomerExempt(customerType)) {
            log.info("Transaction exempt due to customer type: {}", customerType);
            return true;
        }

        // Check transaction type exemption
        if (isTransactionTypeExempt(transactionType)) {
            log.info("Transaction exempt due to transaction type: {}", transactionType);
            return true;
        }

        return false;
    }

    /**
     * Check if a customer type is VAT-exempt
     */
    public boolean isCustomerExempt(String customerType) {
        if (customerType == null) {
            return false;
        }

        return EXEMPT_CUSTOMER_TYPES.contains(customerType.toUpperCase());
    }

    /**
     * Check if a transaction type is VAT-exempt
     */
    public boolean isTransactionTypeExempt(String transactionType) {
        if (transactionType == null) {
            return false;
        }

        return EXEMPT_TRANSACTION_TYPES.contains(transactionType.toUpperCase());
    }

    /**
     * Get exemption reason for a product type
     */
    public String getExemptionReason(String productType) {
        if (productType == null) {
            return null;
        }

        String reason = EXEMPTION_REASONS.get(productType.toUpperCase());

        if (reason == null) {
            // Check custom exemptions from configuration
            Map<String, String> customExemptions = complianceConfig.getCustomVatExemptions();
            if (customExemptions != null) {
                reason = customExemptions.get(productType);
            }
        }

        return reason != null ? reason : "VAT exemption applied";
    }

    /**
     * Check if a specific fee type is VAT-exempt
     */
    public boolean isFeeExempt(String feeType) {
        // In KSA, most fees are VATable
        // Only specific government fees might be exempt

        Set<String> exemptFees = Set.of(
            "GOVERNMENT_FEE",
            "COURT_FEE",
            "REGULATORY_FEE"
        );

        return feeType != null && exemptFees.contains(feeType.toUpperCase());
    }

    /**
     * Determine VAT treatment for cross-border transactions
     */
    public VatTreatment getCrossBorderTreatment(
        String destinationCountry, String serviceType) {

        // GCC countries - standard VAT applies
        Set<String> gccCountries = Set.of("AE", "BH", "KW", "OM", "QA");

        if (gccCountries.contains(destinationCountry)) {
            return VatTreatment.STANDARD_RATE;
        }

        // Export of services - zero-rated
        if (isExportService(serviceType)) {
            return VatTreatment.ZERO_RATED;
        }

        // International financial services - typically zero-rated
        return VatTreatment.ZERO_RATED;
    }

    /**
     * Check if service is considered export for VAT purposes
     */
    private boolean isExportService(String serviceType) {
        Set<String> exportServices = Set.of(
            "EXPORT_FINANCE",
            "TRADE_FINANCE",
            "LETTER_OF_CREDIT",
            "INTERNATIONAL_GUARANTEE"
        );

        return serviceType != null && exportServices.contains(serviceType.toUpperCase());
    }

    /**
     * VAT treatment types
     */
    public enum VatTreatment {
        STANDARD_RATE("15% VAT applies"),
        ZERO_RATED("0% VAT - Zero-rated supply"),
        EXEMPT("VAT Exempt - No VAT applies"),
        OUT_OF_SCOPE("Outside VAT scope");

        private final String description;

        VatTreatment(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Validate exemption claim with supporting documentation
     */
    public boolean validateExemptionClaim(
        String claimType, String documentReference) {

        if (claimType == null || documentReference == null) {
            log.warn("Invalid exemption claim: missing type or documentation");
            return false;
        }

        // In production, this would verify against actual documentation
        // For now, we'll do basic validation
        boolean isValid = EXEMPTION_REASONS.containsKey(claimType.toUpperCase())
            && !documentReference.isEmpty();

        if (isValid) {
            log.info("Exemption claim validated: type={}, ref={}",
                claimType, documentReference);
        } else {
            log.warn("Exemption claim validation failed: type={}, ref={}",
                claimType, documentReference);
        }

        return isValid;
    }

    /**
     * Get all available exemption types
     */
    public Map<String, String> getAvailableExemptions() {
        Map<String, String> exemptions = new HashMap<>(EXEMPTION_REASONS);

        // Add custom exemptions from configuration
        Map<String, String> customExemptions = complianceConfig.getCustomVatExemptions();
        if (customExemptions != null) {
            exemptions.putAll(customExemptions);
        }

        return exemptions;
    }
}