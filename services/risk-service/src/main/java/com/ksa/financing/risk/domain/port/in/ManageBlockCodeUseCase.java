package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.domain.model.BlockCodeType;

import java.util.List;
import java.util.UUID;

public interface ManageBlockCodeUseCase {
    BlockCode createBlockCode(UUID tenantId, CreateBlockCodeCommand command);
    BlockCode updateBlockCode(UUID tenantId, UUID id, UpdateBlockCodeCommand command);
    List<BlockCode> listBlockCodes(UUID tenantId);
    void deleteBlockCode(UUID tenantId, UUID id);

    record CreateBlockCodeCommand(String code, String description, BlockCodeType type) {}
    record UpdateBlockCodeCommand(String description, BlockCodeType type, Boolean active) {}
}
