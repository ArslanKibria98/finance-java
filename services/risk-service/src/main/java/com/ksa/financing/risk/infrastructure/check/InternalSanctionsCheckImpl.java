package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.InternalSanctionsCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class InternalSanctionsCheckImpl implements InternalSanctionsCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public InternalSanctionsResult check(InternalSanctionsInput input) {
        log.info("Starting internal sanctions list check");

        try {
            List<Map<String, Object>> matches = jdbcTemplate.queryForList(
                """
                SELECT id, source, source_reference, reason
                FROM watchlist_entries
                WHERE list_type = 'SANCTIONS'
                  AND identifier_type = 'NID_HASH'
                  AND identifier_value = ?
                  AND is_active = true
                """,
                input.nidHash()
            );

            if (matches.isEmpty()) {
                log.info("Internal sanctions check passed: no match");
                return new InternalSanctionsResult(CheckDecision.PASS, false, null, null);
            }

            log.error("SANCTIONS MATCH detected for NID hash: count={}", matches.size());

            // Log sanctions match to fraud_signal_logs for mandatory retention
            try {
                jdbcTemplate.update(
                    """
                    INSERT INTO fraud_signal_logs
                        (tenant_id, customer_id, signal_type, signal_source, severity,
                         signal_value, weight, score_contribution, is_triggered, detected_at)
                    VALUES
                        (?::uuid,
                         gen_random_uuid(),
                         'SANCTIONS_MATCH', 'INTERNAL_SANCTIONS_CHECK', 'CRITICAL',
                         ?, 1.0000, 100, true, NOW())
                    """,
                    input.tenantId(),
                    input.nidHash()
                );
            } catch (Exception logEx) {
                log.error("Failed to log sanctions match to fraud_signal_logs", logEx);
            }

            return new InternalSanctionsResult(
                CheckDecision.HARD_BLOCK, true, "HIGH", null
            );

        } catch (Exception e) {
            log.error("Internal sanctions check failed", e);
            return new InternalSanctionsResult(
                CheckDecision.HARD_BLOCK, false, null, "Sanctions check error"
            );
        }
    }
}
