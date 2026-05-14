package com.ksa.financing.customer.infrastructure.persistence.repository;

import com.ksa.financing.customer.domain.model.BlockCode;
import com.ksa.financing.customer.domain.port.out.BlockCodeRepository;
import com.ksa.financing.customer.infrastructure.persistence.entity.BlockCodeJpaEntity;
import com.ksa.financing.customer.infrastructure.persistence.mapper.BlockCodePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class BlockCodeRepositoryImpl implements BlockCodeRepository {

    private final JpaBlockCodeRepository jpaBlockCodeRepository;

    @Override
    public BlockCode save(BlockCode blockCode) {
        BlockCodeJpaEntity entity = BlockCodePersistenceMapper.toEntity(blockCode);
        BlockCodeJpaEntity saved = jpaBlockCodeRepository.save(entity);
        return BlockCodePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<BlockCode> findById(UUID id) {
        return jpaBlockCodeRepository.findById(id)
                .map(BlockCodePersistenceMapper::toDomain);
    }

    @Override
    public Optional<BlockCode> findByCode(UUID tenantId, String code) {
        return jpaBlockCodeRepository.findByTenantIdAndCode(tenantId, code)
                .map(BlockCodePersistenceMapper::toDomain);
    }

    @Override
    public List<BlockCode> findAllActiveByTenant(UUID tenantId) {
        return jpaBlockCodeRepository.findAllByTenantIdAndActiveTrue(tenantId).stream()
                .map(BlockCodePersistenceMapper::toDomain)
                .toList();
    }
}
