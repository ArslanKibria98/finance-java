package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.parameter.RiskParameter;
import com.ksa.financing.risk.domain.model.parameter.RiskType;
import com.ksa.financing.risk.domain.port.in.ManageRiskParameterUseCase;
import com.ksa.financing.risk.domain.port.out.RiskParameterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManageRiskParameterService implements ManageRiskParameterUseCase {

    private final RiskParameterRepository riskParameterRepository;

    @Override
    @Transactional
    public RiskParameter create(UUID tenantId, CreateRiskParameterCommand command) {
        var param = new RiskParameter();
        param.setId(UUID.randomUUID());
        param.setTenantId(tenantId);
        param.setRiskType(command.riskType());
        param.setFlow(command.flow());
        param.setCategory(command.category());
        param.setSubCategory(command.subCategory());
        param.setQuestionEn(command.questionEn());
        param.setQuestionAr(command.questionAr());
        param.setInputType(command.inputType());
        param.setLovSetId(command.lovSetId());
        param.setParentParameterId(command.parentParameterId());
        param.setParentTriggerValue(command.parentTriggerValue());
        param.setCategoryWeight(command.categoryWeight());
        param.setOperator(command.operator());
        param.setExpectedValue(command.expectedValue());
        param.setFlagType(command.flagType());
        param.setFilledBy(command.filledBy());
        param.setActive(true);
        param.setDisplayOrder(command.displayOrder());
        param.setLanguage(command.language());
        param.setCreatedAt(Instant.now());
        param.setUpdatedAt(Instant.now());
        param.setVersion(1);

        var saved = riskParameterRepository.save(param);
        log.info("Risk parameter created: {} for tenant: {}", saved.getId(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public RiskParameter update(UUID tenantId, UUID parameterId, UpdateRiskParameterCommand command) {
        var param = riskParameterRepository.findById(tenantId, parameterId)
                .orElseThrow(() -> NotFoundException.forEntity("RiskParameter", parameterId.toString()));

        if (command.questionEn() != null) param.setQuestionEn(command.questionEn());
        if (command.questionAr() != null) param.setQuestionAr(command.questionAr());
        if (command.inputType() != null) param.setInputType(command.inputType());
        if (command.lovSetId() != null) param.setLovSetId(command.lovSetId());
        if (command.parentParameterId() != null) param.setParentParameterId(command.parentParameterId());
        if (command.parentTriggerValue() != null) param.setParentTriggerValue(command.parentTriggerValue());
        if (command.categoryWeight() != null) param.setCategoryWeight(command.categoryWeight());
        if (command.operator() != null) param.setOperator(command.operator());
        if (command.expectedValue() != null) param.setExpectedValue(command.expectedValue());
        if (command.flagType() != null) param.setFlagType(command.flagType());
        if (command.filledBy() != null) param.setFilledBy(command.filledBy());
        if (command.displayOrder() != null) param.setDisplayOrder(command.displayOrder());
        if (command.language() != null) param.setLanguage(command.language());
        param.setUpdatedAt(Instant.now());

        var saved = riskParameterRepository.save(param);
        log.info("Risk parameter updated: {}", parameterId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public RiskParameter getById(UUID tenantId, UUID parameterId) {
        return riskParameterRepository.findById(tenantId, parameterId)
                .orElseThrow(() -> NotFoundException.forEntity("RiskParameter", parameterId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskParameter> getByRiskType(UUID tenantId, RiskType riskType) {
        return riskParameterRepository.findByRiskType(tenantId, riskType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskParameter> getActiveByRiskType(UUID tenantId, RiskType riskType) {
        return riskParameterRepository.findActiveByRiskType(tenantId, riskType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RiskParameter> getByCategory(UUID tenantId, RiskType riskType, String category) {
        return riskParameterRepository.findByCategory(tenantId, riskType, category);
    }

    @Override
    @Transactional
    public void deactivate(UUID tenantId, UUID parameterId) {
        var param = riskParameterRepository.findById(tenantId, parameterId)
                .orElseThrow(() -> NotFoundException.forEntity("RiskParameter", parameterId.toString()));
        param.setActive(false);
        param.setUpdatedAt(Instant.now());
        riskParameterRepository.save(param);
        log.info("Risk parameter deactivated: {}", parameterId);
    }
}
