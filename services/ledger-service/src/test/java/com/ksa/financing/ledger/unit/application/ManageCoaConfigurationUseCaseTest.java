package com.ksa.financing.ledger.unit.application;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.infra.pagination.PageMetadata;
import com.ksa.financing.infra.pagination.PageQuery;
import com.ksa.financing.infra.pagination.PageResponse;
import com.ksa.financing.ledger.application.usecase.ManageCoaConfigurationUseCaseImpl;
import com.ksa.financing.ledger.domain.model.AccountAggregate;
import com.ksa.financing.ledger.domain.model.AccountType;
import com.ksa.financing.ledger.domain.model.CoaConfigurationProfile;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.port.in.ManageCoaConfigurationUseCase;
import com.ksa.financing.ledger.domain.port.out.AccountRepository;
import com.ksa.financing.ledger.domain.port.out.CoaConfigurationRepository;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageCoaConfigurationUseCase Tests")
class ManageCoaConfigurationUseCaseTest {

    @Mock
    private CoaConfigurationRepository configurationRepository;
    @Mock
    private CoaFieldLovRepository fieldLovRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private ManageCoaConfigurationUseCaseImpl useCase;

    @Test
    @DisplayName("Should create profile and save")
    void shouldCreateProfile() {
        var tenantId = UUID.randomUUID();
        var command = new ManageCoaConfigurationUseCase.CreateProfileCommand(
                tenantId,
                "MICRO_FINANCE",
                "Micro Finance Mapping",
                null,
                null
        );
        when(configurationRepository.saveProfile(any(CoaConfigurationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = useCase.createProfile(command);

        assertThat(created.getProductCode()).isEqualTo("MICRO_FINANCE");
        assertThat(created.getProfileName()).isEqualTo("Micro Finance Mapping");
        assertThat(created.getTenantId()).isEqualTo(tenantId);
    }

    @Test
    @DisplayName("Should reject mapping if account code missing")
    void shouldRejectInvalidAccountCode() {
        var tenantId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var field = CoaFieldLov.create(
                tenantId, "COLLECTION_ACCOUNT", "Collection Account", null, "COLLECTIONS", true, 1
        );
        var profile = CoaConfigurationProfile.create(tenantId, "MICRO_FINANCE", "Micro Finance Mapping");

        when(configurationRepository.findProfileById(tenantId, profileId)).thenReturn(Optional.of(profile));
        when(fieldLovRepository.findByFieldKey(tenantId, "COLLECTION_ACCOUNT")).thenReturn(Optional.of(field));
        when(accountRepository.findByCode(tenantId, "110201")).thenReturn(Optional.empty());

        var command = new ManageCoaConfigurationUseCase.UpsertMappingsCommand(
                tenantId,
                profileId,
                List.of(new ManageCoaConfigurationUseCase.MappingItem(
                        "COLLECTION_ACCOUNT", "110201", true, null
                ))
        );

        assertThatThrownBy(() -> useCase.upsertMappings(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Account code not found");
    }

    @Test
    @DisplayName("Should validate profile and return missing mandatory fields")
    void shouldReturnMissingMandatoryFields() {
        var tenantId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var profile = CoaConfigurationProfile.create(tenantId, "MICRO_FINANCE", "Micro Finance Mapping");

        when(configurationRepository.findProfileById(tenantId, profileId)).thenReturn(Optional.of(profile));
        when(configurationRepository.findMappingsByProfileId(tenantId, profileId)).thenReturn(List.of());
        var mandatoryField = CoaFieldLov.create(tenantId, "COLLECTION_ACCOUNT", "Collection Account", null, "COLLECTIONS", true, 1);
        var fieldPage = new PageResponse<>(List.of(mandatoryField), new PageMetadata(0, 1000, 1, 1, true, true, false));
        when(fieldLovRepository.findAllActiveByTenant(eq(tenantId), any(PageQuery.class))).thenReturn(fieldPage);
        when(fieldLovRepository.findAllByTenant(eq(tenantId), any(PageQuery.class))).thenReturn(fieldPage);

        var validation = useCase.validate(tenantId, profileId);

        assertThat(validation.valid()).isFalse();
        assertThat(validation.missingMandatoryFieldKeys()).contains("COLLECTION_ACCOUNT");
    }

    @Test
    @DisplayName("Should activate valid profile")
    void shouldActivateProfile() {
        var tenantId = UUID.randomUUID();
        var profileId = UUID.randomUUID();
        var profile = CoaConfigurationProfile.create(tenantId, "MICRO_FINANCE", "Micro Finance Mapping");
        var field = CoaFieldLov.create(tenantId, "COLLECTION_ACCOUNT", "Collection Account", null, "COLLECTIONS", true, 1);
        var account = AccountAggregate.create(tenantId, "110201", "Collection", AccountType.ASSET, null, false);
        var mapping = com.ksa.financing.ledger.domain.model.CoaConfigurationMapping.create(
                tenantId, profileId, field.getId(), account.getId().value(), true, null
        );

        when(configurationRepository.findProfileById(tenantId, profileId)).thenReturn(Optional.of(profile));
        when(configurationRepository.findMappingsByProfileId(tenantId, profileId)).thenReturn(List.of(mapping));
        var fieldPage = new PageResponse<>(List.of(field), new PageMetadata(0, 1000, 1, 1, true, true, false));
        when(fieldLovRepository.findAllActiveByTenant(eq(tenantId), any(PageQuery.class))).thenReturn(fieldPage);
        when(fieldLovRepository.findAllByTenant(eq(tenantId), any(PageQuery.class))).thenReturn(fieldPage);
        when(accountRepository.findById(tenantId, account.getId())).thenReturn(Optional.of(account));
        when(configurationRepository.saveProfile(any(CoaConfigurationProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var activated = useCase.activate(tenantId, profileId);
        assertThat(activated.getStatus().name()).isEqualTo("ACTIVE");
    }
}
