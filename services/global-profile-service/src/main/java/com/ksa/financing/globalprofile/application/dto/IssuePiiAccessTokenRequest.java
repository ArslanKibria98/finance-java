package com.ksa.financing.globalprofile.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record IssuePiiAccessTokenRequest(
    @NotEmpty List<String> allowedFields,
    @NotNull UUID requesterId,
    @NotBlank String requesterRole,
    @NotBlank String accessPurpose,
    String relatedEntityType,
    UUID relatedEntityId
) {}
