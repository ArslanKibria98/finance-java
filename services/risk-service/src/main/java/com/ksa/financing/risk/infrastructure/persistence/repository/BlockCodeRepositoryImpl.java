package com.ksa.financing.risk.infrastructure.persistence.repository;

import com.ksa.financing.risk.domain.model.BlockCode;
import com.ksa.financing.risk.domain.model.BlockCodeType;
import com.ksa.financing.risk.domain.port.out.BlockCodeRepository;
import com.ksa.financing.risk.infrastructure.persistence.mapper.BlockCodePersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class BlockCodeRepositoryImpl implements BlockCodeRepository {

    private final JpaBlockCodeRepository jpaRepository;
    private final BlockCodePersistenceMapper mapper;

    @Override
    @Transactional
    public BlockCode save(BlockCode blockCode) {
        var entity = mapper.toJpa(blockCode);
        var saved = jpaRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<BlockCode> findById(UUID tenantId, UUID id) {
        return jpaRepository.findByTenantIdAndId(tenantId, id)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<BlockCode> findByCode(UUID tenantId, String code) {
        return jpaRepository.findByTenantIdAndCode(tenantId, code)
            .map(mapper::toDomain);
    }

    @Override
    public List<BlockCode> findAll(UUID tenantId) {
        return jpaRepository.findAllByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    public List<BlockCode> findByType(UUID tenantId, BlockCodeType type) {
        return jpaRepository.findAllByTenantIdAndType(tenantId, type).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        jpaRepository.deleteByTenantIdAndId(tenantId, id);
    }
}
