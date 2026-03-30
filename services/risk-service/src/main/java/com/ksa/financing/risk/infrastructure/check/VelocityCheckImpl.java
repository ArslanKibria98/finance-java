package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.VelocityCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
@Slf4j
public class VelocityCheckImpl implements VelocityCheck {

    private static final int IP_MAX_PER_HOUR = 10;
    private static final int DEVICE_MAX_PER_DAY = 5;
    private static final int NID_MAX_PER_DAY = 3;
    private static final int MOBILE_MAX_PER_DAY = 3;

    private static final String KEY_PREFIX = "velocity:";
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");
    private static final String DAY_FORMAT_PATTERN = "yyyyMMdd";

    private final StringRedisTemplate redisTemplate;

    @Override
    public VelocityResult check(VelocityInput input) {
        log.info("Starting velocity checks for IP={}, device={}", input.ipAddress(), input.deviceId());

        try {
            String hourWindow = LocalDateTime.now().format(HOUR_FORMAT);
            String dayWindow = LocalDate.now().format(DateTimeFormatter.ofPattern(DAY_FORMAT_PATTERN));

            // Check 1: IP rate limit (max per hour)
            if (input.ipAddress() != null && !input.ipAddress().isBlank()) {
                String ipKey = KEY_PREFIX + "ip:" + input.ipAddress() + ":" + hourWindow;
                long ipCount = incrementWithExpiry(ipKey, Duration.ofHours(1));
                if (ipCount > IP_MAX_PER_HOUR) {
                    log.warn("Velocity EXCEEDED: IP {} has {} requests/hour (max={})",
                            input.ipAddress(), ipCount, IP_MAX_PER_HOUR);
                    return new VelocityResult(CheckDecision.SOFT_BLOCK, true,
                            "IP rate limit exceeded: " + ipCount + "/" + IP_MAX_PER_HOUR + " per hour",
                            (int) ipCount, "IP_HOURLY");
                }
            }

            // Check 2: Device rate limit (max per day)
            if (input.deviceId() != null && !input.deviceId().isBlank()) {
                String deviceKey = KEY_PREFIX + "device:" + input.deviceId() + ":" + dayWindow;
                long deviceCount = incrementWithExpiry(deviceKey, Duration.ofDays(1));
                if (deviceCount > DEVICE_MAX_PER_DAY) {
                    log.warn("Velocity EXCEEDED: Device {} has {} requests/day (max={})",
                            input.deviceId(), deviceCount, DEVICE_MAX_PER_DAY);
                    return new VelocityResult(CheckDecision.SOFT_BLOCK, true,
                            "Device rate limit exceeded: " + deviceCount + "/" + DEVICE_MAX_PER_DAY + " per day",
                            (int) deviceCount, "DEVICE_DAILY");
                }
            }

            // Check 3: NID rate limit (max per day)
            if (input.nidHash() != null && !input.nidHash().isBlank()) {
                String nidKey = KEY_PREFIX + "nid:" + input.nidHash() + ":" + dayWindow;
                long nidCount = incrementWithExpiry(nidKey, Duration.ofDays(1));
                if (nidCount > NID_MAX_PER_DAY) {
                    log.warn("Velocity EXCEEDED: NID hash {} has {} requests/day (max={})",
                            input.nidHash().substring(0, 8) + "...", nidCount, NID_MAX_PER_DAY);
                    return new VelocityResult(CheckDecision.SOFT_BLOCK, true,
                            "NID rate limit exceeded: " + nidCount + "/" + NID_MAX_PER_DAY + " per day",
                            (int) nidCount, "NID_DAILY");
                }
            }

            // Check 4: Mobile rate limit (max per day)
            if (input.mobileHash() != null && !input.mobileHash().isBlank()) {
                String mobileKey = KEY_PREFIX + "mobile:" + input.mobileHash() + ":" + dayWindow;
                long mobileCount = incrementWithExpiry(mobileKey, Duration.ofDays(1));
                if (mobileCount > MOBILE_MAX_PER_DAY) {
                    log.warn("Velocity EXCEEDED: Mobile hash {} has {} requests/day (max={})",
                            input.mobileHash().substring(0, 8) + "...", mobileCount, MOBILE_MAX_PER_DAY);
                    return new VelocityResult(CheckDecision.SOFT_BLOCK, true,
                            "Mobile rate limit exceeded: " + mobileCount + "/" + MOBILE_MAX_PER_DAY + " per day",
                            (int) mobileCount, "MOBILE_DAILY");
                }
            }

            log.info("Velocity checks PASSED for IP={}, device={}", input.ipAddress(), input.deviceId());
            return new VelocityResult(CheckDecision.PASS, false, null, 0, null);

        } catch (Exception e) {
            // CRITICAL: Never block the flow if Redis is unavailable
            log.warn("Velocity check failed due to Redis error ({}), allowing request to proceed", e.getMessage());
            return new VelocityResult(CheckDecision.PASS, false, null, 0, null);
        }
    }

    private long incrementWithExpiry(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count != null ? count : 0L;
    }
}
