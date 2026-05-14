package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;

import java.util.List;
import java.util.UUID;

public interface ManageDeviceRegistryUseCase {

    DeviceRegistryEntry blockDevice(String deviceId, String reason, UUID blockCodeId);

    DeviceRegistryEntry unblockDevice(String deviceId);

    DeviceRegistryEntry getDeviceStatus(String deviceId);

    List<DeviceRegistryEntry> listBlockedDevices(String search);
    
    List<DeviceRegistryEntry> listAllDevicesGrouped(String search);

    PageResponse<DeviceRegistryEntry> listAllDevices(PageQuery query);

    void removeDevice(String deviceId);
}
