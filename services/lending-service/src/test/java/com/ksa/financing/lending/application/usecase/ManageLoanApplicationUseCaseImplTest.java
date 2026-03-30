package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageLoanApplicationUseCaseImpl")
class ManageLoanApplicationUseCaseImplTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    private ManageLoanApplicationUseCaseImpl useCase;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ManageLoanApplicationUseCaseImpl(applicationRepository);
    }

    @Test
    @DisplayName("should get application by ID")
    void shouldGetApplication() {
        var appId = UUID.randomUUID();
        var aggregate = LoanApplicationAggregate.create(
                TENANT_ID, "APP-00000001", CUSTOMER_ID, "1234567890",
                new BigDecimal("5000"), new BigDecimal("3000"), new BigDecimal("1000"),
                2, 1,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                UUID.randomUUID()
        );

        when(applicationRepository.findById(eq(TENANT_ID), any(LoanApplicationId.class)))
                .thenReturn(Optional.of(aggregate));

        var result = useCase.getApplication(TENANT_ID, appId);

        assertThat(result).isNotNull();
        assertThat(result.getApplicationNumber()).isEqualTo("APP-00000001");
        verify(applicationRepository).findById(eq(TENANT_ID), any(LoanApplicationId.class));
    }

    @Test
    @DisplayName("should throw NotFoundException for missing application")
    void shouldThrowNotFound() {
        var appId = UUID.randomUUID();
        when(applicationRepository.findById(eq(TENANT_ID), any(LoanApplicationId.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.getApplication(TENANT_ID, appId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("should list all applications for tenant")
    void shouldListApplications() {
        when(applicationRepository.findAllByTenant(TENANT_ID))
                .thenReturn(List.of());

        var result = useCase.listApplications(TENANT_ID);

        assertThat(result).isEmpty();
        verify(applicationRepository).findAllByTenant(TENANT_ID);
    }

    @Test
    @DisplayName("should list applications by customer")
    void shouldListByCustomer() {
        when(applicationRepository.findByCustomer(TENANT_ID, CUSTOMER_ID))
                .thenReturn(List.of());

        var result = useCase.listApplicationsByCustomer(TENANT_ID, CUSTOMER_ID);

        assertThat(result).isEmpty();
        verify(applicationRepository).findByCustomer(TENANT_ID, CUSTOMER_ID);
    }
}
