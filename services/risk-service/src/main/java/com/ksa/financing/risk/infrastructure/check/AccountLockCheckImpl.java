package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.AccountLockCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AccountLockCheckImpl implements AccountLockCheck {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public AccountLockResult check(AccountLockInput input) {
        log.info("Starting account lock history check");

        try {
            List<Map<String, Object>> locks = jdbcTemplate.queryForList(
                """
                SELECT lock_type, lock_reason, is_active, locked_at
                FROM account_locks
                WHERE nid_hash = ? AND is_active = true
                ORDER BY locked_at DESC
                """,
                input.nidHash()
            );

            if (locks.isEmpty()) {
                log.info("Account lock check passed: no active locks");
                return new AccountLockResult(CheckDecision.PASS, false, null, null, null);
            }

            Map<String, Object> latestLock = locks.getFirst();
            String lockType = (String) latestLock.get("lock_type");
            String lockReason = (String) latestLock.get("lock_reason");

            if ("COMPLIANCE".equalsIgnoreCase(lockType) || "FRAUD".equalsIgnoreCase(lockType)) {
                log.warn("Active {} lock found for NID hash", lockType);
                return new AccountLockResult(
                    CheckDecision.HARD_BLOCK, true, lockType, lockReason,
                    "Account registration is not available. Please contact support."
                );
            }

            if ("CONDITIONAL".equalsIgnoreCase(lockType)) {
                log.warn("Conditional lock found, routing to re-onboarding");
                return new AccountLockResult(
                    CheckDecision.ROUTE_REONBOARDING, true, lockType, lockReason, null
                );
            }

            if ("USER_REQUESTED".equalsIgnoreCase(lockType)) {
                log.info("User-requested active lock found");
                return new AccountLockResult(
                    CheckDecision.HARD_BLOCK, true, lockType, lockReason,
                    "Account registration is not available. Please contact support."
                );
            }

            log.warn("Unknown lock type '{}' found, treating as hard block", lockType);
            return new AccountLockResult(
                CheckDecision.HARD_BLOCK, true, lockType, lockReason,
                "Account registration is not available. Please contact support."
            );

        } catch (Exception e) {
            log.error("Account lock check failed", e);
            return new AccountLockResult(
                CheckDecision.HARD_BLOCK, false, null, null, "Account lock check error"
            );
        }
    }
}
