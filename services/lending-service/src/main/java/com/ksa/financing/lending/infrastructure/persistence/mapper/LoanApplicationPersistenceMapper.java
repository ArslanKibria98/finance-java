package com.ksa.financing.lending.infrastructure.persistence.mapper;

import com.ksa.financing.lending.domain.model.*;
import com.ksa.financing.lending.infrastructure.persistence.entity.LoanApplicationJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * Maps between LoanApplicationAggregate domain model and JPA entity.
 * Keeps domain layer independent of persistence concerns.
 */
@Component
public class LoanApplicationPersistenceMapper {

    public LoanApplicationJpaEntity toEntity(LoanApplicationAggregate agg) {
        var entity = new LoanApplicationJpaEntity();
        entity.setId(agg.getId().getValue());
        entity.setTenantId(agg.getTenantId());
        entity.setApplicationNumber(agg.getApplicationNumber());
        entity.setCustomerId(agg.getCustomerId());
        entity.setNationalId(agg.getNationalId());

        // Pre-qualification
        entity.setMonthlyIncome(agg.getMonthlyIncome());
        entity.setTotalExpenses(agg.getTotalExpenses());
        entity.setExistingLiabilities(agg.getExistingLiabilities());
        entity.setAdultDependents(agg.getAdultDependents());
        entity.setChildDependents(agg.getChildDependents());

        // Individual expense categories
        entity.setFoodGroceries(agg.getFoodGroceries());
        entity.setUtilities(agg.getUtilities());
        entity.setHealthcare(agg.getHealthcare());
        entity.setCommunication(agg.getCommunication());
        entity.setHousingRent(agg.getHousingRent());
        entity.setClothingEssentials(agg.getClothingEssentials());
        entity.setEducation(agg.getEducation());
        entity.setTransportation(agg.getTransportation());

        // Step 1: Basic Info
        entity.setProductId(agg.getProductId());
        entity.setProductCode(agg.getProductCode());
        entity.setProductName(agg.getProductName());
        entity.setShariaStructure(agg.getShariaStructure() != null ? agg.getShariaStructure().name() : null);
        entity.setRequestedAmount(agg.getRequestedAmount());
        entity.setRequestedTenureMonths(agg.getRequestedTenureMonths());
        entity.setPurposeOfFinance(agg.getPurposeOfFinance());
        entity.setPurposeOfFinanceOther(agg.getPurposeOfFinanceOther());
        entity.setProfitRate(agg.getProfitRate());
        entity.setProcessingFeePercent(agg.getProcessingFeePercent());
        entity.setProcessingFeeAmount(agg.getProcessingFeeAmount());
        entity.setAdminFeeAmount(agg.getAdminFeeAmount());
        entity.setApr(agg.getApr());
        entity.setPartnerId(agg.getPartnerId());
        entity.setLeadId(agg.getLeadId());

        // SafeWatch AML
        entity.setSafeWatchSessionId(agg.getSafeWatchSessionId());
        entity.setSafeWatchStatus(agg.getSafeWatchStatus());

        // Masdar Employment
        entity.setEmployerName(agg.getEmployerName());
        entity.setEmploymentSector(agg.getEmploymentSector());
        entity.setEmploymentStatus(agg.getEmploymentStatus());
        entity.setBasicSalary(agg.getBasicSalary());
        entity.setTotalSalary(agg.getTotalSalary());
        entity.setEmploymentStartDate(agg.getEmploymentStartDate());

        // AML Declaration
        entity.setAmlDeclarationCompleted(agg.isAmlDeclarationCompleted());
        entity.setAmlDeclarationAt(agg.getAmlDeclarationAt());

        // Step 2: Bank Account
        entity.setDisbursementBankCode(agg.getDisbursementBankCode());
        entity.setDisbursementBankName(agg.getDisbursementBankName());
        entity.setDisbursementIban(agg.getDisbursementIban());
        entity.setDisbursementAccountHolder(agg.getDisbursementAccountHolder());
        entity.setIbanVerified(agg.isIbanVerified());

        // Step 3: SIMAH / Eligibility
        entity.setSimahConsent(agg.isSimahConsent());
        entity.setSimahConsentAt(agg.getSimahConsentAt());
        entity.setCreditScore(agg.getCreditScore());
        entity.setSimahReferenceId(agg.getSimahReferenceId());
        entity.setVerifiedSalary(agg.getVerifiedSalary());
        entity.setMaxEligibleAmount(agg.getMaxEligibleAmount());

        // Step 4: Offer
        entity.setOfferedAmount(agg.getOfferedAmount());
        entity.setOfferedMonthlyInstallment(agg.getOfferedMonthlyInstallment());
        entity.setOfferedTotalProfit(agg.getOfferedTotalProfit());
        entity.setOfferedTotalPayable(agg.getOfferedTotalPayable());
        entity.setProcessingFee(agg.getProcessingFee());
        entity.setAdminFee(agg.getAdminFee());
        entity.setAcceptedAmount(agg.getAcceptedAmount());

        // Step 5: Contract
        entity.setContractExpiresAt(agg.getContractExpiresAt());
        entity.setAuthorizeDigitalSignature(agg.isAuthorizeDigitalSignature());
        entity.setAuthorizeSellCommodity(agg.isAuthorizeSellCommodity());
        entity.setWantPhysicalDelivery(agg.isWantPhysicalDelivery());
        entity.setCommodityTradeId(agg.getCommodityTradeId());
        entity.setOtpVerified(agg.isOtpVerified());
        entity.setOtpAttempts(agg.getOtpAttempts());
        entity.setIvrVerified(agg.isIvrVerified());
        entity.setIvrAttempts(agg.getIvrAttempts());

        // NABA
        entity.setNabaNotificationSent(agg.isNabaNotificationSent());

        // PaymentGuard
        entity.setPaymentGuardSessionId(agg.getPaymentGuardSessionId());
        entity.setPaymentGuardStatus(agg.getPaymentGuardStatus());

        // Status & Workflow
        entity.setStatus(agg.getStatus().name());
        entity.setWorkflowId(agg.getWorkflowId());
        entity.setCurrentStage(agg.getCurrentStage());
        entity.setSubmittedAt(agg.getSubmittedAt());
        entity.setExpiresAt(agg.getExpiresAt());
        entity.setIdempotencyKey(agg.getIdempotencyKey());

        // Disbursement delay (snapshot from product at apply time)
        entity.setDisbursementDurationHours(agg.getDisbursementDurationHours());
        entity.setDisbursementScheduledAt(agg.getDisbursementScheduledAt());

        // Audit
        entity.setCreatedBy(agg.getCreatedBy());
        entity.setCreatedAt(agg.getCreatedAt());
        entity.setUpdatedBy(agg.getUpdatedBy());
        entity.setUpdatedAt(agg.getUpdatedAt());
        entity.setVersion(agg.getVersion());

        return entity;
    }

