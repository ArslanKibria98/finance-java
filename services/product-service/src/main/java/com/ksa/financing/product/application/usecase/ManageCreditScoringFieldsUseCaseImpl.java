package com.ksa.financing.product.application.usecase;

import com.ksa.financing.product.domain.model.CreditScoringFieldDefinition;
import com.ksa.financing.product.domain.port.in.ManageCreditScoringFieldsUseCase;
import com.ksa.financing.product.domain.port.out.CreditScoringFieldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageCreditScoringFieldsUseCaseImpl implements ManageCreditScoringFieldsUseCase {

    private final CreditScoringFieldRepository creditScoringFieldRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringFieldDefinition> listFieldDefinitions(UUID tenantId) {
        log.info("Listing credit scoring field definitions for tenant: {}", tenantId);
        return creditScoringFieldRepository.findAllWithOptions(tenantId);
    }
}
