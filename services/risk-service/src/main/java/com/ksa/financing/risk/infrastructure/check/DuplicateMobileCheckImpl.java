package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.port.out.DuplicateMobileCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicateMobileCheckImpl implements DuplicateMobileCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DuplicateMobileResult check(DuplicateMobileInput input) {
        log.info("Starting duplicate mobile check for hash: {}...", maskHash(input.mobileHash()));

        try {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM customers_view WHERE mobile_hash = ? AND status != 'DELETED'",
                Integer.class,
                input.mobileHash()
            );

            boolean duplicate = count != null && count > 0;

            if (duplicate) {
                log.warn("Duplicate mobile detected: {} active account(s) found", count);
                return new DuplicateMobileResult(true,
                    "This mobile number is already registered. Please log in or use a different number.");
            }

            log.info("Duplicate mobile check passed: mobile not registered");
            return new DuplicateMobileResult(false, null);

        } catch (Exception e) {
            log.error("Duplicate mobile check failed", e);
            return new DuplicateMobileResult(true, "Mobile verification error");
        }
    }

    private String maskHash(String hash) {
        if (hash == null || hash.length() < 8) return "****";
        return hash.substring(0, 8) + "...";
    }
}
