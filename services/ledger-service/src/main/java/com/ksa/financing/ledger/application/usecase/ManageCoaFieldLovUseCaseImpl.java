package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.port.in.ManageCoaFieldLovUseCase;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageCoaFieldLovUseCaseImpl implements ManageCoaFieldLovUseCase {

    private final CoaFieldLovRepository repository;

    @Override
    @Transactional
    public CoaFieldLov create(CreateCoaFieldLovCommand command) {
        if (repository.existsByFieldKey(command.tenantId(), command.fieldKey())) {
            throw new BusinessException(
                    ErrorCodes.CONFLICT,
                    "COA field key already exists: " + command.fieldKey());
        }

        var lov = CoaFieldLov.create(
                command.tenantId(),
                command.fieldKey(),
                command.fieldLabelEn(),
                command.fieldLabelAr(),
                command.category(),
                command.mandatoryDefault(),
                command.displayOrder()
        );
        var saved = repository.save(lov);
        log.info("COA field LOV created: tenant={} key={}", command.tenantId(), saved.getFieldKey());
        return saved;
    }

    @Override
    @Transactional
    public CoaFieldLov update(UUID tenantId, UUID fieldLovId, UpdateCoaFieldLovCommand command) {
        var lov = repository.findById(tenantId, fieldLovId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaFieldLov", fieldLovId.toString()));

        lov.update(
                command.fieldLabelEn(),
                command.fieldLabelAr(),
                command.category(),
                command.mandatoryDefault(),
                command.displayOrder()
        );
        return repository.save(lov);
    }

    @Override
    @Transactional(readOnly = true)
    public CoaFieldLov getById(UUID tenantId, UUID fieldLovId) {
        return repository.findById(tenantId, fieldLovId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaFieldLov", fieldLovId.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CoaFieldLov> list(UUID tenantId, boolean activeOnly, PageQuery pageQuery) {
        return activeOnly ? repository.findAllActiveByTenant(tenantId, pageQuery) : repository.findAllByTenant(tenantId, pageQuery);
    }

    @Override
    @Transactional
    public CoaFieldLov deactivate(UUID tenantId, UUID fieldLovId) {
        var lov = repository.findById(tenantId, fieldLovId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaFieldLov", fieldLovId.toString()));
        lov.deactivate();
        var saved = repository.save(lov);
        log.info("COA field LOV deactivated: tenant={} id={} key={}", tenantId, fieldLovId, saved.getFieldKey());
        return saved;
    }

    @Override
    @Transactional
    public CoaFieldLov activate(UUID tenantId, UUID fieldLovId) {
        var lov = repository.findById(tenantId, fieldLovId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaFieldLov", fieldLovId.toString()));
        lov.activate();
        var saved = repository.save(lov);
        log.info("COA field LOV activated: tenant={} id={} key={}", tenantId, fieldLovId, saved.getFieldKey());
        return saved;
    }
}
