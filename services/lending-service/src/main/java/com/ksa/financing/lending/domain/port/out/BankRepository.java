package com.ksa.financing.lending.domain.port.out;

import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.lending.domain.model.Bank;

import java.util.UUID;

public interface BankRepository {

    PageResponse<Bank> findAllActive(UUID tenantId, PageQuery query);
}
