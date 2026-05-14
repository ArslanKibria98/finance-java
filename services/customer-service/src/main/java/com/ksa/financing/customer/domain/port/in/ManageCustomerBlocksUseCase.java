package com.ksa.financing.customer.domain.port.in;

import com.ksa.financing.customer.domain.model.BlockCode;
import com.ksa.financing.customer.domain.model.CustomerBlock;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ManageCustomerBlocksUseCase {

    void assignBlockCode(UUID tenantId, UUID customerId, AssignBlockCommand command);
    
    void removeBlockCode(UUID tenantId, UUID customerId, List<UUID> blockCodeIds);
    
    List<CustomerBlock> getCustomerBlocks(UUID customerId);
    
    List<BlockCode> getAvailableBlockCodes(UUID tenantId);

    record AssignBlockCommand(
        List<UUID> blockCodeIds,
        String reason,
        Instant expiresAt,
        UUID assignedBy
    ) {}
}
