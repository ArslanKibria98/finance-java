package com.ksa.financing.ledger.application.usecase;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.exception.ErrorCodes;
import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.ledger.domain.model.CoaConfigurationMapping;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;
import com.ksa.financing.ledger.domain.model.CoaConfigurationStatus;
import com.ksa.financing.ledger.domain.port.in.ManageCoaConfigurationUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.CoaConfigurationRepository;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageCoaConfigurationUseCaseImpl implements ManageCoaConfigurationUseCase {

    private final CoaConfigurationRepository configurationRepository;
    private final CoaFieldLovRepository fieldLovRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public CoaConfigurationProfile createProfile(CreateProfileCommand command) {
        var profile = CoaConfigurationProfile.create(command.tenantId(), command.productCode(), command.profileName());
        profile.updateProfile(command.profileName(), command.effectiveFrom(), command.effectiveTo());
        return configurationRepository.saveProfile(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public CoaConfigurationProfile getProfile(UUID tenantId, UUID profileId) {
        return configurationRepository.findProfileById(tenantId, profileId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaConfigurationProfile", profileId.toString()));
    }

    @Override
    @Transactional
    public CoaConfigurationProfile getOrCreateProfileByProduct(UUID tenantId, UUID productId) {
        var productCode = productId.toString().toUpperCase();
        var existing = configurationRepository.findProfilesByProductCode(tenantId, productCode);
        if (!existing.isEmpty()) {
            return existing.getFirst();
        }

        var profile = CoaConfigurationProfile.create(
                tenantId,
                productCode,
                "AUTO_PROFILE_" + productCode
        );
        return configurationRepository.saveProfile(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoaConfigurationProfile> listProfilesByProduct(UUID tenantId, String productCode) {
        return configurationRepository.findProfilesByProductCode(tenantId, productCode);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoaConfigurationMapping> getMappings(UUID tenantId, UUID profileId) {
        ensureProfile(tenantId, profileId);
        return configurationRepository.findMappingsByProfileId(tenantId, profileId);
    }

    @Override
    @Transactional
    public List<CoaConfigurationMapping> selectFields(SelectFieldsCommand command) {
        ensureProfile(command.tenantId(), command.profileId());

        var existing = configurationRepository.findMappingsByProfileId(command.tenantId(), command.profileId());
        var existingByFieldId = new HashMap<UUID, CoaConfigurationMapping>();
        for (var mapping : existing) {
            existingByFieldId.put(mapping.coaFieldId(), mapping);
        }

        var selected = new ArrayList<CoaConfigurationMapping>();
        for (var fieldKey : command.fieldKeys()) {
            var field = fieldLovRepository.findByFieldKey(command.tenantId(), fieldKey)
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.BAD_REQUEST,
                            "COA field key not found: " + fieldKey));

            var existingMapping = existingByFieldId.get(field.getId());
            if (existingMapping != null) {
                selected.add(existingMapping);
            } else {
                selected.add(CoaConfigurationMapping.create(
                        command.tenantId(),
                        command.profileId(),
                        field.getId(),
                        null,
                        field.isMandatoryDefault(),
                        "Selected for product configuration"
                ));
            }
        }

        configurationRepository.replaceMappings(command.tenantId(), command.profileId(), selected);
        return configurationRepository.findMappingsByProfileId(command.tenantId(), command.profileId());
    }

    @Override
    @Transactional
    public List<CoaConfigurationMapping> assignAccounts(AssignAccountsCommand command) {
        ensureProfile(command.tenantId(), command.profileId());
        var currentMappings = configurationRepository.findMappingsByProfileId(command.tenantId(), command.profileId());
        var byFieldId = new HashMap<UUID, CoaConfigurationMapping>();
        for (var mapping : currentMappings) {
            byFieldId.put(mapping.coaFieldId(), mapping);
        }

        var updated = new ArrayList<>(currentMappings);
        for (var assignment : command.assignments()) {
            var field = fieldLovRepository.findByFieldKey(command.tenantId(), assignment.fieldKey())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.BAD_REQUEST,
                            "COA field key not found: " + assignment.fieldKey()));
            var selectedMapping = byFieldId.get(field.getId());
            if (selectedMapping == null) {
                throw new BusinessException(
                        ErrorCodes.BAD_REQUEST,
                        "COA field is not selected for this product: " + assignment.fieldKey());
            }
            var account = accountRepository.findByCode(command.tenantId(), assignment.accountCode())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.BAD_REQUEST,
                            "Account code not found: " + assignment.accountCode()));

            var newMapping = selectedMapping.withAccountId(account.getId().value());
            updated.remove(selectedMapping);
            updated.add(newMapping);
            byFieldId.put(field.getId(), newMapping);
        }

        configurationRepository.replaceMappings(command.tenantId(), command.profileId(), updated);
        return configurationRepository.findMappingsByProfileId(command.tenantId(), command.profileId());
    }

    @Override
    @Transactional
    public List<CoaConfigurationMapping> upsertMappings(UpsertMappingsCommand command) {
        ensureProfile(command.tenantId(), command.profileId());

        var mappings = new ArrayList<CoaConfigurationMapping>();
        for (var item : command.mappings()) {
            var field = fieldLovRepository.findByFieldKey(command.tenantId(), item.fieldKey())
                    .orElseThrow(() -> new BusinessException(
                            ErrorCodes.BAD_REQUEST,
                            "COA field key not found: " + item.fieldKey()));

            UUID accountId = null;
            if (item.accountCode() != null && !item.accountCode().isBlank()) {
                var account = accountRepository.findByCode(command.tenantId(), item.accountCode())
                        .orElseThrow(() -> new BusinessException(
                                ErrorCodes.BAD_REQUEST,
                                "Account code not found: " + item.accountCode()));
                accountId = account.getId().value();
            }

            mappings.add(CoaConfigurationMapping.create(
                    command.tenantId(),
                    command.profileId(),
                    field.getId(),
                    accountId,
                    item.mandatoryOverride(),
                    item.notes()
            ));
        }

        configurationRepository.replaceMappings(command.tenantId(), command.profileId(), mappings);
        return configurationRepository.findMappingsByProfileId(command.tenantId(), command.profileId());
    }

    @Override
    @Transactional(readOnly = true)
    public ValidationResult validate(UUID tenantId, UUID profileId) {
        var profile = ensureProfile(tenantId, profileId);
        var mappings = configurationRepository.findMappingsByProfileId(tenantId, profileId);
        var activeFields = fieldLovRepository.findAllActiveByTenant(tenantId);
        var allFields = fieldLovRepository.findAllByTenant(tenantId);
        var fieldsById = allFields.stream().collect(java.util.stream.Collectors.toMap(f -> f.getId(), f -> f));

        var mappedFieldIds = mappings.stream().map(CoaConfigurationMapping::coaFieldId).collect(java.util.stream.Collectors.toSet());
        var missingMandatorySelection = activeFields.stream()
                .filter(f -> f.isMandatoryDefault() && !mappedFieldIds.contains(f.getId()))
                .map(f -> f.getFieldKey())
                .toList();

        var missingAssignedAccount = mappings.stream()
                .filter(m -> m.accountId() == null)
                .map(m -> fieldsById.get(m.coaFieldId()))
                .filter(java.util.Objects::nonNull)
                .map(f -> f.getFieldKey())
                .toList();

        var missingMandatory = new ArrayList<String>();
        missingMandatory.addAll(missingMandatorySelection);
        missingMandatory.addAll(missingAssignedAccount);

        var invalidAccounts = mappings.stream()
                .filter(m -> m.accountId() != null)
                .filter(m -> accountRepository.findById(tenantId, com.ksa.financing.ledger.domain.model.AccountId.of(m.accountId())).isEmpty())
                .map(CoaConfigurationMapping::accountId)
                .map(UUID::toString)
                .toList();

        log.info("Validated COA profile: profileId={} product={} missingMandatory={} invalidAccounts={}",
                profile.getId(), profile.getProductCode(), missingMandatory.size(), invalidAccounts.size());

        return new ValidationResult(missingMandatory.isEmpty() && invalidAccounts.isEmpty(), missingMandatory, invalidAccounts);
    }

    @Override
    @Transactional
    public CoaConfigurationProfile activate(UUID tenantId, UUID profileId) {
        var profile = ensureProfile(tenantId, profileId);
        var validation = validate(tenantId, profileId);
        if (!validation.valid()) {
            throw new BusinessException(
                    ErrorCodes.BAD_REQUEST,
                    "Cannot activate profile with invalid mappings");
        }
        profile.activate();
        return configurationRepository.saveProfile(profile);
    }

    @Override
    @Transactional
    public CoaConfigurationProfile deactivate(UUID tenantId, UUID profileId) {
        var profile = ensureProfile(tenantId, profileId);
        if (profile.getStatus() == CoaConfigurationStatus.INACTIVE) {
            return profile;
        }
        profile.deactivate();
        return configurationRepository.saveProfile(profile);
    }

    private CoaConfigurationProfile ensureProfile(UUID tenantId, UUID profileId) {
        return configurationRepository.findProfileById(tenantId, profileId)
                .orElseThrow(() -> NotFoundException.forEntity("CoaConfigurationProfile", profileId.toString()));
    }
}
