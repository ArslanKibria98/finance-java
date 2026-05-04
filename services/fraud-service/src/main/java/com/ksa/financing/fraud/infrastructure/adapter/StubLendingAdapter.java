package com.ksa.financing.fraud.infrastructure.adapter;

import com.ksa.financing.fraud.domain.port.out.LendingPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@Slf4j
public class StubLendingAdapter implements LendingPort {

    @Override
    public Optional<DisbursementInfo> fetchDisbursement(UUID tenantId, String loanApplicationId) {
        log.debug("Stub fetchDisbursement: tenantId={} appId={} → empty", tenantId, loanApplicationId);
        return Optional.empty();
    }

    @Override
    public Optional<ScheduledInstallment> fetchScheduledInstallment(UUID tenantId, String loanId, int installmentNo) {
        log.debug("Stub fetchScheduledInstallment: tenantId={} loanId={} no={} → empty", tenantId, loanId, installmentNo);
        return Optional.empty();
    }
}
