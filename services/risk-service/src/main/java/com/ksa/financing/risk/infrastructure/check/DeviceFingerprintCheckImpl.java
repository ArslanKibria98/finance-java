package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.DeviceFingerprintCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceFingerprintCheckImpl implements DeviceFingerprintCheck {

    private static final int MAX_NID_PER_DEVICE = 3;
    private static final int MAX_ATTEMPTS_PER_DAY = 5;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public DeviceFingerprintResult check(DeviceFingerprintInput input) {
        log.info("Starting device fingerprint check for deviceId: {}...",
            maskDeviceId(input.deviceId()));

        try {
            // Check if device is blocked
            Integer blockedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM device_registry WHERE device_id = ? AND is_blocked = true",
                Integer.class,
                input.deviceId()
            );

            if (blockedCount != null && blockedCount > 0) {
                log.warn("Device is BLOCKED: {}", maskDeviceId(input.deviceId()));
                return new DeviceFingerprintResult(
                    CheckDecision.HARD_BLOCK, 0, true, false, 0,
                    "Unable to proceed from this device."
                );
            }

            // Count distinct NIDs associated with this device
            Integer nidCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT nid_hash) FROM device_registry WHERE device_id = ?",
                Integer.class,
                input.deviceId()
            );
            int nidAssociationCount = nidCount != null ? nidCount : 0;

            if (nidAssociationCount > MAX_NID_PER_DEVICE) {
                log.warn("Identity farming detected: device {} has {} NID associations",
                    maskDeviceId(input.deviceId()), nidAssociationCount);
                return new DeviceFingerprintResult(
                    CheckDecision.HARD_BLOCK, nidAssociationCount, false, false, 0,
                    "Unable to proceed from this device."
                );
            }

            // Count attempts in last 24 hours — SKIPPED (temporarily disabled for dev/testing)
            // TODO: Re-enable device attempt velocity check before production
            int attemptCount = 0;

            // Register/update device entry (upsert)
            jdbcTemplate.update(
                """
                INSERT INTO device_registry (device_id, device_fingerprint, nid_hash, attempt_count, last_seen_at, updated_at)
                VALUES (?, ?, ?, 1, NOW(), NOW())
                ON CONFLICT (device_id, nid_hash)
                DO UPDATE SET
                    device_fingerprint = COALESCE(EXCLUDED.device_fingerprint, device_registry.device_fingerprint),
                    attempt_count = device_registry.attempt_count + 1,
                    last_seen_at = NOW(),
                    updated_at = NOW()
                """,
                input.deviceId(),
                input.deviceFingerprint(),
                "pending"
            );

            log.info("Device fingerprint check passed: {} NID associations, {} recent attempts",
                nidAssociationCount, attemptCount);
            return new DeviceFingerprintResult(
                CheckDecision.PASS, nidAssociationCount, false, false, attemptCount, null
            );

        } catch (Exception e) {
            log.error("Device fingerprint check failed", e);
            return new DeviceFingerprintResult(
                CheckDecision.HARD_BLOCK, 0, false, false, 0, "Device verification error"
            );
        }
    }

    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) return "****";
        return deviceId.substring(0, 8) + "...";
    }
}
