package com.demo.islamic.compliance.sama;

import com.demo.islamic.compliance.config.ComplianceConfig;
import com.demo.islamic.compliance.exception.DataResidencyViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.util.List;
import java.util.Set;

/**
 * Ensures data residency compliance as per SAMA requirements
 * All customer PII, financial records, and transaction data must remain within KSA borders
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SamaDataResidency {

    private final ComplianceConfig complianceConfig;
    private final SamaAuditLogger auditLogger;

    // KSA data center regions
    private static final Set<String> KSA_REGIONS = Set.of(
        "riyadh", "jeddah", "dammam",
        "sa-central-1", "me-south-1", // Cloud provider regions
        "ksa-1", "ksa-2" // Private cloud regions
    );

    // Approved KSA IP ranges (examples - should be configured)
    private static final List<String> KSA_IP_RANGES = List.of(
        "46.0.0.0/8",     // Example KSA IP range
        "185.0.0.0/8",    // Example KSA IP range
        "10.0.0.0/8"      // Private network
    );

    /**
     * Validate that data storage location is within KSA
     */
    public void validateStorageLocation(String region) {
        if (!isValidKsaRegion(region)) {
            String message = String.format(
                "Data residency violation: Storage region '%s' is not within KSA borders", region
            );

            log.error(message);
            auditLogger.logComplianceViolation(
                "SYSTEM",
                "DATA_RESIDENCY",
                message,
                "CRITICAL"
            );

            throw new DataResidencyViolationException(message);
        }

        log.debug("Storage location validated: region={}", region);
    }

    /**
     * Validate that data processing occurs within KSA
     */
    public void validateProcessingLocation(String serverAddress) {
        try {
            InetAddress address = InetAddress.getByName(serverAddress);
            String ipAddress = address.getHostAddress();

            if (!isKsaIpAddress(ipAddress)) {
                String message = String.format(
                    "Data residency violation: Processing server '%s' (%s) is not within KSA",
                    serverAddress, ipAddress
                );

                log.error(message);
                auditLogger.logComplianceViolation(
                    "SYSTEM",
                    "DATA_RESIDENCY",
                    message,
                    "CRITICAL"
                );

                throw new DataResidencyViolationException(message);
            }

            log.debug("Processing location validated: server={}, ip={}",
                serverAddress, ipAddress);
        } catch (Exception e) {
            log.error("Failed to validate processing location: {}", serverAddress, e);
            throw new DataResidencyViolationException(
                "Unable to validate processing location: " + serverAddress
            );
        }
    }

    /**
     * Validate cross-border data transfer request
     */
    public boolean validateCrossBorderTransfer(
        String destinationCountry, String dataType, boolean hasConsent) {

        // Check if transfer is to KSA (allowed)
        if ("SA".equalsIgnoreCase(destinationCountry) ||
            "SAU".equalsIgnoreCase(destinationCountry) ||
            "Saudi Arabia".equalsIgnoreCase(destinationCountry)) {
            return true;
        }

        // Check if data type is restricted
        if (isRestrictedDataType(dataType)) {
            log.error("Cross-border transfer blocked: Restricted data type '{}' to '{}'",
                dataType, destinationCountry);

            auditLogger.logComplianceViolation(
                "SYSTEM",
                "CROSS_BORDER_TRANSFER",
                String.format("Attempted transfer of %s to %s", dataType, destinationCountry),
                "HIGH"
            );

            return false;
        }

        // Check if user consent exists
        if (!hasConsent) {
            log.warn("Cross-border transfer requires consent: dataType={}, destination={}",
                dataType, destinationCountry);
            return false;
        }

        // Check if destination country is approved by SAMA
        if (!isApprovedDestination(destinationCountry)) {
            log.error("Cross-border transfer to non-approved country: {}",
                destinationCountry);

            auditLogger.logComplianceViolation(
                "SYSTEM",
                "CROSS_BORDER_TRANSFER",
                String.format("Transfer to non-approved country: %s", destinationCountry),
                "MEDIUM"
            );

            return false;
        }

        log.info("Cross-border transfer approved: dataType={}, destination={}",
            dataType, destinationCountry);

        return true;
    }

    /**
     * Validate backup location is within KSA
     */
    public void validateBackupLocation(String backupRegion) {
        if (!isValidKsaRegion(backupRegion)) {
            String message = String.format(
                "Backup location '%s' violates data residency requirements", backupRegion
            );

            log.error(message);
            auditLogger.logComplianceViolation(
                "SYSTEM",
                "BACKUP_RESIDENCY",
                message,
                "HIGH"
            );

            throw new DataResidencyViolationException(message);
        }

        log.debug("Backup location validated: region={}", backupRegion);
    }

    /**
     * Validate disaster recovery site is within KSA
     */
    public void validateDrSiteLocation(String drRegion) {
        if (!isValidKsaRegion(drRegion)) {
            String message = String.format(
                "DR site location '%s' violates data residency requirements", drRegion
            );

            log.error(message);
            auditLogger.logComplianceViolation(
                "SYSTEM",
                "DR_RESIDENCY",
                message,
                "HIGH"
            );

            throw new DataResidencyViolationException(message);
        }

        // Ensure DR site is in different city than primary
        String primaryRegion = complianceConfig.getPrimaryDataCenter();
        if (primaryRegion != null && primaryRegion.equals(drRegion)) {
            log.warn("DR site should be in different location than primary: primary={}, dr={}",
                primaryRegion, drRegion);
        }

        log.debug("DR site location validated: region={}", drRegion);
    }

    /**
     * Check if region is within KSA borders
     */
    private boolean isValidKsaRegion(String region) {
        if (region == null) {
            return false;
        }

        String normalizedRegion = region.toLowerCase().trim();
        return KSA_REGIONS.contains(normalizedRegion) ||
               normalizedRegion.startsWith("ksa-") ||
               normalizedRegion.contains("saudi") ||
               normalizedRegion.contains("riyadh") ||
               normalizedRegion.contains("jeddah") ||
               normalizedRegion.contains("dammam");
    }

    /**
     * Check if IP address is within KSA
     */
    private boolean isKsaIpAddress(String ipAddress) {
        // In production, this would check against actual KSA IP ranges
        // from CITC (Communications and Information Technology Commission)

        for (String range : KSA_IP_RANGES) {
            if (isIpInRange(ipAddress, range)) {
                return true;
            }
        }

        // Check against configured allowed IPs
        List<String> allowedIps = complianceConfig.getAllowedProcessingIps();
        if (allowedIps != null && allowedIps.contains(ipAddress)) {
            return true;
        }

        return false;
    }

    /**
     * Check if IP is within CIDR range
     */
    private boolean isIpInRange(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            String rangeIp = parts[0];
            int prefixLength = parts.length > 1 ? Integer.parseInt(parts[1]) : 32;

            InetAddress targetAddr = InetAddress.getByName(ip);
            InetAddress rangeAddr = InetAddress.getByName(rangeIp);

            byte[] targetBytes = targetAddr.getAddress();
            byte[] rangeBytes = rangeAddr.getAddress();

            int bytesToCheck = prefixLength / 8;
            int bitsToCheck = prefixLength % 8;

            for (int i = 0; i < bytesToCheck; i++) {
                if (targetBytes[i] != rangeBytes[i]) {
                    return false;
                }
            }

            if (bitsToCheck > 0 && bytesToCheck < targetBytes.length) {
                int mask = 0xFF << (8 - bitsToCheck);
                return (targetBytes[bytesToCheck] & mask) == (rangeBytes[bytesToCheck] & mask);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to check IP range: ip={}, cidr={}", ip, cidr, e);
            return false;
        }
    }

    /**
     * Check if data type is restricted from cross-border transfer
     */
    private boolean isRestrictedDataType(String dataType) {
        Set<String> restrictedTypes = Set.of(
            "NATIONAL_ID",
            "IQAMA",
            "FINANCIAL_RECORDS",
            "TRANSACTION_DATA",
            "CREDIT_INFORMATION",
            "SIMAH_DATA",
            "LOAN_DETAILS",
            "PAYMENT_HISTORY",
            "BANK_ACCOUNTS"
        );

        return restrictedTypes.contains(dataType.toUpperCase());
    }

    /**
     * Check if destination country is approved for data transfer
     */
    private boolean isApprovedDestination(String country) {
        // Countries with adequate data protection as per SAMA
        Set<String> approvedCountries = Set.of(
            "AE", "UAE",     // UAE
            "KW", "KUWAIT",  // Kuwait
            "QA", "QATAR",   // Qatar
            "BH", "BAHRAIN", // Bahrain
            "OM", "OMAN",    // Oman
            "EG", "EGYPT",   // Egypt
            "JO", "JORDAN"   // Jordan
        );

        return approvedCountries.contains(country.toUpperCase());
    }

    /**
     * Generate data residency compliance report
     */
    public DataResidencyReport generateComplianceReport() {
        DataResidencyReport report = new DataResidencyReport();

        report.setPrimaryDataCenter(complianceConfig.getPrimaryDataCenter());
        report.setBackupDataCenter(complianceConfig.getBackupDataCenter());
        report.setDrDataCenter(complianceConfig.getDrDataCenter());

        // Validate all locations
        boolean primaryCompliant = isValidKsaRegion(report.getPrimaryDataCenter());
        boolean backupCompliant = isValidKsaRegion(report.getBackupDataCenter());
        boolean drCompliant = isValidKsaRegion(report.getDrDataCenter());

        report.setPrimaryCompliant(primaryCompliant);
        report.setBackupCompliant(backupCompliant);
        report.setDrCompliant(drCompliant);
        report.setFullyCompliant(primaryCompliant && backupCompliant && drCompliant);

        if (!report.isFullyCompliant()) {
            report.addViolation("Data centers not fully within KSA borders");
        }

        log.info("Data residency compliance report generated: compliant={}",
            report.isFullyCompliant());

        return report;
    }

    /**
     * Data residency compliance report
     */
    public static class DataResidencyReport {
        private String primaryDataCenter;
        private String backupDataCenter;
        private String drDataCenter;
        private boolean primaryCompliant;
        private boolean backupCompliant;
        private boolean drCompliant;
        private boolean fullyCompliant;
        private List<String> violations = new ArrayList<>();

        // Getters and setters omitted for brevity
        public void addViolation(String violation) {
            this.violations.add(violation);
        }
    }
}