package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.BlockCode;
import com.ksa.financing.customer.domain.model.BlockCodeType;
import com.ksa.financing.customer.domain.model.Customer;
import com.ksa.financing.customer.domain.model.CustomerBlock;
import com.ksa.financing.customer.domain.port.in.ManageCustomerBlocksUseCase;
import com.ksa.financing.customer.domain.port.out.BlockCodeRepository;
import com.ksa.financing.customer.domain.port.out.CustomerBlockRepository;
import com.ksa.financing.customer.domain.port.out.CustomerRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.security.blacklist.BlacklistCacheService;
import com.ksa.financing.infra.security.blacklist.BlacklistType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageCustomerBlocksService implements ManageCustomerBlocksUseCase {

    private final CustomerRepository customerRepository;
    private final BlockCodeRepository blockCodeRepository;
    private final CustomerBlockRepository customerBlockRepository;
    private final BlacklistCacheService blacklistCacheService;

    @Override
    @Transactional
    public void assignBlockCode(UUID tenantId, UUID customerId, AssignBlockCommand command) {
        log.info("Assigning block codes {} to customer {}", command.blockCodeIds(), customerId);

        Customer customer = customerRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Customer not found"));

        boolean shouldBlock = false;
        for (UUID blockCodeId : command.blockCodeIds()) {
            BlockCode blockCode = blockCodeRepository.findById(blockCodeId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Block code not found with ID: " + blockCodeId));

            // Check if already assigned
            boolean alreadyAssigned = customerBlockRepository.findAllActiveByCustomerId(customerId).stream()
                    .anyMatch(b -> b.getBlockCodeId().equals(blockCodeId));

            if (alreadyAssigned) {
                log.warn("Block code {} already assigned to customer {}", blockCodeId, customerId);
                continue;
            }

            CustomerBlock newBlock = CustomerBlock.builder()
                    .id(UUID.randomUUID())
                    .customerId(customerId)
                    .blockCodeId(blockCodeId)
                    .assignedBy(command.assignedBy())
                    .reason(command.reason())
                    .assignedAt(Instant.now())
                    .expiresAt(command.expiresAt())
                    .active(true)
                    .build();

            customerBlockRepository.save(newBlock);

            if (blockCode.getType() == BlockCodeType.HARD_BLOCK || blockCode.getType() == BlockCodeType.SOFT_BLOCK) {
                shouldBlock = true;
            }
        }

        if (shouldBlock) {
            customer.setActive(false);
            customer.setBlockedAt(Instant.now());
            customer.setBlockedReason(command.reason() != null ? command.reason() : "Security Policy Violation");
            customerRepository.save(customer);

            // Sync with Blacklist Cache to prevent login
            blacklistCacheService.put(BlacklistType.NID, customer.getNationalId(), 
                customer.getBlockedReason(), customer.getBlockedAt().plus(java.time.Duration.ofDays(365)));
            blacklistCacheService.put(BlacklistType.MOBILE, customer.getMobileNumber(), 
                customer.getBlockedReason(), customer.getBlockedAt().plus(java.time.Duration.ofDays(365)));
            log.info("Customer {} (NID: {}, Mobile: {}) blacklisted in Redis for login prevention", 
                customerId, customer.getNationalId(), customer.getMobileNumber());
        }
    }

    @Override
    @Transactional
    public void removeBlockCode(UUID tenantId, UUID customerId, List<UUID> blockCodeIds) {
        log.info("Removing block codes {} from customer {}", blockCodeIds, customerId);

        List<CustomerBlock> activeBlocks = customerBlockRepository.findAllActiveByCustomerId(customerId);
        
        for (UUID blockCodeId : blockCodeIds) {
            CustomerBlock blockToRemove = activeBlocks.stream()
                    .filter(b -> b.getBlockCodeId().equals(blockCodeId))
                    .findFirst()
                    .orElse(null);

            if (blockToRemove != null) {
                blockToRemove.setActive(false);
                customerBlockRepository.save(blockToRemove);
                log.debug("Deactivated block code {} for customer {}", blockCodeId, customerId);
            }
        }

        // Re-evaluate customer active status
        List<CustomerBlock> remainingActiveBlocks = customerBlockRepository.findAllActiveByCustomerId(customerId);
        boolean hasHardOrSoftBlock = remainingActiveBlocks.stream()
                .anyMatch(b -> b.getBlockType() == BlockCodeType.HARD_BLOCK || b.getBlockType() == BlockCodeType.SOFT_BLOCK);

        if (!hasHardOrSoftBlock) {
            Customer customer = customerRepository.findByIdWithLock(customerId)
                    .orElseThrow(() -> new BusinessException(ErrorCodes.NOT_FOUND, "Customer not found"));
            
            if (!customer.isActive()) {
                customer.setActive(true);
                customer.setBlockedAt(null);
                customer.setBlockedReason(null);
                customerRepository.save(customer);
                
                // Remove from Blacklist Cache to allow login
                blacklistCacheService.remove(BlacklistType.NID, customer.getNationalId());
                blacklistCacheService.remove(BlacklistType.MOBILE, customer.getMobileNumber());
                log.info("Customer {} (NID: {}, Mobile: {}) removed from blacklist in Redis", 
                    customerId, customer.getNationalId(), customer.getMobileNumber());
            }
        }
    }

    @Override
    public List<CustomerBlock> getCustomerBlocks(UUID customerId) {
        return customerBlockRepository.findAllActiveByCustomerId(customerId);
    }

    @Override
    public List<BlockCode> getAvailableBlockCodes(UUID tenantId) {
        return blockCodeRepository.findAllActiveByTenant(tenantId);
    }
}
