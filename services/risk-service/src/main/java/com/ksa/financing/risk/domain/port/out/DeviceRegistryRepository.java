package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRegistryRepository {

    Optional<DeviceRegistryEntry> findByDeviceId(String deviceId);

    List<DeviceRegistryEntry> findAllBlocked(String search);
    
    List<DeviceRegistryEntry> findAllForGrouping(String search);

    PageResponse<DeviceRegistryEntry> findAll(PageQuery query);

    void updateBlockStatus(String deviceId, boolean blocked, String reason, UUID blockCodeId);

    void resetAttempts(String deviceId);

    void resetFarmingOverride(String deviceId);

    boolean hasAnyRowWithExceededAttempts(String deviceId, int threshold);

    void deleteByDeviceId(String deviceId);

    boolean existsByDeviceId(String deviceId);
}
