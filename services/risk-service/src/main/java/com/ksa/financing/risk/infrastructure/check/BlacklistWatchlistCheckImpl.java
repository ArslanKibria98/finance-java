package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.BlacklistWatchlistCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class BlacklistWatchlistCheckImpl implements BlacklistWatchlistCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public BlacklistResult check(BlacklistInput input) {
        log.info("Starting blacklist/watchlist check");

        try {
            // --- Check nid_blacklist table (raw nationalId) ---
            if (input.nationalId() != null && !input.nationalId().isBlank()) {
                Integer nidCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM nid_blacklist WHERE national_id = ? AND status = 'BLACKLISTED'",
                    Integer.class, input.nationalId()
                );
                if (nidCount != null && nidCount > 0) {
                    log.warn("NID BLACKLIST HIT: nationalId is blacklisted");
                    return new BlacklistResult(
                        CheckDecision.HARD_BLOCK, true, false, "NID_BLACKLIST", null
                    );
                }
            }

            // --- Check mobile_blacklist table (raw mobileNumber) ---
            if (input.mobileNumber() != null && !input.mobileNumber().isBlank()) {
                Integer mobileCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM mobile_blacklist WHERE mobile_number = ? AND status = 'BLACKLISTED'",
                    Integer.class, input.mobileNumber()
                );
                if (mobileCount != null && mobileCount > 0) {
                    log.warn("MOBILE BLACKLIST HIT: mobileNumber is blacklisted");
                    return new BlacklistResult(
                        CheckDecision.HARD_BLOCK, true, false, "MOBILE_BLACKLIST", null
                    );
                }
            }

            // --- Check watchlist_entries table (hashed values) ---
            List<Map<String, Object>> watchlistHits = jdbcTemplate.queryForList(
                """
                SELECT id, list_type, identifier_type, reason
                FROM watchlist_entries
                WHERE is_active = true
                  AND list_type = 'INTERNAL_GREYLIST'
                  AND (
                    (identifier_type = 'NID_HASH' AND identifier_value = ?)
                    OR (identifier_type = 'MOBILE_HASH' AND identifier_value = ?)
                  )
                """,
                input.nidHash(), input.mobileHash()
            );

            if (!watchlistHits.isEmpty()) {
                log.warn("WATCHLIST match detected: count={}", watchlistHits.size());
                return new BlacklistResult(
                    CheckDecision.FLAG_ENHANCED_MONITORING, false, true, "WATCHLIST", null
                );
            }

            log.info("Blacklist/watchlist check passed: no matches");
            return new BlacklistResult(CheckDecision.PASS, false, false, "CLEAR", null);

        } catch (Exception e) {
            log.error("Blacklist/watchlist check failed", e);
            return new BlacklistResult(
                CheckDecision.HARD_BLOCK, false, false, null, "Blacklist check error"
            );
        }
    }
}
