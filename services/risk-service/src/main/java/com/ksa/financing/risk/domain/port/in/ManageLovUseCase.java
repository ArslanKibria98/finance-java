package com.ksa.financing.risk.domain.port.in;

import com.ksa.financing.risk.domain.model.lov.LovEntry;
import com.ksa.financing.risk.domain.model.lov.LovCategoryType;
import com.ksa.financing.risk.domain.model.lov.LovSet;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ManageLovUseCase {

    // LOV Set operations
    LovSet createLovSet(UUID tenantId, CreateLovSetCommand command);

    LovSet updateLovSet(UUID tenantId, UUID lovSetId, UpdateLovSetCommand command);

    LovSet getLovSetById(UUID tenantId, UUID lovSetId);

    List<LovSet> getAllLovSets(UUID tenantId);

    List<LovSet> getActiveLovSets(UUID tenantId);

    void deactivateLovSet(UUID tenantId, UUID lovSetId);

    // LOV Entry operations
    LovEntry createLovEntry(UUID tenantId, UUID lovSetId, CreateLovEntryCommand command);

    LovEntry updateLovEntry(UUID tenantId, UUID entryId, UpdateLovEntryCommand command);

    List<LovEntry> getEntriesByLovSet(UUID tenantId, UUID lovSetId);

    List<LovEntry> getActiveEntriesByLovSet(UUID tenantId, UUID lovSetId);

    void deactivateLovEntry(UUID tenantId, UUID entryId);

    record CreateLovSetCommand(
            String code,
            String nameEn,
            String nameAr,
            LovCategoryType categoryType
    ) {}

    record UpdateLovSetCommand(
            String nameEn,
            String nameAr,
            LovCategoryType categoryType
    ) {}

    record CreateLovEntryCommand(
            String factorCode,
            String labelEn,
            String labelAr,
            BigDecimal factorWeight,
            String riskStatus,
            int sortOrder
    ) {}

    record UpdateLovEntryCommand(
            String labelEn,
            String labelAr,
            BigDecimal factorWeight,
            String riskStatus,
            Integer sortOrder
    ) {}
}
