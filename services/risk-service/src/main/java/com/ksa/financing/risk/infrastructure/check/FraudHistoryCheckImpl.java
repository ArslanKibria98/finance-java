package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.FraudHistoryCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class FraudHistoryCheckImpl implements FraudHistoryCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public FraudHistoryResult check(FraudHistoryInput input) {
        log.info("Starting fraud history check");

        try {
            List<Map<String, Object>> signals = jdbcTemplate.queryForList(
                """
                SELECT id, signal_type, severity, is_triggered, score_contribution
                FROM fraud_signal_logs
                WHERE is_triggered = true
                  AND (signal_value = ? OR signal_value = ?)
                ORDER BY severity DESC, detected_at DESC
                """,
                input.nidHash(), input.mobileHash()
            );

            if (signals.isEmpty()) {
                log.info("Fraud history check passed: no fraud history");
                return new FraudHistoryResult(CheckDecision.PASS, false, false, 0, null);
            }

            int incidentCount = signals.size();
            boolean confirmedFraud = false;
            boolean suspectedFraud = false;

            for (Map<String, Object> signal : signals) {
                String severity = (String) signal.get("severity");
                if ("CRITICAL".equals(severity) || "HIGH".equals(severity)) {
                    confirmedFraud = true;
                } else if ("MEDIUM".equals(severity)) {
                    suspectedFraud = true;
                }
            }

            if (confirmedFraud) {
                log.warn("CONFIRMED FRAUD detected: {} incident(s)", incidentCount);
                return new FraudHistoryResult(
                    CheckDecision.HARD_BLOCK, true, false, incidentCount, null
                );
            }

            if (suspectedFraud) {
                log.warn("SUSPECTED FRAUD detected: {} incident(s)", incidentCount);
                return new FraudHistoryResult(
                    CheckDecision.FLAG_HIGH_RISK, false, true, incidentCount, null
                );
            }

            log.info("Fraud history check: {} low-severity signal(s) found", incidentCount);
            return new FraudHistoryResult(CheckDecision.PASS, false, false, incidentCount, null);

        } catch (Exception e) {
            log.error("Fraud history check failed", e);
            return new FraudHistoryResult(
                CheckDecision.HARD_BLOCK, false, false, 0, "Fraud history check error"
            );
        }
    }
}
