package com.ksa.financing.lending.application.usecase;

import com.ksa.financing.infra.exception.NotFoundException;
import com.ksa.financing.lending.domain.model.ApplicationStatus;
import com.ksa.financing.lending.domain.model.LoanApplicationAggregate;
import com.ksa.financing.lending.domain.model.LoanApplicationId;
import com.ksa.financing.lending.domain.model.ShariaStructure;
import com.ksa.financing.lending.domain.port.in.ManageLoanApplicationUseCase;
import com.ksa.financing.lending.domain.port.out.EventPublisher;
import com.ksa.financing.lending.domain.port.out.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManageLoanApplicationUseCaseImpl")
class ManageLoanApplicationUseCaseImplTest {

    @Mock
    private LoanApplicationRepository applicationRepository;

    @Mock
    private EventPublisher eventPublisher;

    private ManageLoanApplicationUseCaseImpl useCase;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final UUID PRODUCT_ID = UUID.randomUUID();
    private static final UUID USER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new ManageLoanApplicationUseCaseImpl(applicationRepository, eventPublisher);
    }

    @Test
    @DisplayName("should create loan application and publish events")
    void shouldCreateApplication() {
        when(applicationRepository.generateApplicationNumber(TENANT_ID))
                .thenReturn("APP-00000001");
        when(applicationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new ManageLoanApplicationUseCase.CreateApplicationCommand(
                TENANT_ID, CUSTOMER_ID, PRODUCT_ID, "MURABAHA_PERSONAL",
                ShariaStructure.MURABAHA, new BigDecimal("100000"), 60,
                null, null, USER_ID, null
        );

        var result = useCase.createApplication(command);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.DRAFT);
        assertThat(result.getApplicationNumber()).isEqualTo("APP-00000001");

        verify(applicationRepository).save(any());
        verify(eventPublisher).publishAll(any());
    }

    @Test
    @DisplayName("should submit existing application")
    void shouldSubmitApplication() {
        var aggregate = LoanApplicationAggregate.create(
                TENANT_ID, "APP-00000001", CUSTOMER_ID, PRODUCT_ID,
                "MURABAHA_PERSONAL", ShariaStructure.MURABAHA,
                new BigDecimal("100000"), 60, USER_ID
        );
        aggregate.markEventsAsCommitted();

        when(applicationRepository.findById(eq(TENANT_ID), any(LoanApplicationId.class)))
                .thenReturn(Optional.of(aggregate));
        when(applicationRepository.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new ManageLoanApplicationUseCase.SubmitApplicationCommand(
                TENANT_ID, aggregate.getId().getValue(), USER_ID
        );

        var result = useCase.submitApplication(command);

        assertThat(result.getStatus()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(result.getSubmittedAt()).isNotNull();
        verify(eventPublisher).publishAll(any());
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
}
