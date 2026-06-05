package com.ksa.financing.customer.domain.port.out;

import com.ksa.financing.customer.domain.model.BlockCode;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockCodeRepository {
    BlockCode save(BlockCode blockCode);
    Optional<BlockCode> findById(UUID id);
    Optional<BlockCode> findByCode(UUID tenantId, String code);
    List<BlockCode> findAllActiveByTenant(UUID tenantId);
}
