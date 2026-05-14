package com.ksa.financing.fraud.infrastructure.persistence;

import com.ksa.financing.fraud.domain.model.blacklist.DeviceBlacklistEntry;
import com.ksa.financing.fraud.domain.port.out.DeviceBlacklistRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class DeviceBlacklistRepositoryImpl implements DeviceBlacklistRepository {

    @Override
    public DeviceBlacklistEntry save(DeviceBlacklistEntry entry) {
        return entry;
    }

    @Override
    public Optional<DeviceBlacklistEntry> findActiveByDeviceId(UUID tenantId, String deviceId) {
        return Optional.empty();
    }

    @Override
    public List<DeviceBlacklistEntry> findAllActive(UUID tenantId) {
        return List.of();
    }

    @Override
    public void deactivate(UUID tenantId, String deviceId) {
    }

    @Override
    public boolean isBlacklisted(UUID tenantId, String deviceId) {
        return false;
    }
}
