package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.CustomerBlock;
import com.ksa.financing.customer.domain.port.out.CustomerBlockRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.BlockCodeJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.entity.CustomerBlockJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.BlockCodePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CustomerBlockRepositoryImpl implements CustomerBlockRepository {

    private final JpaCustomerBlockRepository jpaCustomerBlockRepository;
    private final JpaBlockCodeRepository jpaBlockCodeRepository;

    @Override
    public CustomerBlock save(CustomerBlock customerBlock) {
        BlockCodeJpaEntity blockCodeEntity = jpaBlockCodeRepository.findById(customerBlock.getBlockCodeId())
                .orElseThrow(() -> new RuntimeException("Block code not found: " + customerBlock.getBlockCodeId()));
        
        CustomerBlockJpaEntity entity = BlockCodePersistenceMapper.toEntity(customerBlock, blockCodeEntity);
        CustomerBlockJpaEntity saved = jpaCustomerBlockRepository.save(entity);
        return BlockCodePersistenceMapper.toDomain(saved);
    }

    @Override
    public List<CustomerBlock> findAllActiveByCustomerId(UUID customerId) {
        return jpaCustomerBlockRepository.findAllByCustomerIdAndActiveTrue(customerId).stream()
                .map(BlockCodePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<CustomerBlock> findAllByCustomerId(UUID customerId) {
        return jpaCustomerBlockRepository.findAllByCustomerId(customerId).stream()
                .map(BlockCodePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void deactivateAllForCustomer(UUID customerId) {
        List<CustomerBlockJpaEntity> activeBlocks = jpaCustomerBlockRepository.findAllByCustomerIdAndActiveTrue(customerId);
        activeBlocks.forEach(block -> block.setActive(false));
        jpaCustomerBlockRepository.saveAll(activeBlocks);
    }
}
