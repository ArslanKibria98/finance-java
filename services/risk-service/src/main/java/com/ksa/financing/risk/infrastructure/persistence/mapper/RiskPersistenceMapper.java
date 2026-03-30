package com.ksa.financing.risk.infrastructure.persistence.mapper;

import com.ksa.financing.risk.domain.model.assessment.AnswerChangeReason;
import com.ksa.financing.risk.domain.model.assessment.AnswerVersionStatus;
import com.ksa.financing.risk.domain.model.assessment.AssessmentAnswer;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSession;
import com.ksa.financing.risk.domain.model.assessment.AssessmentSessionStatus;
import com.ksa.financing.risk.domain.model.audit.AuditEntry;
import com.ksa.financing.risk.domain.model.audit.AuditEntityType;
import com.ksa.financing.risk.domain.model.lov.LovCategoryType;
import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.model.lov.LovSet;
import com.ksa.financing.risk.domain.model.parameter.*;
import com.ksa.financing.risk.domain.model.review.ReviewAction;
import com.ksa.financing.risk.domain.model.review.ReviewRecommendation;
import com.ksa.financing.risk.domain.model.review.ReviewTask;
import com.ksa.financing.risk.domain.model.review.ReviewTaskStatus;
import com.ksa.financing.risk.domain.model.scenario.ScenarioRule;
import com.ksa.financing.risk.domain.model.scenario.ThirdPartyCheckType;
import com.ksa.financing.risk.domain.model.status.*;
import com.ksa.financing.risk.domain.model.tenant.TenantConfig;
import com.ksa.financing.risk.domain.model.tenant.TenantStatus;
import com.ksa.financing.risk.infrastructure.persistence.entity.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class RiskPersistenceMapper {

    private RiskPersistenceMapper() {}

    private static Instant toInstant(OffsetDateTime odt) { return odt != null ? odt.toInstant() : null; }
    private static OffsetDateTime toOdt(Instant i) { return i != null ? i.atOffset(ZoneOffset.UTC) : null; }

    // ===== TenantConfig =====
    public static TenantConfig toDomain(TenantConfigJpaEntity e) {
        var d = new TenantConfig();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setTenantName(e.getTenantName()); d.setTenantNameAr(e.getTenantNameAr());
        d.setStatus(e.getStatus() != null ? TenantStatus.valueOf(e.getStatus().name()) : null);
        d.setCustomerRiskEnabled(e.isCustomerRiskEnabled());
        d.setBusinessRiskEnabled(e.isBusinessRiskEnabled());
        d.setLoanRiskEnabled(e.isLoanRiskEnabled());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static TenantConfigJpaEntity toEntity(TenantConfig d) {
        var e = new TenantConfigJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setTenantName(d.getTenantName()); e.setTenantNameAr(d.getTenantNameAr());
        e.setStatus(d.getStatus() != null ? TenantConfigJpaEntity.TenantStatusEnum.valueOf(d.getStatus().name()) : null);
        e.setCustomerRiskEnabled(d.isCustomerRiskEnabled());
        e.setBusinessRiskEnabled(d.isBusinessRiskEnabled());
        e.setLoanRiskEnabled(d.isLoanRiskEnabled());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== LovSet =====
    public static LovSet toDomain(LovSetJpaEntity e) {
        var d = new LovSet();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setCode(e.getCode()); d.setNameEn(e.getNameEn()); d.setNameAr(e.getNameAr());
        d.setCategoryType(e.getCategoryType() != null ? LovCategoryType.valueOf(e.getCategoryType().name()) : null);
        d.setCurrentVersion(e.getCurrentVersion()); d.setActive(e.isActive());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static LovSetJpaEntity toEntity(LovSet d) {
        var e = new LovSetJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setCode(d.getCode()); e.setNameEn(d.getNameEn()); e.setNameAr(d.getNameAr());
        e.setCategoryType(d.getCategoryType() != null ? LovSetJpaEntity.LovCategoryTypeEnum.valueOf(d.getCategoryType().name()) : null);
        e.setCurrentVersion(d.getCurrentVersion()); e.setActive(d.isActive());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== LovEntry =====
    public static LovEntry toDomain(LovEntryJpaEntity e) {
        var d = new LovEntry();
        d.setId(e.getId()); d.setLovSetId(e.getLovSetId()); d.setTenantId(e.getTenantId());
        d.setFactorCode(e.getFactorCode()); d.setLabelEn(e.getLabelEn()); d.setLabelAr(e.getLabelAr());
        d.setFactorWeight(e.getFactorWeight()); d.setRiskStatus(e.getRiskStatus());
        d.setLovVersion(e.getLovVersion()); d.setActive(e.isActive()); d.setSortOrder(e.getSortOrder());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static LovEntryJpaEntity toEntity(LovEntry d) {
        var e = new LovEntryJpaEntity();
        e.setId(d.getId()); e.setLovSetId(d.getLovSetId()); e.setTenantId(d.getTenantId());
        e.setFactorCode(d.getFactorCode()); e.setLabelEn(d.getLabelEn()); e.setLabelAr(d.getLabelAr());
        e.setFactorWeight(d.getFactorWeight()); e.setRiskStatus(d.getRiskStatus());
        e.setLovVersion(d.getLovVersion()); e.setActive(d.isActive()); e.setSortOrder(d.getSortOrder());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== RiskParameter =====
    public static RiskParameter toDomain(RiskParameterJpaEntity e) {
        var d = new RiskParameter();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setRiskType(e.getRiskType() != null ? RiskType.valueOf(e.getRiskType().name()) : null);
        d.setFlow(e.getFlow()); d.setCategory(e.getCategory()); d.setSubCategory(e.getSubCategory());
        d.setQuestionEn(e.getQuestionEn()); d.setQuestionAr(e.getQuestionAr());
        d.setInputType(e.getInputType() != null ? ParameterInputType.valueOf(e.getInputType().name()) : null);
        d.setLovSetId(e.getLovSetId()); d.setParentParameterId(e.getParentParameterId());
        d.setParentTriggerValue(e.getParentTriggerValue()); d.setCategoryWeight(e.getCategoryWeight());
        d.setOperator(e.getOperator()); d.setExpectedValue(e.getExpectedValue());
        d.setFlagType(e.getFlagType() != null ? ParameterFlagType.valueOf(e.getFlagType().name()) : null);
        d.setFilledBy(e.getFilledBy() != null ? FilledByType.valueOf(e.getFilledBy().name()) : null);
        d.setActive(e.isActive()); d.setDisplayOrder(e.getDisplayOrder()); d.setLanguage(e.getLanguage());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static RiskParameterJpaEntity toEntity(RiskParameter d) {
        var e = new RiskParameterJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setRiskType(d.getRiskType() != null ? RiskParameterJpaEntity.RiskTypeEnum.valueOf(d.getRiskType().name()) : null);
        e.setFlow(d.getFlow()); e.setCategory(d.getCategory()); e.setSubCategory(d.getSubCategory());
        e.setQuestionEn(d.getQuestionEn()); e.setQuestionAr(d.getQuestionAr());
        e.setInputType(d.getInputType() != null ? RiskParameterJpaEntity.InputTypeEnum.valueOf(d.getInputType().name()) : null);
        e.setLovSetId(d.getLovSetId()); e.setParentParameterId(d.getParentParameterId());
        e.setParentTriggerValue(d.getParentTriggerValue()); e.setCategoryWeight(d.getCategoryWeight());
        e.setOperator(d.getOperator()); e.setExpectedValue(d.getExpectedValue());
        e.setFlagType(d.getFlagType() != null ? RiskParameterJpaEntity.FlagTypeEnum.valueOf(d.getFlagType().name()) : null);
        e.setFilledBy(d.getFilledBy() != null ? RiskParameterJpaEntity.FilledByEnum.valueOf(d.getFilledBy().name()) : null);
        e.setActive(d.isActive()); e.setDisplayOrder(d.getDisplayOrder()); e.setLanguage(d.getLanguage());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== ScoringThreshold =====
    public static ScoringThreshold toDomain(ScoringThresholdJpaEntity e) {
        var d = new ScoringThreshold();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setRiskType(e.getRiskType() != null ? RiskType.valueOf(e.getRiskType().name()) : null);
        d.setRiskLevel(e.getRiskLevel()); d.setMinScore(e.getMinScore()); d.setMaxScore(e.getMaxScore());
        d.setDescriptionEn(e.getDescriptionEn()); d.setDescriptionAr(e.getDescriptionAr());
        d.setActive(e.isActive());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static ScoringThresholdJpaEntity toEntity(ScoringThreshold d) {
        var e = new ScoringThresholdJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setRiskType(d.getRiskType() != null ? RiskParameterJpaEntity.RiskTypeEnum.valueOf(d.getRiskType().name()) : null);
        e.setRiskLevel(d.getRiskLevel()); e.setMinScore(d.getMinScore()); e.setMaxScore(d.getMaxScore());
        e.setDescriptionEn(d.getDescriptionEn()); e.setDescriptionAr(d.getDescriptionAr());
        e.setActive(d.isActive());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== AssessmentSession =====
    public static AssessmentSession toDomain(AssessmentSessionJpaEntity e) {
        var d = new AssessmentSession();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setRiskType(e.getRiskType() != null ? RiskType.valueOf(e.getRiskType().name()) : null);
        d.setEntityReference(e.getEntityReference()); d.setParentSessionId(e.getParentSessionId());
        d.setStatus(e.getStatus() != null ? AssessmentSessionStatus.valueOf(e.getStatus().name()) : null);
        d.setTotalScore(e.getTotalScore()); d.setRiskLevel(e.getRiskLevel());
        d.setPepFlag(e.isPepFlag()); d.setEddFlag(e.isEddFlag()); d.setKycFlag(e.isKycFlag());
        d.setDominantOverride(e.isDominantOverride());
        d.setThirdPartyAmlResult(e.getThirdPartyAmlResult());
        d.setThirdPartySanctionsResult(e.getThirdPartySanctionsResult());
        d.setThirdPartyBlocklistResult(e.getThirdPartyBlocklistResult());
        d.setParameterVersionSnapshot(e.getParameterVersionSnapshot());
        d.setLovVersionSnapshot(e.getLovVersionSnapshot());
        d.setIdempotencyKey(e.getIdempotencyKey());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static AssessmentSessionJpaEntity toEntity(AssessmentSession d) {
        var e = new AssessmentSessionJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setRiskType(d.getRiskType() != null ? RiskParameterJpaEntity.RiskTypeEnum.valueOf(d.getRiskType().name()) : null);
        e.setEntityReference(d.getEntityReference()); e.setParentSessionId(d.getParentSessionId());
        e.setStatus(d.getStatus() != null ? AssessmentSessionJpaEntity.SessionStatusEnum.valueOf(d.getStatus().name()) : null);
        e.setTotalScore(d.getTotalScore()); e.setRiskLevel(d.getRiskLevel());
        e.setPepFlag(d.isPepFlag()); e.setEddFlag(d.isEddFlag()); e.setKycFlag(d.isKycFlag());
        e.setDominantOverride(d.isDominantOverride());
        e.setThirdPartyAmlResult(d.getThirdPartyAmlResult());
        e.setThirdPartySanctionsResult(d.getThirdPartySanctionsResult());
        e.setThirdPartyBlocklistResult(d.getThirdPartyBlocklistResult());
        e.setParameterVersionSnapshot(d.getParameterVersionSnapshot());
        e.setLovVersionSnapshot(d.getLovVersionSnapshot());
        e.setIdempotencyKey(d.getIdempotencyKey());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== AssessmentAnswer =====
    public static AssessmentAnswer toDomain(AssessmentAnswerJpaEntity e) {
        var d = new AssessmentAnswer();
        d.setId(e.getId()); d.setSessionId(e.getSessionId()); d.setTenantId(e.getTenantId());
        d.setParameterId(e.getParameterId()); d.setAnswerVersion(e.getAnswerVersion());
        d.setVersionStatus(e.getVersionStatus() != null ? AnswerVersionStatus.valueOf(e.getVersionStatus().name()) : null);
        d.setAnswerValue(e.getAnswerValue()); d.setAnswerType(e.getAnswerType());
        d.setLanguageCode(e.getLanguageCode()); d.setWeightContribution(e.getWeightContribution());
        d.setRiskScoreAtSubmission(e.getRiskScoreAtSubmission());
        d.setChangeReason(e.getChangeReason() != null ? AnswerChangeReason.valueOf(e.getChangeReason().name()) : null);
        d.setScoreDelta(e.getScoreDelta()); d.setLevelChanged(e.isLevelChanged());
        d.setPreviousRiskLevel(e.getPreviousRiskLevel()); d.setNewRiskLevel(e.getNewRiskLevel());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static AssessmentAnswerJpaEntity toEntity(AssessmentAnswer d) {
        var e = new AssessmentAnswerJpaEntity();
        e.setId(d.getId()); e.setSessionId(d.getSessionId()); e.setTenantId(d.getTenantId());
        e.setParameterId(d.getParameterId()); e.setAnswerVersion(d.getAnswerVersion());
        e.setVersionStatus(d.getVersionStatus() != null ? AssessmentAnswerJpaEntity.VersionStatusEnum.valueOf(d.getVersionStatus().name()) : null);
        e.setAnswerValue(d.getAnswerValue()); e.setAnswerType(d.getAnswerType());
        e.setLanguageCode(d.getLanguageCode()); e.setWeightContribution(d.getWeightContribution());
        e.setRiskScoreAtSubmission(d.getRiskScoreAtSubmission());
        e.setChangeReason(d.getChangeReason() != null ? AssessmentAnswerJpaEntity.ChangeReasonEnum.valueOf(d.getChangeReason().name()) : null);
        e.setScoreDelta(d.getScoreDelta()); e.setLevelChanged(d.isLevelChanged());
        e.setPreviousRiskLevel(d.getPreviousRiskLevel()); e.setNewRiskLevel(d.getNewRiskLevel());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== EntityStatusRecord =====
    public static EntityStatusRecord toDomain(EntityStatusJpaEntity e) {
        var d = new EntityStatusRecord();
        d.setId(e.getId()); d.setTenantId(e.getTenantId()); d.setSessionId(e.getSessionId());
        d.setEntityReference(e.getEntityReference());
        d.setRiskStatus(e.getRiskStatus() != null ? EntityRiskStatus.valueOf(e.getRiskStatus().name()) : null);
        d.setAccountStatus(e.getAccountStatus() != null ? AccountStatus.valueOf(e.getAccountStatus().name()) : null);
        d.setComplianceStatus(e.getComplianceStatus() != null ? ComplianceStatus.valueOf(e.getComplianceStatus().name()) : null);
        d.setStatusReason(e.getStatusReason()); d.setChangedBy(e.getChangedBy());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static EntityStatusJpaEntity toEntity(EntityStatusRecord d) {
        var e = new EntityStatusJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setSessionId(d.getSessionId());
        e.setEntityReference(d.getEntityReference());
        e.setRiskStatus(d.getRiskStatus() != null ? EntityStatusJpaEntity.RiskStatusEnum.valueOf(d.getRiskStatus().name()) : null);
        e.setAccountStatus(d.getAccountStatus() != null ? EntityStatusJpaEntity.AccountStatusEnum.valueOf(d.getAccountStatus().name()) : null);
        e.setComplianceStatus(d.getComplianceStatus() != null ? EntityStatusJpaEntity.ComplianceStatusEnum.valueOf(d.getComplianceStatus().name()) : null);
        e.setStatusReason(d.getStatusReason()); e.setChangedBy(d.getChangedBy());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== ScenarioRule =====
    public static ScenarioRule toDomain(ScenarioRuleJpaEntity e) {
        var d = new ScenarioRule();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setScenarioName(e.getScenarioName()); d.setScenarioNameAr(e.getScenarioNameAr());
        d.setTriggerRiskStatus(e.getTriggerRiskStatus());
        d.setTriggerPepFlag(e.getTriggerPepFlag());
        d.setTriggerThirdPartyCheckType(e.getTriggerThirdPartyCheckType() != null ? ThirdPartyCheckType.valueOf(e.getTriggerThirdPartyCheckType().name()) : null);
        d.setTriggerThirdPartyResult(e.getTriggerThirdPartyResult());
        d.setResultingAccountStatus(e.getResultingAccountStatus() != null ? AccountStatus.valueOf(e.getResultingAccountStatus().name()) : null);
        d.setResultingComplianceStatus(e.getResultingComplianceStatus() != null ? ComplianceStatus.valueOf(e.getResultingComplianceStatus().name()) : null);
        d.setRequiresManualReview(e.isRequiresManualReview()); d.setNotifyRole(e.getNotifyRole());
        d.setPriority(e.getPriority()); d.setSlaDurationHours(e.getSlaDurationHours()); d.setActive(e.isActive());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static ScenarioRuleJpaEntity toEntity(ScenarioRule d) {
        var e = new ScenarioRuleJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setScenarioName(d.getScenarioName()); e.setScenarioNameAr(d.getScenarioNameAr());
        e.setTriggerRiskStatus(d.getTriggerRiskStatus());
        e.setTriggerPepFlag(d.getTriggerPepFlag());
        e.setTriggerThirdPartyCheckType(d.getTriggerThirdPartyCheckType() != null ? ScenarioRuleJpaEntity.ThirdPartyCheckTypeEnum.valueOf(d.getTriggerThirdPartyCheckType().name()) : null);
        e.setTriggerThirdPartyResult(d.getTriggerThirdPartyResult());
        e.setResultingAccountStatus(d.getResultingAccountStatus() != null ? EntityStatusJpaEntity.AccountStatusEnum.valueOf(d.getResultingAccountStatus().name()) : null);
        e.setResultingComplianceStatus(d.getResultingComplianceStatus() != null ? EntityStatusJpaEntity.ComplianceStatusEnum.valueOf(d.getResultingComplianceStatus().name()) : null);
        e.setRequiresManualReview(d.isRequiresManualReview()); e.setNotifyRole(d.getNotifyRole());
        e.setPriority(d.getPriority()); e.setSlaDurationHours(d.getSlaDurationHours()); e.setActive(d.isActive());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== ReviewTask =====
    public static ReviewTask toDomain(ReviewTaskJpaEntity e) {
        var d = new ReviewTask();
        d.setId(e.getId()); d.setTenantId(e.getTenantId()); d.setSessionId(e.getSessionId());
        d.setEntityReference(e.getEntityReference());
        d.setStatus(e.getStatus() != null ? ReviewTaskStatus.valueOf(e.getStatus().name()) : null);
        d.setMakerId(e.getMakerId());
        d.setMakerRecommendation(e.getMakerRecommendation() != null ? ReviewRecommendation.valueOf(e.getMakerRecommendation().name()) : null);
        d.setMakerComment(e.getMakerComment());
        d.setMakerAccountStatus(e.getMakerAccountStatus() != null ? AccountStatus.valueOf(e.getMakerAccountStatus().name()) : null);
        d.setMakerComplianceStatus(e.getMakerComplianceStatus() != null ? ComplianceStatus.valueOf(e.getMakerComplianceStatus().name()) : null);
        d.setMakerActionAt(toInstant(e.getMakerActionAt()));
        d.setApproverId(e.getApproverId());
        d.setApproverAction(e.getApproverAction() != null ? ReviewAction.valueOf(e.getApproverAction().name()) : null);
        d.setApproverComment(e.getApproverComment());
        d.setApproverAccountStatus(e.getApproverAccountStatus() != null ? AccountStatus.valueOf(e.getApproverAccountStatus().name()) : null);
        d.setApproverComplianceStatus(e.getApproverComplianceStatus() != null ? ComplianceStatus.valueOf(e.getApproverComplianceStatus().name()) : null);
        d.setApproverActionAt(toInstant(e.getApproverActionAt()));
        d.setSlaDeadline(toInstant(e.getSlaDeadline())); d.setSlaBreached(e.isSlaBreached());
        d.setCreatedAt(toInstant(e.getCreatedAt())); d.setUpdatedAt(toInstant(e.getUpdatedAt()));
        d.setVersion(e.getVersion());
        return d;
    }
    public static ReviewTaskJpaEntity toEntity(ReviewTask d) {
        var e = new ReviewTaskJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId()); e.setSessionId(d.getSessionId());
        e.setEntityReference(d.getEntityReference());
        e.setStatus(d.getStatus() != null ? ReviewTaskJpaEntity.ReviewStatusEnum.valueOf(d.getStatus().name()) : null);
        e.setMakerId(d.getMakerId());
        e.setMakerRecommendation(d.getMakerRecommendation() != null ? ReviewTaskJpaEntity.RecommendationEnum.valueOf(d.getMakerRecommendation().name()) : null);
        e.setMakerComment(d.getMakerComment());
        e.setMakerAccountStatus(d.getMakerAccountStatus() != null ? EntityStatusJpaEntity.AccountStatusEnum.valueOf(d.getMakerAccountStatus().name()) : null);
        e.setMakerComplianceStatus(d.getMakerComplianceStatus() != null ? EntityStatusJpaEntity.ComplianceStatusEnum.valueOf(d.getMakerComplianceStatus().name()) : null);
        e.setMakerActionAt(toOdt(d.getMakerActionAt()));
        e.setApproverId(d.getApproverId());
        e.setApproverAction(d.getApproverAction() != null ? ReviewTaskJpaEntity.ApproverActionEnum.valueOf(d.getApproverAction().name()) : null);
        e.setApproverComment(d.getApproverComment());
        e.setApproverAccountStatus(d.getApproverAccountStatus() != null ? EntityStatusJpaEntity.AccountStatusEnum.valueOf(d.getApproverAccountStatus().name()) : null);
        e.setApproverComplianceStatus(d.getApproverComplianceStatus() != null ? EntityStatusJpaEntity.ComplianceStatusEnum.valueOf(d.getApproverComplianceStatus().name()) : null);
        e.setApproverActionAt(toOdt(d.getApproverActionAt()));
        e.setSlaDeadline(toOdt(d.getSlaDeadline())); e.setSlaBreached(d.isSlaBreached());
        e.setCreatedAt(toOdt(d.getCreatedAt())); e.setUpdatedAt(toOdt(d.getUpdatedAt()));
        e.setVersion(d.getVersion());
        return e;
    }

    // ===== AuditEntry =====
    public static AuditEntry toDomain(AuditEntryJpaEntity e) {
        var d = new AuditEntry();
        d.setId(e.getId()); d.setTenantId(e.getTenantId());
        d.setEntityType(e.getEntityType() != null ? AuditEntityType.valueOf(e.getEntityType().name()) : null);
        d.setEntityId(e.getEntityId()); d.setAction(e.getAction());
        d.setBeforeState(e.getBeforeState()); d.setAfterState(e.getAfterState());
        d.setActorId(e.getActorId()); d.setActorRole(e.getActorRole());
        d.setIpAddress(e.getIpAddress()); d.setCorrelationId(e.getCorrelationId());
        d.setCreatedAt(toInstant(e.getCreatedAt()));
        return d;
    }
    public static AuditEntryJpaEntity toEntity(AuditEntry d) {
        var e = new AuditEntryJpaEntity();
        e.setId(d.getId()); e.setTenantId(d.getTenantId());
        e.setEntityType(d.getEntityType() != null ? AuditEntryJpaEntity.AuditEntityTypeEnum.valueOf(d.getEntityType().name()) : null);
        e.setEntityId(d.getEntityId()); e.setAction(d.getAction());
        e.setBeforeState(d.getBeforeState()); e.setAfterState(d.getAfterState());
        e.setActorId(d.getActorId()); e.setActorRole(d.getActorRole());
        e.setIpAddress(d.getIpAddress()); e.setCorrelationId(d.getCorrelationId());
        e.setCreatedAt(toOdt(d.getCreatedAt()));
        return e;
    }
}
