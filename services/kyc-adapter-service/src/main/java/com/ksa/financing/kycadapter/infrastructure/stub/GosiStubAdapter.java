package com.ksa.financing.kycadapter.infrastructure.stub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Stub adapter simulating the GOSI (General Organization for Social Insurance) API
 * for fetching employment and salary information.
 * In production, this would call the actual GOSI government service.
 */
@Component
public class GosiStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(GosiStubAdapter.class);

    /**
     * Fetch salary and employment information for a given national ID.
     * Simulates a 200ms delay and returns mock salary data.
     *
     * @param nationalId     the national ID of the employee
     * @param employerCrNumber the commercial registration number of the employer (optional)
     * @return a map containing salary and employment details
     */
    public Map<String, Object> fetchSalaryInfo(String nationalId, String employerCrNumber) {
        log.info("GOSI stub: Fetching salary info for nationalId={}, employerCR={}",
                maskId(nationalId), employerCrNumber);

        simulateDelay(200);

        Map<String, Object> salaryInfo = new LinkedHashMap<>();
        salaryInfo.put("employerName", "Saudi Aramco");
        salaryInfo.put("employerCrNumber", employerCrNumber != null ? employerCrNumber : "1010000001");
        salaryInfo.put("basicSalary", new BigDecimal("8000.00"));
        salaryInfo.put("housingAllowance", new BigDecimal("2500.00"));
        salaryInfo.put("otherAllowances", new BigDecimal("1500.00"));
        salaryInfo.put("grossSalary", new BigDecimal("12000.00"));
        salaryInfo.put("deductions", new BigDecimal("1200.00"));
        salaryInfo.put("netSalary", new BigDecimal("10800.00"));
        salaryInfo.put("currency", "SAR");
        salaryInfo.put("employmentType", "GOVERNMENT");
        salaryInfo.put("startDate", LocalDate.now().minusYears(3).toString());
        salaryInfo.put("nationalId", nationalId);

        log.info("GOSI stub: Salary info fetched successfully for nationalId={}", maskId(nationalId));
        return salaryInfo;
    }

    private void simulateDelay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("GOSI stub: Delay interrupted");
        }
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
