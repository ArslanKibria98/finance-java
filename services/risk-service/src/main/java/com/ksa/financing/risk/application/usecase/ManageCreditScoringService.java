package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.risk.domain.model.credit.CreditScoringCriteria;
import com.ksa.financing.risk.domain.model.credit.CreditScoringOperator;
import com.ksa.financing.risk.domain.model.credit.CreditScoringRule;
import com.ksa.financing.risk.domain.port.in.ManageCreditScoringUseCase;
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
public class ManageCreditScoringService implements ManageCreditScoringUseCase {

    private final CreditScoringRepository creditScoringRepository;

    @Override
    @Transactional(readOnly = true)
    public List<CreditScoringCriteria> getCriteriaByProduct(UUID tenantId, UUID productId) {
        log.info("Fetching credit scoring criteria for product={} tenant={}", productId, tenantId);
        return creditScoringRepository.findCriteriaByProductId(tenantId, productId);
    }

    @Override
    @Transactional
    public void saveCriteria(UUID tenantId, UUID productId, List<CreditScoringCriteriaCommand> commands) {
        log.info("Saving {} credit scoring criteria for product={} tenant={}", commands.size(), productId, tenantId);

        var criteria = commands.stream()
                .map(cmd -> new CreditScoringCriteria(
                        null,
                        tenantId,
                        productId,
                        cmd.fieldDefinitionId(),
                        cmd.customName(),
                        cmd.custom(),
                        cmd.enabled(),
                        cmd.sortOrder(),
                        cmd.rules() == null ? List.of() : cmd.rules().stream()
                                .map(r -> new CreditScoringRule(
                                        null,
                                        tenantId,
                                        null,
                                        CreditScoringOperator.valueOf(r.operator()),
                                        r.value(),
                                        r.weight(),
                                        r.percentage()
                                ))
                                .toList()
                ))
                .toList();

        creditScoringRepository.saveCriteria(tenantId, productId, criteria);
        log.info("Credit scoring criteria saved for product={}", productId);
    }

    @Override
    @Transactional
    public void deleteCriteriaByProduct(UUID tenantId, UUID productId) {
        log.info("Deleting credit scoring criteria for product={} tenant={}", productId, tenantId);
        creditScoringRepository.deleteCriteriaByProductId(tenantId, productId);
        log.info("Credit scoring criteria deleted for product={}", productId);
    }
}
