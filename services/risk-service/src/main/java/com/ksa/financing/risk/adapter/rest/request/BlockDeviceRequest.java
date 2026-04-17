package com.ksa.financing.risk.adapter.rest.request;

import jakarta.validation.constraints.NotBlank;

public record BlockDeviceRequest(
    @NotBlank(message = "Device ID is required")
    String deviceId,

    String reason
) {}
