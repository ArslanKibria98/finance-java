package com.ksa.financing.risk.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.model.lov.LovSet;
import com.ksa.financing.risk.domain.port.in.ManageLovUseCase;
import com.ksa.financing.risk.domain.port.out.LovEntryRepository;
import com.ksa.financing.risk.domain.port.out.LovSetRepository;
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
public class ManageLovService implements ManageLovUseCase {

    private final LovSetRepository lovSetRepository;
    private final LovEntryRepository lovEntryRepository;

    @Override
    @Transactional
    public LovSet createLovSet(UUID tenantId, CreateLovSetCommand command) {
        if (lovSetRepository.existsByCode(tenantId, command.code())) {
            throw new BusinessException("RISK.LOV.DUPLICATE_CODE",
                    "LOV set with code already exists: " + command.code());
        }

        var lovSet = new LovSet();
        lovSet.setId(UUID.randomUUID());
        lovSet.setTenantId(tenantId);
        lovSet.setCode(command.code());
        lovSet.setNameEn(command.nameEn());
        lovSet.setNameAr(command.nameAr());
        lovSet.setCategoryType(command.categoryType());
        lovSet.setCurrentVersion(1);
        lovSet.setActive(true);
        lovSet.setCreatedAt(Instant.now());
        lovSet.setUpdatedAt(Instant.now());
        lovSet.setVersion(1);

        var saved = lovSetRepository.save(lovSet);
        log.info("LOV set created: {} for tenant: {}", command.code(), tenantId);
        return saved;
    }

    @Override
    @Transactional
    public LovSet updateLovSet(UUID tenantId, UUID lovSetId, UpdateLovSetCommand command) {
        var lovSet = lovSetRepository.findById(tenantId, lovSetId)
                .orElseThrow(() -> NotFoundException.forEntity("LovSet", lovSetId.toString()));

        if (command.nameEn() != null) lovSet.setNameEn(command.nameEn());
        if (command.nameAr() != null) lovSet.setNameAr(command.nameAr());
        if (command.categoryType() != null) lovSet.setCategoryType(command.categoryType());
        lovSet.setUpdatedAt(Instant.now());

        var saved = lovSetRepository.save(lovSet);
        log.info("LOV set updated: {}", lovSetId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public LovSet getLovSetById(UUID tenantId, UUID lovSetId) {
        return lovSetRepository.findById(tenantId, lovSetId)
                .orElseThrow(() -> NotFoundException.forEntity("LovSet", lovSetId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovSet> getAllLovSets(UUID tenantId) {
        return lovSetRepository.findAllByTenantId(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovSet> getActiveLovSets(UUID tenantId) {
        return lovSetRepository.findActiveByTenantId(tenantId);
    }

    @Override
    @Transactional
    public void deactivateLovSet(UUID tenantId, UUID lovSetId) {
        var lovSet = lovSetRepository.findById(tenantId, lovSetId)
                .orElseThrow(() -> NotFoundException.forEntity("LovSet", lovSetId.toString()));
        lovSet.setActive(false);
        lovSet.setUpdatedAt(Instant.now());
        lovSetRepository.save(lovSet);
        log.info("LOV set deactivated: {}", lovSetId);
    }

    @Override
    @Transactional
    public LovEntry createLovEntry(UUID tenantId, UUID lovSetId, CreateLovEntryCommand command) {
        lovSetRepository.findById(tenantId, lovSetId)
                .orElseThrow(() -> NotFoundException.forEntity("LovSet", lovSetId.toString()));

        if (lovEntryRepository.existsByFactorCode(tenantId, lovSetId, command.factorCode())) {
            throw new BusinessException("RISK.LOV.DUPLICATE_FACTOR_CODE",
                    "Factor code already exists in LOV set: " + command.factorCode());
        }

        var entry = new LovEntry();
        entry.setId(UUID.randomUUID());
        entry.setLovSetId(lovSetId);
        entry.setTenantId(tenantId);
        entry.setFactorCode(command.factorCode());
        entry.setLabelEn(command.labelEn());
        entry.setLabelAr(command.labelAr());
        entry.setFactorWeight(command.factorWeight());
        entry.setRiskStatus(command.riskStatus());
        entry.setLovVersion(1);
        entry.setActive(true);
        entry.setSortOrder(command.sortOrder());
        entry.setCreatedAt(Instant.now());
        entry.setUpdatedAt(Instant.now());
        entry.setVersion(1);

        var saved = lovEntryRepository.save(entry);
        log.info("LOV entry created: {} in set: {}", command.factorCode(), lovSetId);
        return saved;
    }

    @Override
    @Transactional
    public LovEntry updateLovEntry(UUID tenantId, UUID entryId, UpdateLovEntryCommand command) {
        var entry = lovEntryRepository.findById(tenantId, entryId)
                .orElseThrow(() -> NotFoundException.forEntity("LovEntry", entryId.toString()));

        if (command.labelEn() != null) entry.setLabelEn(command.labelEn());
        if (command.labelAr() != null) entry.setLabelAr(command.labelAr());
        if (command.factorWeight() != null) entry.setFactorWeight(command.factorWeight());
        if (command.riskStatus() != null) entry.setRiskStatus(command.riskStatus());
        if (command.sortOrder() != null) entry.setSortOrder(command.sortOrder());
        entry.setUpdatedAt(Instant.now());

        var saved = lovEntryRepository.save(entry);
        log.info("LOV entry updated: {}", entryId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovEntry> getEntriesByLovSet(UUID tenantId, UUID lovSetId) {
        return lovEntryRepository.findByLovSetId(tenantId, lovSetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LovEntry> getActiveEntriesByLovSet(UUID tenantId, UUID lovSetId) {
        return lovEntryRepository.findActiveByLovSetId(tenantId, lovSetId);
    }

    @Override
    @Transactional
    public void deactivateLovEntry(UUID tenantId, UUID entryId) {
        var entry = lovEntryRepository.findById(tenantId, entryId)
                .orElseThrow(() -> NotFoundException.forEntity("LovEntry", entryId.toString()));
        entry.setActive(false);
        entry.setUpdatedAt(Instant.now());
        lovEntryRepository.save(entry);
        log.info("LOV entry deactivated: {}", entryId);
    }
}
