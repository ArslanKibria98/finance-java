package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.domain.model.PenaltyWaiverRequest;
import com.ksa.financing.collections.domain.port.in.ApprovePenaltyWaiverUseCase;
import com.ksa.financing.collections.domain.port.in.WaivePenaltyUseCase;
import com.ksa.financing.collections.domain.port.out.PenaltyWaiverRequestRepository;
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
public class ApprovePenaltyWaiverUseCaseImpl implements ApprovePenaltyWaiverUseCase {

    private final PenaltyWaiverRequestRepository requestRepository;
    private final WaivePenaltyUseCase waivePenaltyUseCase;

    @Override
    @Transactional
    public void approve(UUID tenantId, UUID requestId, BigDecimal approvedAmount, UUID adminId) {
        log.info("Admin {} approving penalty waiver request {} with amount={}",
                adminId, requestId, approvedAmount);

        var request = requestRepository.findById(tenantId, requestId)
                .orElseThrow(() -> NotFoundException.forEntity("PenaltyWaiverRequest", requestId.toString()));

        BigDecimal requested = request.getRequestedAmount();
        BigDecimal effective = approvedAmount != null ? approvedAmount : requested;

        if (effective.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Approved amount must be greater than zero");
        }
        if (requested != null && effective.compareTo(requested) > 0) {
            throw new BusinessException(ErrorCodes.VALIDATION_FAILED,
                    "Approved amount (" + effective + ") cannot exceed requested amount (" + requested + ")");
        }

        request.approve(adminId);
        requestRepository.save(request);

        // Integrate with existing waiver logic to actually apply the waiver
        var command = new WaivePenaltyUseCase.WaivePenaltyCommand(
                tenantId,
                request.getLoanId(),
                request.getInstallmentId(),
                effective,
                "APPROVED_REQUEST: " + request.getReason(),
                "WAIVER-REQ-" + request.getId(),
                adminId
        );

        waivePenaltyUseCase.waivePenalty(command);
    }

    @Override
    @Transactional
    public void reject(UUID tenantId, UUID requestId, String reason, UUID adminId) {
        log.info("Admin {} rejecting penalty waiver request {} with reason: {}", adminId, requestId, reason);

        var request = requestRepository.findById(tenantId, requestId)
                .orElseThrow(() -> NotFoundException.forEntity("PenaltyWaiverRequest", requestId.toString()));

        request.reject(adminId, reason);
        requestRepository.save(request);
    }
}
