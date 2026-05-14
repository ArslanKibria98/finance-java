package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record BlockDeviceRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId,

    String reason,

    UUID blockCodeId
) {}
