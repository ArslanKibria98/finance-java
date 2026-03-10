package com.ksa.financing.globalprofile.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record LinkRegionalProfileRequest(
    @NotBlank String countryCode,
    @NotBlank String regionalCifNumber,
    @NotNull UUID piiVaultRecordId,
    UUID keycloakUserId
) {}
