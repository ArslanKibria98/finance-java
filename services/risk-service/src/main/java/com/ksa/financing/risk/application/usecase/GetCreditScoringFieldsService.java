package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.credit.CreditScoringFieldDefinition;
import com.ksa.financing.risk.domain.port.in.GetCreditScoringFieldsUseCase;
import com.ksa.financing.risk.domain.port.out.CreditScoringRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GetCreditScoringFieldsService implements GetCreditScoringFieldsUseCase {

    private static final String SEED_TENANT = "00000000-0000-0000-0000-000000000001";

    private final CreditScoringRepository creditScoringRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringFieldDefinition> getActiveFieldDefinitions(UUID tenantId) {
        log.info("Fetching active credit scoring field definitions for tenant={}", tenantId);

        var fields = creditScoringRepository.findActiveFieldDefinitions(tenantId.toString());
        if (fields.isEmpty()) {
            log.info("No field definitions for tenant={}, falling back to seed tenant", tenantId);
            fields = creditScoringRepository.findActiveFieldDefinitions(SEED_TENANT);
        }

        log.info("Found {} active field definitions", fields.size());
        return fields;
    }
}
