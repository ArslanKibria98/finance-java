package com.ksa.financing.customer.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerBlock {
    private UUID id;
    private UUID customerId;
    private UUID blockCodeId;
    private String blockCode; // Denormalized for easier display
    private String blockDescription;
    private BlockCodeType blockType;
    private UUID assignedBy;
    private String reason;
    private Instant assignedAt;
    private Instant expiresAt;
    private boolean active;
}
