package com.ksa.financing.collections.application.usecase;

import com.ksa.financing.collections.application.service.DelinquencyRulesResolver;
import com.ksa.financing.collections.application.service.EarlySettlementMatcher;
import com.ksa.financing.collections.domain.model.DelinquencyRule;
import com.ksa.financing.collections.domain.model.DelinquencyType;
import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.*;
import com.ksa.financing.collections.domain.port.in.ManageSettlementUseCase;
import com.ksa.financing.collections.domain.port.out.EventPublisher;
import com.ksa.financing.collections.domain.port.out.RepaymentScheduleRepository;
import com.ksa.financing.collections.domain.port.out.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageSettlementUseCaseImpl implements ManageSettlementUseCase {

    private final RepaymentScheduleRepository scheduleRepository;
    private final SettlementRepository settlementRepository;
    private final EventPublisher eventPublisher;
    private final DelinquencyRulesResolver rulesResolver;
    private final EarlySettlementMatcher matcher;

    @Override
    @Transactional(readOnly = true)
    public SettlementQuote getSettlementQuote(UUID tenantId, UUID loanId) {
        log.info("Calculating settlement quote for loan: {} tenant: {}", loanId, tenantId);

        var schedule = scheduleRepository.findActiveByLoanId(tenantId, loanId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Active repayment schedule not found for loan: " + loanId));

        LocalDate today = LocalDate.now();

        BigDecimal outstandingPrincipal = sumOutstanding(schedule, Installment::getOutstandingPrincipal);
        BigDecimal outstandingProfit = sumOutstanding(schedule, Installment::getOutstandingProfit);
        BigDecimal outstandingFees = sumOutstanding(schedule, Installment::getOutstandingFee);

        // Sharia: unearned profit (future installments) waived 100% = Ibra
        BigDecimal unearnedProfit = schedule.getInstallments().stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID && i.getStatus() != InstallmentStatus.WAIVED)
                .filter(i -> i.getDueDate().isAfter(today))
                .map(Installment::getOutstandingProfit)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);

        // EARLY_SETTLEMENT DelinquencyRule (type=1) — admin-configured discount on top of Ibra
        BigDecimal earlySettlementDiscount = computeEarlySettlementDiscount(tenantId, schedule, today);

        BigDecimal settlementAmount = outstandingPrincipal
                .add(outstandingProfit.subtract(unearnedProfit))
                .add(outstandingFees)
                .subtract(earlySettlementDiscount)
                .max(BigDecimal.ZERO)
                .setScale(6, RoundingMode.HALF_UP);

        return new SettlementQuote(
                loanId,
                outstandingPrincipal,
                outstandingProfit,
                outstandingFees,
                outstandingPrincipal.add(outstandingProfit).add(outstandingFees),
                unearnedProfit,
                earlySettlementDiscount,
                settlementAmount,
                today,
                today.plusDays(1)
        );
    }

    @Override
    @Transactional
    public SettlementResponse initiateSettlement(InitiateSettlementCommand command) {
        log.info("Initiating {} for loan: {} tenant: {}",
                command.settlementType(), command.loanId(), command.tenantId());

        // 1. Idempotency check
        var existing = settlementRepository.findByIdempotencyKey(command.tenantId(), command.idempotencyKey());
        if (existing.isPresent()) {
            var s = existing.get();
            return new SettlementResponse(s.getId().value(), s.getSettlementNumber(), s.getSettlementAmount(), s.getIbraAmount(), s.getStatus().name());
        }

        var quote = getSettlementQuote(command.tenantId(), command.loanId());

        BigDecimal settlementAmount = command.partialAmount() != null
                ? command.partialAmount()
                : quote.settlementAmount();

        String settlementNumber = "SETT-" + System.currentTimeMillis();

        var settlement = SettlementAggregate.initiate(
                command.tenantId(),
                settlementNumber,
                command.loanId(),
                command.customerId(),
                command.settlementType(),
                settlementAmount,
                quote.ibraAmount(),
                quote.earlySettlementDiscount(),
                LocalDate.now(),
                command.idempotencyKey()
        );

        settlement = settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.getUncommittedEvents());
        settlement.markEventsAsCommitted();

        return new SettlementResponse(
                settlement.getId().value(),
                settlement.getSettlementNumber(),
                settlement.getSettlementAmount(),
                settlement.getIbraAmount(),
                settlement.getStatus().name()
        );
    }

    @Override
    @Transactional
    public SettlementResponse confirmSettlement(UUID tenantId, UUID settlementId, UUID paymentId) {
        log.info("Confirming settlement: {} payment: {}", settlementId, paymentId);

        var settlement = settlementRepository.findById(tenantId, SettlementId.of(settlementId))
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        settlement.complete(paymentId);
        
        settlement = settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.getUncommittedEvents());
        settlement.markEventsAsCommitted();

        return new SettlementResponse(
                settlement.getId().value(),
                settlement.getSettlementNumber(),
                settlement.getSettlementAmount(),
                settlement.getIbraAmount(),
                settlement.getStatus().name()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SettlementResponse getSettlement(UUID tenantId, UUID settlementId) {
        var settlement = settlementRepository.findById(tenantId, SettlementId.of(settlementId))
                .orElseThrow(() -> new IllegalArgumentException("Settlement not found: " + settlementId));

        return new SettlementResponse(
                settlement.getId().value(),
                settlement.getSettlementNumber(),
                settlement.getSettlementAmount(),
                settlement.getIbraAmount(),
                settlement.getStatus().name()
        );
    }

    // ─────────────────────────────────────────────
    // EARLY_SETTLEMENT rule application
    // ─────────────────────────────────────────────

    private BigDecimal computeEarlySettlementDiscount(UUID tenantId, RepaymentScheduleAggregate schedule, LocalDate today) {
        UUID productId = schedule.getProductId();
        if (productId == null) {
            return BigDecimal.ZERO;
        }
        Optional<DelinquencyRule> ruleOpt = rulesResolver.rule(tenantId, productId, DelinquencyType.EARLY_SETTLEMENT);
        if (ruleOpt.isEmpty()) {
            return BigDecimal.ZERO;
        }
        DelinquencyRule rule = ruleOpt.get();

        // New Logic: Support Principle Based Settlement (Image 3)
        if (rule.getSettlementStrategy() == com.ksa.financing.collections.domain.model.EarlySettlementStrategy.PRINCIPLE_BASED) {
            // Formula: Discount = settlementMonths * settlementAmountPerMonth
            // We give a flat discount as defined in the principle-based rule.
            return rule.getSettlementAmountPerMonth()
                    .multiply(BigDecimal.valueOf(rule.getSettlementMonths()))
                    .setScale(6, RoundingMode.HALF_UP);
        }

        // Existing Logic: Invoice Based (Default)
        BigDecimal total = BigDecimal.ZERO;
        for (Installment inst : schedule.getInstallments()) {
            if (inst.getStatus() == InstallmentStatus.PAID || inst.getStatus() == InstallmentStatus.WAIVED) {
                continue;
            }
            BigDecimal discount = discountForInstallment(rule, inst, today);
            total = total.add(discount);
        }
        return total.setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * Delegates the match to {@link EarlySettlementMatcher} so the rules are identical across
     * the quote, the per-installment flag, and the cross-service eligibility endpoint.
     */
    private BigDecimal discountForInstallment(DelinquencyRule rule, Installment inst, LocalDate today) {
        return matcher.match(rule, inst, today)
                .map(m -> m.isPercentage()
                        ? inst.getOutstandingAmount()
                                .multiply(m.discountPercentage() != null ? m.discountPercentage() : BigDecimal.ZERO)
                                .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
                        : (m.discountAmount() != null ? m.discountAmount() : BigDecimal.ZERO))
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal sumOutstanding(RepaymentScheduleAggregate schedule,
                                       java.util.function.Function<Installment, BigDecimal> extractor) {
        return schedule.getInstallments().stream()
                .filter(i -> i.getStatus() != InstallmentStatus.PAID && i.getStatus() != InstallmentStatus.WAIVED)
                .map(extractor)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(6, RoundingMode.HALF_UP);
    }
}
