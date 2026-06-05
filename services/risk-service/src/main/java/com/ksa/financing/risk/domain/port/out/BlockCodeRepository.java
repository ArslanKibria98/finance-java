package com.ksa.financing.risk.domain.port.out;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.domain.model.BlockCodeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BlockCodeRepository {
    BlockCode save(BlockCode blockCode);
    Optional<BlockCode> findById(UUID tenantId, UUID id);
    Optional<BlockCode> findByCode(UUID tenantId, String code);
    List<BlockCode> findAll(UUID tenantId);
    List<BlockCode> findByType(UUID tenantId, BlockCodeType type);
    void delete(UUID tenantId, UUID id);
}
