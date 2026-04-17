package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;

import java.util.List;

public interface ManageDeviceRegistryUseCase {

    DeviceRegistryEntry blockDevice(String deviceId, String reason);

    DeviceRegistryEntry unblockDevice(String deviceId);

    DeviceRegistryEntry getDeviceStatus(String deviceId);

    List<DeviceRegistryEntry> listBlockedDevices();

    List<DeviceRegistryEntry> listAllDevices();

    void removeDevice(String deviceId);
}
