package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CifStatus;
import com.ksa.financing.risk.domain.port.out.CifLookupCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CifLookupCheckImpl implements CifLookupCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public CifLookupResult lookup(CifLookupInput input) {
        log.info("Starting CIF lookup for NID hash: {}...", maskHash(input.nidHash()));

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                "SELECT id, status FROM customers_view WHERE nid_hash = ?",
                input.nidHash()
            );

            if (results.isEmpty()) {
                log.info("CIF lookup: NEW customer (no existing record)");
                return new CifLookupResult(CifStatus.NEW, null, null);
            }

            Map<String, Object> customer = results.getFirst();
            String status = (String) customer.get("status");
            String customerId = customer.get("id").toString();

            return switch (status.toUpperCase()) {
                case "BLOCKED", "SUSPENDED" -> {
                    log.warn("CIF lookup: BLOCKED customer, customerId: {}", customerId);
                    yield new CifLookupResult(CifStatus.BLOCKED, customerId, null);
                }
                case "QUALIFIED" -> {
                    log.info("CIF lookup: PENDING customer (onboarding incomplete), customerId: {}", customerId);
                    yield new CifLookupResult(CifStatus.EXISTING_PENDING, customerId, null);
                }
                case "INACTIVE" -> {
                    log.info("CIF lookup: INACTIVE customer, customerId: {}", customerId);
                    yield new CifLookupResult(CifStatus.EXISTING_INACTIVE, customerId, null);
                }
                case "DORMANT" -> {
                    log.info("CIF lookup: DORMANT customer, customerId: {}", customerId);
                    yield new CifLookupResult(CifStatus.EXISTING_DORMANT, customerId, null);
                }
                default -> {
                    log.info("CIF lookup: ACTIVE customer, customerId: {}", customerId);
                    yield new CifLookupResult(CifStatus.EXISTING_ACTIVE, customerId, null);
                }
            };

        } catch (Exception e) {
            log.error("CIF lookup failed", e);
            return new CifLookupResult(CifStatus.BLOCKED, null, "CIF lookup error");
        }
    }

    private String maskHash(String hash) {
        if (hash == null || hash.length() < 8) return "****";
        return hash.substring(0, 8) + "...";
    }
}
