package com.ksa.financing.middleware.application.dto;

import java.util.List;
import java.util.UUID;

public record BulkGrantAccessResponse(
        UUID clientId,
        int providersGranted,
        int apisGranted
) {}
