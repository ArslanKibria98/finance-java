package com.ksa.financing.risk.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class BlockCode {
    private final UUID id;
    private final UUID tenantId;
    private final String code;
    private final String description;
    private final BlockCodeType type;
    private final boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final int version;

    public static BlockCode create(UUID tenantId, String code, String description, BlockCodeType type) {
        return BlockCode.builder()
            .tenantId(tenantId)
            .code(code)
            .description(description)
            .type(type)
            .active(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .version(1)
            .build();
    }
}
