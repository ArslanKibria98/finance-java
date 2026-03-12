package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.PurposeOfFinanceEntry;
import com.ksa.financing.lending.domain.port.in.ManagePurposeOfFinanceUseCase;
import com.ksa.financing.lending.domain.port.out.PurposeOfFinanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManagePurposeOfFinanceUseCaseImpl implements ManagePurposeOfFinanceUseCase {

    private final PurposeOfFinanceRepository repository;

    @Override
    @Transactional
    public PurposeOfFinanceEntry create(UUID tenantId, String code, String nameEn, String nameAr,
                                         String descriptionEn, String descriptionAr, int sortOrder, UUID createdBy) {
        repository.findByCode(tenantId, code.toUpperCase()).ifPresent(existing -> {
            throw new BusinessException("LENDING.PURPOSE_OF_FINANCE.DUPLICATE",
                    "Purpose of finance with code '" + code + "' already exists");
        });

        var entry = PurposeOfFinanceEntry.create(tenantId, code, nameEn, nameAr,
                descriptionEn, descriptionAr, sortOrder, createdBy);

        entry = repository.save(entry);
        log.info("Created purpose of finance: {} ({})", entry.getCode(), entry.getId());
        return entry;
    }

    @Override
    @Transactional
    public PurposeOfFinanceEntry update(UUID tenantId, UUID id, String nameEn, String nameAr,
                                         String descriptionEn, String descriptionAr, int sortOrder, boolean active) {
        var entry = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("PurposeOfFinance", id.toString()));

        entry.update(nameEn, nameAr, descriptionEn, descriptionAr, sortOrder, active);
        entry = repository.save(entry);
        log.info("Updated purpose of finance: {} ({})", entry.getCode(), entry.getId());
        return entry;
    }

    @Override
    @Transactional(readOnly = true)
    public PurposeOfFinanceEntry getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("PurposeOfFinance", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurposeOfFinanceEntry> listActive(UUID tenantId) {
        return repository.findAllActive(tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurposeOfFinanceEntry> listAll(UUID tenantId) {
        return repository.findAll(tenantId);
    }

    @Override
    @Transactional
    public void delete(UUID tenantId, UUID id) {
        var entry = repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("PurposeOfFinance", id.toString()));
        repository.delete(tenantId, id);
        log.info("Deleted purpose of finance: {} ({})", entry.getCode(), id);
    }
}
