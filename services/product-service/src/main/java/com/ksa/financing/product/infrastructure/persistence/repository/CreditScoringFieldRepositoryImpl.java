package com.ksa.financing.product.infrastructure.persistence.repository;

import com.ksa.financing.product.domain.model.CreditScoringFieldDefinition;
import com.ksa.financing.product.domain.model.CreditScoringFieldOption;
import com.ksa.financing.product.domain.port.out.CreditScoringFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
@Slf4j
public class CreditScoringFieldRepositoryImpl implements CreditScoringFieldRepository {

    private final JpaCreditScoringFieldDefinitionRepository jpaFieldDefRepo;
    private final JpaCreditScoringFieldOptionRepository jpaFieldOptionRepo;

    @Override
    public List<CreditScoringFieldDefinition> findAllWithOptions(UUID tenantId) {
        log.debug("Loading credit scoring field definitions with options for tenant: {}", tenantId);

        var definitions = jpaFieldDefRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId);
        var allOptions = jpaFieldOptionRepo.findByTenantIdAndActiveTrueOrderBySortOrder(tenantId);

        Map<UUID, List<CreditScoringFieldOption>> optionsByFieldId = allOptions.stream()
                .collect(Collectors.groupingBy(
                        o -> o.getFieldDefinitionId(),
                        Collectors.mapping(
                                o -> new CreditScoringFieldOption(
                                        o.getId(), o.getOptionKey(),
                                        o.getLabelEn(), o.getLabelAr(), o.getSortOrder()),
                                Collectors.toList())));

        return definitions.stream()
                .map(d -> new CreditScoringFieldDefinition(
                        d.getId(), d.getFieldKey(), d.getNameEn(), d.getNameAr(),
                        d.getDataType(), d.isActive(), d.getSortOrder(),
                        optionsByFieldId.getOrDefault(d.getId(), Collections.emptyList())))
                .toList();
    }
}
