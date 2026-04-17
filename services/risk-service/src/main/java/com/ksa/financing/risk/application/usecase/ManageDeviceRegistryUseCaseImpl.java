package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.DeviceRegistryEntry;
import com.ksa.financing.risk.domain.port.in.ManageDeviceRegistryUseCase;
import com.ksa.financing.risk.domain.port.out.DeviceRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageDeviceRegistryUseCaseImpl implements ManageDeviceRegistryUseCase {

    private final DeviceRegistryRepository deviceRegistryRepository;

    @Override
    @Transactional
    public DeviceRegistryEntry blockDevice(String deviceId, String reason) {
        var existing = deviceRegistryRepository.findByDeviceId(deviceId);
        if (existing.isPresent() && existing.get().blocked()) {
            throw new BusinessException("RISK.DEVICE.ALREADY_BLOCKED",
                "Device is already blocked: " + maskDeviceId(deviceId));
        }

        if (existing.isEmpty()) {
            throw NotFoundException.forEntity("DeviceRegistry", deviceId);
        }

        deviceRegistryRepository.updateBlockStatus(deviceId, true, reason);
        log.info("Device blocked: {}, reason: {}", maskDeviceId(deviceId), reason);
        return deviceRegistryRepository.findByDeviceId(deviceId).orElseThrow();
    }

    @Override
    @Transactional
    public DeviceRegistryEntry unblockDevice(String deviceId) {
        var existing = deviceRegistryRepository.findByDeviceId(deviceId)
            .orElseThrow(() -> NotFoundException.forEntity("DeviceRegistry", deviceId));

        boolean wasBlocked = existing.blocked();
        boolean identityFarming = existing.nidAssociationCount() > 3;

        // Check max attempt_count across ALL rows for this device (findByDeviceId returns most-recent row only)
        boolean anyRowVelocityExceeded = deviceRegistryRepository.hasAnyRowWithExceededAttempts(deviceId, 5);

        if (!wasBlocked && !anyRowVelocityExceeded && !identityFarming) {
            throw new BusinessException("RISK.DEVICE.NOT_BLOCKED",
                "Device is not blocked: " + maskDeviceId(deviceId));
        }

        if (wasBlocked) {
            deviceRegistryRepository.updateBlockStatus(deviceId, false, null);
            log.info("Device admin-block removed: {}", maskDeviceId(deviceId));
        }

        // Always reset ALL row attempt counts on unblock — findByDeviceId only returns most-recent row
        // so velocityExceeded check on a single row is unreliable
        deviceRegistryRepository.resetAttempts(deviceId);
        log.info("Device attempts reset across all rows: {}", maskDeviceId(deviceId));

        if (identityFarming) {
            deviceRegistryRepository.resetFarmingOverride(deviceId);
            log.info("Device identity-farming override applied (nidCount={}): {}",
                existing.nidAssociationCount(), maskDeviceId(deviceId));
        }

        return deviceRegistryRepository.findByDeviceId(deviceId).orElseThrow();
    }

    @Override
    @Transactional(readOnly = true)
    public DeviceRegistryEntry getDeviceStatus(String deviceId) {
        return deviceRegistryRepository.findByDeviceId(deviceId)
            .orElseThrow(() -> NotFoundException.forEntity("DeviceRegistry", deviceId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceRegistryEntry> listBlockedDevices() {
        return deviceRegistryRepository.findAllBlocked();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceRegistryEntry> listAllDevices() {
        return deviceRegistryRepository.findAll();
    }

    @Override
    @Transactional
    public void removeDevice(String deviceId) {
        if (!deviceRegistryRepository.existsByDeviceId(deviceId)) {
            throw NotFoundException.forEntity("DeviceRegistry", deviceId);
        }
        deviceRegistryRepository.deleteByDeviceId(deviceId);
        log.info("Device removed from registry: {}", maskDeviceId(deviceId));
    }

    private String maskDeviceId(String deviceId) {
        if (deviceId == null || deviceId.length() < 8) return "****";
        return deviceId.substring(0, 8) + "...";
    }
}
