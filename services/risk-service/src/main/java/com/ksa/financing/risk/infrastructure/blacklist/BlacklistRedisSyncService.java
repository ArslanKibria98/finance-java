package com.ksa.financing.risk.infrastructure.blacklist;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.infra.security.blacklist.BlacklistCacheService;
import com.ksa.financing.infra.security.blacklist.BlacklistType;
import com.ksa.financing.risk.domain.model.BlacklistStatus;
import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;
import com.ksa.financing.risk.domain.model.MobileBlacklistEntry;
import com.ksa.financing.risk.domain.model.NidBlacklistEntry;
import com.ksa.financing.risk.domain.port.out.BlacklistRepository;
import com.ksa.financing.risk.domain.port.out.DeviceRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Risk-service component that hydrates the SDK Redis blacklist cache from Postgres.
 *
 * <p>Triggered on:
 * <ul>
 *   <li>application startup ({@link ApplicationReadyEvent})</li>
 *   <li>after CRUD operations on the blacklist (called by use cases)</li>
 * </ul>
 *
 * <p>Source of truth = Postgres. Other services read from Redis only — they
 * never touch this database (DB-per-service rule).</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlacklistRedisSyncService {

    private static final int BATCH_SIZE = 500;

    private final BlacklistRepository blacklistRepository;
    private final DeviceRegistryRepository deviceRegistryRepository;
    private final BlacklistCacheService cacheService;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Bootstrapping blacklist Redis cache from Postgres…");
        try {
            syncAll();
            log.info("Blacklist Redis cache bootstrap complete");
        } catch (Exception e) {
            log.error("Blacklist Redis bootstrap failed (continuing — cache may be partial)", e);
        }
    }

    /** Full re-hydration of all blacklist types. Drops existing keys first. */
    public void syncAll() {
        cacheService.invalidateType(BlacklistType.NID);
        cacheService.invalidateType(BlacklistType.MOBILE);
        cacheService.invalidateType(BlacklistType.DEVICE);
        syncNids();
        syncMobiles();
        syncDevices();
    }

    public void syncNids() {
        int page = 0;
        int total = 0;
        while (true) {
            PageResponse<NidBlacklistEntry> p = blacklistRepository.findAllNid(
                    new PageQuery(page, BATCH_SIZE, null, null, null));
            for (NidBlacklistEntry e : p.content()) {
                if (e.status() == BlacklistStatus.BLACKLISTED) {
                    cacheService.put(BlacklistType.NID, e.nationalId().value(), e.reason(), null);
                    total++;
                }
            }
            if (p.pagination().last()) break;
            page++;
        }
        log.info("Synced {} NID blacklist entries to Redis", total);
    }

    public void syncMobiles() {
        int page = 0;
        int total = 0;
        while (true) {
            PageResponse<MobileBlacklistEntry> p = blacklistRepository.findAllMobile(
                    new PageQuery(page, BATCH_SIZE, null, null, null));
            for (MobileBlacklistEntry e : p.content()) {
                if (e.status() == BlacklistStatus.BLACKLISTED) {
                    cacheService.put(BlacklistType.MOBILE, e.mobileNumber(), e.reason(), null);
                    total++;
                }
            }
            if (p.pagination().last()) break;
            page++;
        }
        log.info("Synced {} mobile blacklist entries to Redis", total);
    }

    public void syncDevices() {
        List<DeviceRegistryEntry> blocked = deviceRegistryRepository.findAllBlocked(null);
        for (DeviceRegistryEntry e : blocked) {
            cacheService.put(BlacklistType.DEVICE, e.deviceId(), e.blockReason(), null);
        }
        log.info("Synced {} blocked devices to Redis", blocked.size());
    }

    // ── Hooks for CRUD use cases ─────────────────────────────────────

    public void onNidBlacklisted(String nid, String reason) {
        cacheService.put(BlacklistType.NID, nid, reason, null);
    }

    public void onNidRemoved(String nid) {
        cacheService.remove(BlacklistType.NID, nid);
    }

    public void onMobileBlacklisted(String mobile, String reason) {
        cacheService.put(BlacklistType.MOBILE, mobile, reason, null);
    }

    public void onMobileRemoved(String mobile) {
        cacheService.remove(BlacklistType.MOBILE, mobile);
    }

    public void onDeviceBlocked(String deviceId, String reason) {
        cacheService.put(BlacklistType.DEVICE, deviceId, reason, null);
    }

    public void onDeviceUnblocked(String deviceId) {
        cacheService.remove(BlacklistType.DEVICE, deviceId);
    }

    public void onUserBlocked(String userId, String reason) {
        cacheService.put(BlacklistType.USER, userId, reason, null);
    }

    public void onUserUnblocked(String userId) {
        cacheService.remove(BlacklistType.USER, userId);
    }
}
