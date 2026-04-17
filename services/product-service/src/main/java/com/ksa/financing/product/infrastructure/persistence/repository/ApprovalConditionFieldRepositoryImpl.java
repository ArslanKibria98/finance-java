package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.ApprovalConditionFieldDefinition;
import com.ksa.financing.product.domain.model.ApprovalConditionFieldOption;
import com.ksa.financing.product.domain.port.out.ApprovalConditionFieldRepository;
import com.ksa.financing.product.infrastructure.persistence.entity.ApprovalConditionFieldDefinitionJpaEntity;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ApprovalConditionFieldRepositoryImpl implements ApprovalConditionFieldRepository {

    private final JpaApprovalConditionFieldDefinitionRepository jpaFieldDefRepo;
    private final JpaApprovalConditionFieldOptionRepository jpaFieldOptionRepo;

    @Override
    public List<ApprovalConditionFieldDefinition> findAllWithOptions(UUID tenantId) {
        log.debug("Loading approval condition field definitions with options for tenant: {}", tenantId);

        var definitions = jpaFieldDefRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId);
        var allOptions = jpaFieldOptionRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId);

        Map<UUID, List<ApprovalConditionFieldOption>> optionsByFieldId = allOptions.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getFieldDefinitionId(),
                        Collectors.mapping(
                                o -> new ApprovalConditionFieldOption(
                                        o.getId(), o.getOptionKey(),
                                        o.getLabelEn(), o.getLabelAr(), o.getSortOrder()),
                                Collectors.toList())));

        return definitions.stream()
                .map(d -> toDomain(d, optionsByFieldId.getOrDefault(d.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    public Optional<ApprovalConditionFieldDefinition> findById(UUID tenantId, UUID id) {
        return jpaFieldDefRepo.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .map(d -> toDomain(d, Collections.emptyList()));
    }

    @Override
    public boolean existsByFieldKey(UUID tenantId, String fieldKey) {
        return jpaFieldDefRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId).stream()
                .anyMatch(d -> d.getFieldKey().equals(fieldKey));
    }

    @Override
    public ApprovalConditionFieldDefinition save(UUID tenantId, ApprovalConditionFieldDefinition field) {
        var entity = new ApprovalConditionFieldDefinitionJpaEntity();
        entity.setTenantId(tenantId);
        entity.setFieldKey(field.fieldKey());
        entity.setNameEn(field.nameEn());
        entity.setNameAr(field.nameAr());
        entity.setDataType(field.dataType());
        entity.setActive(true);
        entity.setSortOrder(field.sortOrder());
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaFieldDefRepo.save(entity);
        log.info("Created approval condition field: {} for tenant: {}", saved.getFieldKey(), tenantId);
        return toDomain(saved, Collections.emptyList());
    }

    @Override
    public ApprovalConditionFieldDefinition update(UUID tenantId, UUID id, ApprovalConditionFieldDefinition field) {
        var entity = jpaFieldDefRepo.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> NotFoundException.forEntity("ApprovalConditionField", id.toString()));

        entity.setNameEn(field.nameEn());
        entity.setNameAr(field.nameAr());
        entity.setDataType(field.dataType());
        entity.setActive(field.active());
        entity.setSortOrder(field.sortOrder());
        entity.setUpdatedAt(OffsetDateTime.now());

        var saved = jpaFieldDefRepo.save(entity);
        log.info("Updated approval condition field: {} for tenant: {}", saved.getFieldKey(), tenantId);
        return toDomain(saved, Collections.emptyList());
    }

    @Override
    public void delete(UUID tenantId, UUID id) {
        var entity = jpaFieldDefRepo.findById(id)
                .filter(e -> e.getTenantId().equals(tenantId))
                .orElseThrow(() -> NotFoundException.forEntity("ApprovalConditionField", id.toString()));

        jpaFieldDefRepo.delete(entity);
        log.info("Deleted approval condition field: {} for tenant: {}", entity.getFieldKey(), tenantId);
    }

    private ApprovalConditionFieldDefinition toDomain(
            ApprovalConditionFieldDefinitionJpaEntity entity,
            List<ApprovalConditionFieldOption> options) {
        return new ApprovalConditionFieldDefinition(
                entity.getId(), entity.getFieldKey(), entity.getNameEn(), entity.getNameAr(),
                entity.getDataType(), entity.isActive(), entity.getSortOrder(), options);
    }
}
