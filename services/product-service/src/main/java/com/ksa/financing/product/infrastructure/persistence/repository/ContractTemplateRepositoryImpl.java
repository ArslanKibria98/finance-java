package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.ContractTemplate;
import com.ksa.financing.product.domain.port.out.ContractTemplateRepository;
import com.ksa.financing.product.infrastructure.persistence.mapper.ContractTemplatePersistenceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ContractTemplateRepositoryImpl implements ContractTemplateRepository {

    private final JpaContractTemplateRepository jpaContractTemplateRepository;

    @Override
    public List<ContractTemplate> findAllByTenant(UUID tenantId) {
        return jpaContractTemplateRepository.findAllByTenantIdAndIsActiveTrue(tenantId)
                .stream()
                .map(ContractTemplatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ContractTemplate> findByProduct(UUID tenantId, UUID productId) {
        return jpaContractTemplateRepository.findAllByTenantIdAndProductIdAndIsActiveTrue(tenantId, productId)
                .stream()
                .map(ContractTemplatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public List<ContractTemplate> findByType(UUID tenantId, UUID typeId) {
        return jpaContractTemplateRepository.findAllByTenantIdAndTypeIdAndIsActiveTrue(tenantId, typeId)
                .stream()
                .map(ContractTemplatePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<ContractTemplate> findById(UUID id) {
        return jpaContractTemplateRepository.findById(id)
                .map(ContractTemplatePersistenceMapper::toDomain);
    }

    @Override
    public ContractTemplate save(ContractTemplate template) {
        var entity = ContractTemplatePersistenceMapper.toEntity(template);
        var saved = jpaContractTemplateRepository.save(entity);
        return ContractTemplatePersistenceMapper.toDomain(saved);
    }

    @Override
    public void delete(UUID id) {
        jpaContractTemplateRepository.deleteById(id);
    }
}
