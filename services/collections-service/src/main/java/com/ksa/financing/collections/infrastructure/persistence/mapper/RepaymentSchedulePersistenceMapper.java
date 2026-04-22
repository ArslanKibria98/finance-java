package com.ksa.financing.collections.infrastructure.persistence.mapper;

import com.ksa.financing.collections.domain.model.Installment;
import com.ksa.financing.collections.domain.model.InstallmentStatus;
import com.ksa.financing.collections.domain.model.RepaymentScheduleAggregate;
import com.ksa.financing.collections.domain.model.RepaymentScheduleId;
import com.ksa.financing.collections.infrastructure.persistence.entity.InstallmentJpaEntity;
import com.ksa.financing.collections.infrastructure.persistence.entity.RepaymentScheduleJpaEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class RepaymentSchedulePersistenceMapper {

    public RepaymentScheduleJpaEntity toJpa(RepaymentScheduleAggregate aggregate) {
        var entity = new RepaymentScheduleJpaEntity();
        entity.setId(aggregate.getId().getValue());
        entity.setTenantId(aggregate.getTenantId());
        entity.setScheduleNumber(aggregate.getScheduleNumber());
        entity.setLoanId(aggregate.getLoanId());
        entity.setProductId(aggregate.getProductId());
        entity.setVersion(aggregate.getVersion());
        entity.setActive(aggregate.isActive());
        entity.setTotalInstallments(aggregate.getTotalInstallments());
        entity.setTotalPrincipal(aggregate.getTotalPrincipal());
        entity.setTotalProfit(aggregate.getTotalProfit());
        entity.setTotalAmount(aggregate.getTotalAmount());
        entity.setFirstDueDate(aggregate.getFirstDueDate());
        entity.setLastDueDate(aggregate.getLastDueDate());
        entity.setCreatedAt(aggregate.getCreatedAt());
        entity.setCreatedBy(aggregate.getCreatedBy());

        // Map installments - update in place to preserve Hibernate tracking
        var existingInstallments = entity.getInstallments();
        for (var inst : aggregate.getInstallments()) {
            var jpaInst = toInstallmentJpa(inst, entity);
            // Find and update existing or add new
            var existing = existingInstallments.stream()
                    .filter(e -> e.getId().equals(jpaInst.getId()))
                    .findFirst();
            if (existing.isPresent()) {
                updateInstallmentInPlace(existing.get(), jpaInst);
            } else {
                existingInstallments.add(jpaInst);
            }
        }

        return entity;
    }

    /**
     * Merge aggregate state into an already-managed JPA entity so Hibernate's
     * optimistic-locking versions on the schedule and its installments are preserved.
     * Required because a detached graph rebuilt with version=0 causes
     * StaleObjectStateException on cascade merge.
     */
    public void mergeInto(RepaymentScheduleJpaEntity managed, RepaymentScheduleAggregate aggregate) {
        managed.setActive(aggregate.isActive());
        managed.setProductId(aggregate.getProductId());
        managed.setTotalInstallments(aggregate.getTotalInstallments());
        managed.setTotalPrincipal(aggregate.getTotalPrincipal());
        managed.setTotalProfit(aggregate.getTotalProfit());
        managed.setTotalAmount(aggregate.getTotalAmount());
        managed.setFirstDueDate(aggregate.getFirstDueDate());
        managed.setLastDueDate(aggregate.getLastDueDate());

        var managedById = managed.getInstallments().stream()
                .collect(java.util.stream.Collectors.toMap(InstallmentJpaEntity::getId, e -> e));

        for (var inst : aggregate.getInstallments()) {
            var existing = managedById.get(inst.getId());
            if (existing != null) {
                existing.setDueDate(inst.getDueDate());  // test-support / rescheduling can shift this
                existing.setLatePenaltyAmount(inst.getLatePenaltyAmount());
                existing.setTotalAmount(inst.getTotalAmount());
                existing.setPaidPrincipal(inst.getPaidPrincipal());
                existing.setPaidProfit(inst.getPaidProfit());
                existing.setPaidFee(inst.getPaidFee());
                existing.setPaidTotal(inst.getPaidTotal());
                existing.setStatus(inst.getStatus().name());
                existing.setDpd(inst.getDpd());
                existing.setPaidDate(inst.getPaidDate());
                existing.setUpdatedAt(LocalDateTime.now());
            } else {
                managed.getInstallments().add(toInstallmentJpa(inst, managed));
            }
        }
    }

    private void updateInstallmentInPlace(InstallmentJpaEntity entity, InstallmentJpaEntity newData) {
        entity.setDueDate(newData.getDueDate());
        entity.setLatePenaltyAmount(newData.getLatePenaltyAmount());
        entity.setTotalAmount(newData.getTotalAmount());
        entity.setPaidPrincipal(newData.getPaidPrincipal());
        entity.setPaidProfit(newData.getPaidProfit());
        entity.setPaidFee(newData.getPaidFee());
        entity.setPaidTotal(newData.getPaidTotal());
        entity.setStatus(newData.getStatus());
        entity.setDpd(newData.getDpd());
        entity.setPaidDate(newData.getPaidDate());
        entity.setUpdatedAt(LocalDateTime.now());
    }

    public InstallmentJpaEntity toInstallmentJpa(Installment inst, RepaymentScheduleJpaEntity scheduleEntity) {
        var entity = new InstallmentJpaEntity();
        entity.setId(inst.getId());
        entity.setTenantId(inst.getTenantId());
        entity.setSchedule(scheduleEntity);
        entity.setLoanId(inst.getLoanId());
        entity.setInstallmentNumber(inst.getInstallmentNumber());
        entity.setDueDate(inst.getDueDate());
        entity.setPrincipalAmount(inst.getPrincipalAmount());
        entity.setProfitAmount(inst.getProfitAmount());
        entity.setFeeAmount(inst.getFeeAmount());
        entity.setLatePenaltyAmount(inst.getLatePenaltyAmount() != null ? inst.getLatePenaltyAmount() : java.math.BigDecimal.ZERO);
        entity.setTotalAmount(inst.getTotalAmount());
        entity.setPaidPrincipal(inst.getPaidPrincipal());
        entity.setPaidProfit(inst.getPaidProfit());
        entity.setPaidFee(inst.getPaidFee());
        entity.setPaidTotal(inst.getPaidTotal());
        entity.setStatus(inst.getStatus().name());
        entity.setDpd(inst.getDpd());
        entity.setPaidDate(inst.getPaidDate());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public RepaymentScheduleAggregate toDomain(RepaymentScheduleJpaEntity entity) {
        List<Installment> installments = entity.getInstallments().stream()
                .map(this::toInstallmentDomain)
                .toList();

        return RepaymentScheduleAggregate.reconstitute(
                RepaymentScheduleId.of(entity.getId()),
                entity.getTenantId(),
                entity.getScheduleNumber(),
                entity.getLoanId(),
                entity.getProductId(),
                entity.getVersion(),
                entity.isActive(),
                entity.getTotalPrincipal(),
                entity.getTotalProfit(),
                entity.getFirstDueDate(),
                entity.getLastDueDate(),
                installments,
                entity.getCreatedAt(),
                entity.getCreatedBy());
    }

    public Installment toInstallmentDomain(InstallmentJpaEntity entity) {
        return Installment.reconstitute(
                entity.getId(),
                entity.getTenantId(),
                entity.getSchedule() != null ? entity.getSchedule().getId() : null,
                entity.getLoanId(),
                entity.getInstallmentNumber(),
                entity.getDueDate(),
                entity.getPrincipalAmount(),
                entity.getProfitAmount(),
                entity.getFeeAmount(),
                entity.getLatePenaltyAmount(),
                entity.getTotalAmount(),
                entity.getPaidPrincipal(),
                entity.getPaidProfit(),
                entity.getPaidFee(),
                entity.getPaidTotal(),
                InstallmentStatus.valueOf(entity.getStatus()),
                entity.getDpd(),
                entity.getPaidDate());
    }
}
