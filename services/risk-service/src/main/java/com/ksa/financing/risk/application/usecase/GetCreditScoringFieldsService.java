package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.port.in.GetCreditScoringFieldsUseCase;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetCreditScoringFieldsService implements GetCreditScoringFieldsUseCase {

    private static final String SEED_TENANT = "00000000-0000-0000-0000-000000000001";

    private final CreditScoringRepository creditScoringRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CreditScoringFieldDefinition> getActiveFieldDefinitions(UUID tenantId, PageQuery pageQuery) {
        log.info("Fetching active credit scoring field definitions for tenant={}", tenantId);

        var page = creditScoringRepository.findActiveFieldDefinitions(tenantId.toString(), pageQuery);
        if (page.content().isEmpty() && page.pagination().totalElements() == 0L) {
            log.info("No field definitions for tenant={}, falling back to seed tenant", tenantId);
            page = creditScoringRepository.findActiveFieldDefinitions(SEED_TENANT, pageQuery);
        }

        log.info("Found {} active field definitions (total={})",
                page.content().size(), page.pagination().totalElements());
        return page;
    }
}