    public LoanApplicationAggregate toDomain(LoanApplicationJpaEntity e) {
        var agg = LoanApplicationAggregate.reconstitute(
                LoanApplicationId.of(e.getId()),
                e.getTenantId(),
                e.getApplicationNumber(),
                e.getCustomerId(),
                e.getNationalId(),
                // Pre-qualification
                e.getMonthlyIncome(),
                e.getTotalExpenses(),
                e.getExistingLiabilities(),
                e.getAdultDependents() != null ? e.getAdultDependents() : 0,
                e.getChildDependents() != null ? e.getChildDependents() : 0,
                // Individual expense categories
                e.getFoodGroceries(),
                e.getUtilities(),
                e.getHealthcare(),
                e.getCommunication(),
                e.getHousingRent(),
                e.getClothingEssentials(),
                e.getEducation(),
                e.getTransportation(),
                // Step 1
                e.getProductId(),
                e.getProductCode(),
                e.getProductName(),
                e.getShariaStructure() != null ? ShariaStructure.valueOf(e.getShariaStructure()) : null,
                e.getRequestedAmount(),
                e.getRequestedTenureMonths() != null ? e.getRequestedTenureMonths() : 0,
                e.getPurposeOfFinance(),
                e.getPurposeOfFinanceOther(),
                e.getProfitRate(),
                e.getProcessingFeePercent(),
                e.getProcessingFeeAmount(),
                e.getAdminFeeAmount(),
                e.getApr(),
                e.getPartnerId(),
                e.getLeadId(),
                // SafeWatch
                e.getSafeWatchSessionId(),
                e.getSafeWatchStatus(),
                // Masdar Employment
                e.getEmployerName(),
                e.getEmploymentSector(),
                e.getEmploymentStatus(),
                e.getBasicSalary(),
                e.getTotalSalary(),
                e.getEmploymentStartDate(),
                // AML Declaration
                e.getAmlDeclarationCompleted() != null && e.getAmlDeclarationCompleted(),
                e.getAmlDeclarationAt(),
                // Step 2
                e.getDisbursementBankCode(),
                e.getDisbursementBankName(),
                e.getDisbursementIban(),
                e.getDisbursementAccountHolder(),
                e.getIbanVerified() != null && e.getIbanVerified(),
                // Step 3
                e.getSimahConsent() != null && e.getSimahConsent(),
                e.getSimahConsentAt(),
                e.getCreditScore() != null ? e.getCreditScore() : 0,
                e.getSimahReferenceId(),
                e.getVerifiedSalary(),
                null, null, // dbrBefore, dbrAfter — computed, not persisted
                e.getMaxEligibleAmount(),
                // Step 4
                e.getOfferedAmount(),
                e.getOfferedMonthlyInstallment(),
                e.getOfferedTotalProfit(),
                e.getOfferedTotalPayable(),
                e.getProcessingFee(),
                e.getAdminFee(),
                e.getAcceptedAmount(),
                // Step 5
                e.getContractExpiresAt(),
                e.getAuthorizeDigitalSignature() != null && e.getAuthorizeDigitalSignature(),
                e.getAuthorizeSellCommodity() != null && e.getAuthorizeSellCommodity(),
                e.getWantPhysicalDelivery() != null && e.getWantPhysicalDelivery(),
                e.getCommodityTradeId(),
                e.getOtpVerified() != null && e.getOtpVerified(),
                e.getOtpAttempts() != null ? e.getOtpAttempts() : 0,
                e.getIvrVerified() != null && e.getIvrVerified(),
                e.getIvrAttempts() != null ? e.getIvrAttempts() : 0,
                // NABA + PaymentGuard
                e.getNabaNotificationSent() != null && e.getNabaNotificationSent(),
                e.getPaymentGuardSessionId(),
                e.getPaymentGuardStatus(),
                // Status
                toDomainStatus(e.getStatus()),
                e.getWorkflowId(),
                resolveCurrentStage(e.getStatus(), e.getCurrentStage()),
                e.getSubmittedAt(),
                e.getExpiresAt(),
                e.getIdempotencyKey(),
                // Audit
                e.getCreatedBy(),
                e.getCreatedAt(),
                e.getUpdatedBy(),
                e.getUpdatedAt(),
                e.getVersion() != null ? e.getVersion() : 1
        );

        // Disbursement delay (snapshot from product at apply time)
        agg.setDisbursementDurationHours(
                e.getDisbursementDurationHours() != null ? e.getDisbursementDurationHours() : 0);
        agg.setDisbursementScheduledAt(e.getDisbursementScheduledAt());

        return agg;
    }

