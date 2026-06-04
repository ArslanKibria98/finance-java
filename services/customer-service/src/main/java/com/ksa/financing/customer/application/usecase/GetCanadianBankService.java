package com.ksa.financing.customer.application.usecase;

import com.ksa.financing.customer.domain.model.CanadianBankOption;
import com.ksa.financing.customer.domain.port.in.GetCanadianBankUseCase;
import com.ksa.financing.customer.domain.port.out.CanadianBankRepository;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class GetCanadianBankService implements GetCanadianBankUseCase {

    private final CanadianBankRepository repository;

    public GetCanadianBankService(CanadianBankRepository repository) {
        this.repository = repository;
    }

    @Override
    public CanadianBankOption getById(UUID tenantId, UUID id) {
        return repository.findById(tenantId, id)
                .orElseThrow(() -> NotFoundException.forEntity("Canadian bank option", id.toString()));
    }

    @Override
    public PageResponse<CanadianBankOption> getAll(UUID tenantId, PageQuery pageQuery) {
        return repository.findAllByTenantId(tenantId, pageQuery);
    }

    @Override
    public PageResponse<CanadianBankOption> getActive(UUID tenantId, PageQuery pageQuery) {
        return repository.findActiveByTenantId(tenantId, pageQuery);
    }
}
