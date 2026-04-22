package com.ksa.financing.ledger.infrastructure.persistence.repository;

import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;
import com.ksa.financing.ledger.domain.port.out.CoaConfigurationRepository;
import com.ksa.financing.ledger.infrastructure.persistence.mapper.CoaConfigurationPersistenceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CoaConfigurationRepositoryImpl implements CoaConfigurationRepository {

    private final JpaCoaConfigurationProfileRepository profileRepository;
    private final JpaCoaConfigurationMappingRepository mappingRepository;
    private final CoaConfigurationPersistenceMapper mapper;

    @Override
    public CoaConfigurationProfile saveProfile(CoaConfigurationProfile profile) {
        return mapper.toDomain(profileRepository.save(mapper.toJpaEntity(profile)));
    }

    @Override
    public Optional<CoaConfigurationProfile> findProfileById(UUID tenantId, UUID profileId) {
        return profileRepository.findByTenantIdAndId(tenantId, profileId).map(mapper::toDomain);
    }

    @Override
    public List<CoaConfigurationProfile> findProfilesByProductCode(UUID tenantId, String productCode) {
        return profileRepository.findAllByTenantIdAndProductCodeOrderByCreatedAtDesc(tenantId, productCode.trim().toUpperCase())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CoaConfigurationMapping> findMappingsByProfileId(UUID tenantId, UUID profileId) {
        return mappingRepository.findAllByTenantIdAndProfileId(tenantId, profileId).stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    public void replaceMappings(UUID tenantId, UUID profileId, List<CoaConfigurationMapping> mappings) {
        mappingRepository.deleteAllByTenantIdAndProfileId(tenantId, profileId);
        if (mappings.isEmpty()) {
            return;
        }
        mappingRepository.saveAll(mappings.stream().map(mapper::toJpaEntity).toList());
    }
}
