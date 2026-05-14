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
public class BlockCode {
    private UUID id;
    private UUID tenantId;
    private String code;
    private String description;
    private BlockCodeType type;
    private BlockCodeCategory category;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
