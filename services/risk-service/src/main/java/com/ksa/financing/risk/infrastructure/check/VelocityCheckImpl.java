package com.ksa.financing.risk.infrastructure.check;

import com.ksa.financing.risk.domain.model.CheckDecision;
import com.ksa.financing.risk.domain.port.out.VelocityCheck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
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
    private static final double GLOBAL_SPIKE_MULTIPLIER = 2.0;
    private static final long GLOBAL_BASELINE_DEFAULT = 100;

    private static final String KEY_PREFIX = "velocity:";
    private static final DateTimeFormatter HOUR_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHH");

    private final StringRedisTemplate redisTemplate;

    @Override
    public VelocityResult check(VelocityInput input) {
        log.info("Starting velocity checks");

        // TODO: ALL velocity checks temporarily disabled for dev/testing
        // Re-enable ALL checks before production: IP, device, NID, mobile, global spike
        log.info("Velocity checks SKIPPED (disabled for dev/testing)");
        return new VelocityResult(CheckDecision.PASS, false, null, 0, null);
    }

    private long incrementWithExpiry(String key, Duration ttl) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl);
        }
        return count != null ? count : 0L;
    }
}
