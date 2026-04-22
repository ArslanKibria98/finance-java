package com.ksa.financing.ledger.domain.port.in;

import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ManageCoaConfigurationUseCase {

    CoaConfigurationProfile createProfile(CreateProfileCommand command);

    CoaConfigurationProfile getProfile(UUID tenantId, UUID profileId);

    CoaConfigurationProfile getOrCreateProfileByProduct(UUID tenantId, UUID productId);

    List<CoaConfigurationProfile> listProfilesByProduct(UUID tenantId, String productCode);

    List<CoaConfigurationMapping> getMappings(UUID tenantId, UUID profileId);

    List<CoaConfigurationMapping> selectFields(SelectFieldsCommand command);

    List<CoaConfigurationMapping> assignAccounts(AssignAccountsCommand command);

    List<CoaConfigurationMapping> upsertMappings(UpsertMappingsCommand command);

    ValidationResult validate(UUID tenantId, UUID profileId);

    CoaConfigurationProfile activate(UUID tenantId, UUID profileId);

    CoaConfigurationProfile deactivate(UUID tenantId, UUID profileId);

    record CreateProfileCommand(
            UUID tenantId,
            String productCode,
            String profileName,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {}

    record UpsertMappingsCommand(
            UUID tenantId,
            UUID profileId,
            List<MappingItem> mappings
    ) {}

    record SelectFieldsCommand(
            UUID tenantId,
            UUID profileId,
            List<String> fieldKeys
    ) {}

    record AssignAccountsCommand(
            UUID tenantId,
            UUID profileId,
            List<AssignmentItem> assignments
    ) {}

    record MappingItem(
            String fieldKey,
            String accountCode,
            Boolean mandatoryOverride,
            String notes
    ) {}

    record AssignmentItem(
            String fieldKey,
            String accountCode
    ) {}

    record ValidationResult(
            boolean valid,
            List<String> missingMandatoryFieldKeys,
            List<String> invalidAccountCodes
    ) {}
}
