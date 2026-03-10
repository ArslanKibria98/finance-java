package com.ksa.financing.onboarding.infrastructure.config;

import com.ksa.financing.onboarding.workflow.activity.impl.AmlRiskScoringActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.GlobalProfileActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.NotificationActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.RiskDecisionActivityImpl;
import com.ksa.financing.onboarding.workflow.impl.CustomerOnboardingWorkflowImpl;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Temporal Worker registration for the onboarding workflow service.
 *
 * <p>Registers the workflow type and local activities (Notification, GlobalProfile).
 * All other activities (KYC, Customer, Wallet, Identity) are executed in their
 * respective services via cross-queue routing (setTaskQueue in activity stubs).</p>
 */
@Slf4j
@Configuration
public class TemporalWorkerConfig {

    private final WorkerFactory workerFactory;
    private final WorkerOptions defaultWorkerOptions;
    private final RestTemplate restTemplate;
    private final String globalProfileUrl;
    private final String riskServiceUrl;

    public TemporalWorkerConfig(WorkerFactory workerFactory,
                                WorkerOptions defaultWorkerOptions,
                                RestTemplate restTemplate,
                                @Value("${app.services.global-profile-url:http://localhost:8085}") String globalProfileUrl,
                                @Value("${app.services.risk-service-url:http://localhost:8090}") String riskServiceUrl) {
        this.workerFactory = workerFactory;
        this.defaultWorkerOptions = defaultWorkerOptions;
        this.restTemplate = restTemplate;
        this.globalProfileUrl = globalProfileUrl;
        this.riskServiceUrl = riskServiceUrl;
    }

    @PostConstruct
    public void startWorker() {
        log.info("Starting Temporal worker on queue: {}", TaskQueue.ONBOARDING_QUEUE);

        Worker worker = workerFactory.newWorker(TaskQueue.ONBOARDING_QUEUE, defaultWorkerOptions);

        // Register onboarding workflow
        worker.registerWorkflowImplementationTypes(CustomerOnboardingWorkflowImpl.class);

        // Local activities — stay on onboarding queue (no cross-service routing)
        worker.registerActivitiesImplementations(
                new NotificationActivityImpl(),
                new GlobalProfileActivityImpl(restTemplate, globalProfileUrl)
        );

        // Risk assessment worker — picks up RiskDecisionActivity + AmlRiskScoringActivity tasks
        // These run locally since risk-service is called via synchronous HTTP API
        Worker riskWorker = workerFactory.newWorker(TaskQueue.RISK_ASSESSMENT_QUEUE);
        riskWorker.registerActivitiesImplementations(
                new RiskDecisionActivityImpl(),
                new AmlRiskScoringActivityImpl(restTemplate, riskServiceUrl)
        );
        log.info("Registered RiskDecisionActivity + AmlRiskScoringActivity on queue: {}", TaskQueue.RISK_ASSESSMENT_QUEUE);

        workerFactory.start();
        log.info("Temporal workers started: {} (workflow + local activities), {} (RiskDecisionActivity)",
                TaskQueue.ONBOARDING_QUEUE, TaskQueue.RISK_ASSESSMENT_QUEUE);
    }
}
