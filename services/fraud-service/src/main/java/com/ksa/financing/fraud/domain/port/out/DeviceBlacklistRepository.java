package com.ksa.financing.fraud.domain.port.out;

import com.ksa.financing.fraud.domain.model.blacklist.DeviceBlacklistEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceBlacklistRepository {

    DeviceBlacklistEntry save(DeviceBlacklistEntry entry);

    Optional<DeviceBlacklistEntry> findActiveByDeviceId(UUID tenantId, String deviceId);

    List<DeviceBlacklistEntry> findAllActive(UUID tenantId);

    void deactivate(UUID tenantId, String deviceId);

    boolean isBlacklisted(UUID tenantId, String deviceId);
}
