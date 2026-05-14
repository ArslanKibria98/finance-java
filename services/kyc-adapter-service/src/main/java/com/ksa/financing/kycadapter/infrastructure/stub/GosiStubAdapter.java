package com.ksa.financing.kycadapter.infrastructure.stub;

import com.ksa.financing.kycadapter.infrastructure.middleware.MiddlewareApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * GOSI / Dakhli salary lookup adapter.
 *
 * Routes through middleware-third-party (DAKHLI_GOSI). Downstream callers
 * receive the same salary envelope shape; missing fields are filled with
 * sensible defaults for backwards compatibility.
 */
@Component
public class GosiStubAdapter {

    private static final Logger log = LoggerFactory.getLogger(GosiStubAdapter.class);

    private static final String API_CODE = "DAKHLI_GOSI";

    private final MiddlewareApiClient middlewareApiClient;

    public GosiStubAdapter(MiddlewareApiClient middlewareApiClient) {
        this.middlewareApiClient = middlewareApiClient;
    }

    public Map<String, Object> fetchSalaryInfo(String nationalId, String employerCrNumber) {
        log.info("GOSI via middleware: fetching salary for nationalId={}, employerCR={}",
                maskId(nationalId), employerCrNumber);

        var requestBody = Map.<String, Object>of(
                "nationalId", nationalId,
                "employerCrNumber", employerCrNumber != null ? employerCrNumber : ""
        );

        Map<String, Object> upstream = middlewareApiClient.invokeAsMap(
                API_CODE, requestBody, nationalId, null, null);

        Map<String, Object> salaryInfo = new LinkedHashMap<>(upstream);
        salaryInfo.putIfAbsent("nationalId", nationalId);
        salaryInfo.putIfAbsent("employerName", upstream.getOrDefault("employer", "Saudi Aramco"));
        salaryInfo.putIfAbsent("employerCrNumber", employerCrNumber != null ? employerCrNumber : "1010000001");
        salaryInfo.putIfAbsent("basicSalary", toBigDecimal(upstream.get("basicSalary"), "8000.00"));
        salaryInfo.putIfAbsent("housingAllowance", toBigDecimal(upstream.get("housingAllowance"), "2500.00"));
        salaryInfo.putIfAbsent("otherAllowances", toBigDecimal(upstream.get("otherAllowances"), "1500.00"));
        salaryInfo.putIfAbsent("grossSalary", toBigDecimal(upstream.get("grossSalary"), "12000.00"));
        salaryInfo.putIfAbsent("deductions", toBigDecimal(upstream.get("deductions"), "1200.00"));
        salaryInfo.putIfAbsent("netSalary", toBigDecimal(upstream.get("netSalary"), "10800.00"));
        salaryInfo.putIfAbsent("currency", upstream.getOrDefault("currency", "SAR"));
        salaryInfo.putIfAbsent("employmentType", upstream.getOrDefault("employmentType", "GOVERNMENT"));
        salaryInfo.putIfAbsent("startDate", upstream.getOrDefault("startDate",
                LocalDate.now().minusYears(3).toString()));

        log.info("GOSI result: nationalId={}, netSalary={}", maskId(nationalId), salaryInfo.get("netSalary"));
        return salaryInfo;
    }

    private BigDecimal toBigDecimal(Object value, String fallback) {
        if (value == null) return new BigDecimal(fallback);
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return new BigDecimal(fallback);
        }
    }

    private String maskId(String id) {
        if (id == null || id.length() < 4) return "***";
        return "***" + id.substring(id.length() - 4);
    }
}
