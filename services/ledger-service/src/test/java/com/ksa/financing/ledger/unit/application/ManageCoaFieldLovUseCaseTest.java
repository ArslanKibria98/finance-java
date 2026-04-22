package com.ksa.financing.ledger.unit.application;

import com.ksa.financing.infra.exception.BusinessException;
import com.ksa.financing.ledger.application.usecase.ManageCoaFieldLovUseCaseImpl;
import com.ksa.financing.ledger.domain.model.CoaFieldLov;
import com.ksa.financing.ledger.domain.port.in.ManageCoaFieldLovUseCase;
import com.ksa.financing.ledger.domain.port.out.CoaFieldLovRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageCoaFieldLovUseCase Tests")
class ManageCoaFieldLovUseCaseTest {

    @Mock
    private CoaFieldLovRepository repository;

    @InjectMocks
    private ManageCoaFieldLovUseCaseImpl useCase;

    @Test
    @DisplayName("Should create a COA field LOV")
    void shouldCreate() {
        var tenantId = UUID.randomUUID();
        var command = new ManageCoaFieldLovUseCase.CreateCoaFieldLovCommand(
                tenantId,
                "COLLECTION_ACCOUNT",
                "Collection Account",
                "حساب التحصيل",
                "COLLECTIONS",
                true,
                1
        );

        when(repository.existsByFieldKey(tenantId, "COLLECTION_ACCOUNT")).thenReturn(false);
        when(repository.save(org.mockito.ArgumentMatchers.any(CoaFieldLov.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var created = useCase.create(command);

        assertThat(created.getTenantId()).isEqualTo(tenantId);
        assertThat(created.getFieldKey()).isEqualTo("COLLECTION_ACCOUNT");
        assertThat(created.getFieldLabelEn()).isEqualTo("Collection Account");
    }

    @Test
    @DisplayName("Should reject duplicate field key")
    void shouldRejectDuplicate() {
        var tenantId = UUID.randomUUID();
        var command = new ManageCoaFieldLovUseCase.CreateCoaFieldLovCommand(
                tenantId,
                "COLLECTION_ACCOUNT",
                "Collection Account",
                null,
                "COLLECTIONS",
                true,
                1
        );

        when(repository.existsByFieldKey(tenantId, "COLLECTION_ACCOUNT")).thenReturn(true);

        assertThatThrownBy(() -> useCase.create(command))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should update existing field")
    void shouldUpdate() {
        var tenantId = UUID.randomUUID();
        var id = UUID.randomUUID();
        var existing = CoaFieldLov.create(
                tenantId,
                "COLLECTION_ACCOUNT",
                "Collection Account",
                null,
                "COLLECTIONS",
                true,
                1
        );

        when(repository.findById(tenantId, id)).thenReturn(Optional.of(existing));
        when(repository.save(org.mockito.ArgumentMatchers.any(CoaFieldLov.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var updated = useCase.update(tenantId, id, new ManageCoaFieldLovUseCase.UpdateCoaFieldLovCommand(
                "Collection Account Updated",
                "محدث",
                "COLLECTIONS",
                false,
                2
        ));

        assertThat(updated.getFieldLabelEn()).isEqualTo("Collection Account Updated");
        assertThat(updated.isMandatoryDefault()).isFalse();
        assertThat(updated.getDisplayOrder()).isEqualTo(2);
    }
}