    private ApplicationStatus toDomainStatus(String persistedStatus) {
        if (persistedStatus == null || persistedStatus.isBlank()) {
            return ApplicationStatus.DRAFT;
        }
        var normalizedStatus = persistedStatus.trim().toUpperCase(Locale.ROOT);
        if ("MANUAL_REVIEW".equals(normalizedStatus)) {
            // Manual review is a workflow-only state; domain aggregate remains at signed stage.
            return ApplicationStatus.CONTRACT_SIGNED;
        }
        try {
            return ApplicationStatus.valueOf(normalizedStatus);
        } catch (IllegalArgumentException ex) {
            return ApplicationStatus.DRAFT;
        }
    }

    private String resolveCurrentStage(String persistedStatus, String persistedCurrentStage) {
        if (persistedCurrentStage != null && !persistedCurrentStage.isBlank()) {
            return persistedCurrentStage;
        }
        if (persistedStatus == null || persistedStatus.isBlank()) {
            return persistedCurrentStage;
        }
        var normalizedStatus = persistedStatus.trim().toUpperCase(Locale.ROOT);
        if ("MANUAL_REVIEW".equals(normalizedStatus)) {
            // Backward compatibility for historical rows where stage was not persisted.
            return "MANUAL_REVIEW";
        }
        return persistedCurrentStage;
    }
}
