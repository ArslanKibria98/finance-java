package com.ksa.financing.identity.infrastructure.config;

import com.ksa.financing.identity.domain.port.in.RegisterFromOnboardingUseCase;
import com.ksa.financing.identity.domain.port.in.SetPinUseCase;
import com.ksa.financing.identity.domain.port.out.KeycloakAdapterPort;
import com.ksa.financing.identity.domain.port.out.UserIdentityRepository;
import com.ksa.financing.identity.workflow.activity.impl.ForgotPasscodeActivityImpl;
import com.ksa.financing.identity.workflow.activity.impl.KeycloakUserCreationActivityImpl;
import com.ksa.financing.identity.workflow.activity.impl.SetPinActivityImpl;
import com.ksa.financing.identity.workflow.impl.ForgotPasscodeWorkflowImpl;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalWorkerConfig {

    private final WorkerFactory workerFactory;
    private final WorkerOptions defaultWorkerOptions;

    private final RegisterFromOnboardingUseCase registerFromOnboardingUseCase;
    private final SetPinUseCase                 setPinUseCase;
    private final KeycloakAdapterPort           keycloakAdapterPort;
    private final UserIdentityRepository        userIdentityRepository;

    @Value("${keycloak.realm:CompanyRealm}")
    private String realm;

    @PostConstruct
    public void startWorker() {
        log.info("Starting Temporal worker on queue: {}", TaskQueue.IDENTITY_QUEUE);

        Worker worker = workerFactory.newWorker(TaskQueue.IDENTITY_QUEUE, defaultWorkerOptions);

        // Register workflow implementations
        worker.registerWorkflowImplementationTypes(ForgotPasscodeWorkflowImpl.class);

        // Register activity implementations
        worker.registerActivitiesImplementations(
                new KeycloakUserCreationActivityImpl(registerFromOnboardingUseCase, keycloakAdapterPort, realm),
                new SetPinActivityImpl(setPinUseCase),
                new ForgotPasscodeActivityImpl(userIdentityRepository, keycloakAdapterPort, realm)
        );

        workerFactory.start();
        log.info("Temporal worker started on queue: {} with ForgotPasscodeWorkflow + 3 activity implementations",
                TaskQueue.IDENTITY_QUEUE);
    }
}
