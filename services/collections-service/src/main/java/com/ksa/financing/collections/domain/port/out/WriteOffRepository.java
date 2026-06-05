package com.ksa.financing.collections.domain.port.out;

import com.ksa.financing.collections.domain.model.WriteOffId;
import com.ksa.financing.collections.domain.model.WriteOffRecord;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WriteOffRepository {

    WriteOffRecord save(WriteOffRecord record);

    List<WriteOffRecord> saveAll(List<WriteOffRecord> records);

    Optional<WriteOffRecord> findById(UUID tenantId, WriteOffId id);

    List<WriteOffRecord> findByLoanId(UUID tenantId, UUID loanId);

    List<WriteOffRecord> findByInstallmentId(UUID tenantId, UUID installmentId);

    List<WriteOffRecord> findByDateRange(UUID tenantId, LocalDate fromDate, LocalDate toDate);

    PageResponse<WriteOffRecord> findAllByTenant(UUID tenantId, PageQuery query);
}
