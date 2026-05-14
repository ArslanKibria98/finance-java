package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.application.service.DunningPolicyResolver;
import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.port.in.RequestPenaltyWaiverUseCase;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRequestRepository;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RequestPenaltyWaiverUseCaseImpl implements RequestPenaltyWaiverUseCase {

    private final PenaltyWaiverRequestRepository requestRepository;
    private final RepaymentScheduleRepository scheduleRepository;
    private final DunningPolicyResolver policyResolver;

    @Override
    @Transactional
    public PenaltyWaiverRequest submitRequest(SubmitWaiverRequestCommand command) {
        log.info("Customer {} requesting penalty waiver for loan {} installment {}",
                command.customerId(), command.loanId(), command.installmentId());

        var schedule = scheduleRepository.findActiveByLoanId(command.tenantId(), command.loanId())
                .orElseThrow(() -> NotFoundException.forEntity("RepaymentSchedule", command.loanId().toString()));

        // 1. Resolve Policy and check limits
        var productCode = schedule.getProductId() != null ? schedule.getProductId().toString() : null;
        var resolvedPolicy = policyResolver.resolve(command.tenantId(), productCode);
        var policy = resolvedPolicy.policy();

        if (!policy.isPenaltyWaiverAllowed()) {
            throw new BusinessException(ErrorCodes.Collections.WAIVER_NOT_ALLOWED, "Penalty waivers are not allowed for this product");
        }

        long approvedCount = requestRepository.countApprovedByLoanId(command.tenantId(), command.loanId());
        if (approvedCount >= policy.getMaxPenaltyWaiversAllowed()) {
            throw new BusinessException(ErrorCodes.Collections.WAIVER_LIMIT_EXCEEDED,
                    "Maximum number of penalty waivers reached for this loan (" + policy.getMaxPenaltyWaiversAllowed() + ")",
                    policy.getMaxPenaltyWaiversAllowed());
        }

        // 2. Validate installment and penalty
        var installment = schedule.getInstallments().stream()
                .filter(i -> i.getId().equals(command.installmentId()))
                .findFirst()
                .orElseThrow(() -> NotFoundException.forEntity("Installment", command.installmentId().toString()));

        BigDecimal remainingPenalty = installment.getRemainingPenalty();
        if (remainingPenalty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.Collections.NO_PENALTY_TO_WAIVE, "No penalty remaining to waive on this installment");
        }

        BigDecimal amount = command.amount() != null ? command.amount().min(remainingPenalty) : remainingPenalty;

        // 3. Create request
        var request = PenaltyWaiverRequest.create(
                command.tenantId(),
                command.loanId(),
                command.applicationId(),
                command.invoiceId(),
                command.installmentId(),
                amount,
                command.reason(),
                command.customerId()
        );

        return requestRepository.save(request);
    }
}
