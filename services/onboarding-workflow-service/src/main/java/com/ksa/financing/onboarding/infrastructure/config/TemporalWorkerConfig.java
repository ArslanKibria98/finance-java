package com.ksa.financing.onboarding.infrastructure.config;

import com.ksa.financing.onboarding.canada.workflow.CanadaOnboardingWorkflowImpl;
import com.ksa.financing.onboarding.foreign.workflow.ForeignOnboardingWorkflowImpl;
import com.ksa.financing.onboarding.guest.workflow.GuestOnboardingWorkflowImpl;
import com.ksa.financing.onboarding.shared.activity.impl.DualOtpActivityImpl;
import com.ksa.financing.onboarding.shared.activity.impl.FaciaActivityImpl;
import com.ksa.financing.onboarding.shared.activity.impl.SullisActivityImpl;
import com.ksa.financing.onboarding.shared.document.DocumentStorageClient;
import com.ksa.financing.onboarding.shared.activity.impl.OnboardingProfileActivityImpl;
import com.ksa.financing.onboarding.shared.audit.OnboardingSessionRecorder;
import com.ksa.financing.onboarding.shared.downstream.OnboardingDownstreamClient;
import com.ksa.financing.onboarding.shared.facia.FaciaClient;
import com.ksa.financing.onboarding.shared.sullis.SullisClient;
import com.ksa.financing.onboarding.shared.keycloak.OnboardingKeycloakClient;
import com.ksa.financing.onboarding.workflow.activity.impl.AmlRiskScoringActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.GeneralScoringActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.GlobalProfileActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.NotificationActivityImpl;
import com.ksa.financing.onboarding.workflow.activity.impl.RiskDecisionActivityImpl;
import com.ksa.financing.onboarding.workflow.impl.CustomerOnboardingWorkflowImpl;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import com.ksa.financing.onboarding.shared.activity.impl.DualOtpActivityImpl.EmailOtpConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.javamail.JavaMailSender;
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
    private final FaciaClient faciaClient;
    private final SullisClient sullisClient;
    private final OnboardingKeycloakClient onboardingKeycloakClient;
    private final OnboardingSessionRecorder recorder;
    private final OnboardingDownstreamClient downstream;
    private final String canadaTaskQueue;
    private final String canadaCustomerRole;
    private final String guestTaskQueue;
    private final String foreignTaskQueue;
    private final DocumentStorageClient documentStorageClient;
    private final String defaultTenantId;
    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate;
    private final EmailOtpConfig emailOtpConfig;

    public TemporalWorkerConfig(WorkerFactory workerFactory,
                                WorkerOptions defaultWorkerOptions,
                                RestTemplate restTemplate,
                                FaciaClient faciaClient,
                                SullisClient sullisClient,
                                OnboardingKeycloakClient onboardingKeycloakClient,
                                OnboardingSessionRecorder recorder,
                                OnboardingDownstreamClient downstream,
                                DocumentStorageClient documentStorageClient,
                                JavaMailSender mailSender,
                                StringRedisTemplate redisTemplate,
                                @Value("${app.services.global-profile-url:http://localhost:8085}") String globalProfileUrl,
                                @Value("${app.services.risk-service-url:http://localhost:8090}") String riskServiceUrl,
                                @Value("${canada.onboarding.task-queue:canada-onboarding-queue}") String canadaTaskQueue,
                                @Value("${keycloak.default-customer-role:customer}") String canadaCustomerRole,
                                @Value("${guest.onboarding.task-queue:guest-onboarding-queue}") String guestTaskQueue,
                                @Value("${foreign.onboarding.task-queue:foreign-onboarding-queue}") String foreignTaskQueue,
                                @Value("${platform.default-tenant-id:00000000-0000-0000-0000-000000000001}") String defaultTenantId,
                                @Value("${otp.email.enabled:true}") boolean otpEmailEnabled,
                                @Value("${otp.email.from:}") String otpEmailFrom,
                                @Value("${otp.email.from-name:Sullis Onboarding}") String otpEmailFromName,
                                @Value("${otp.email.subject:Your verification code}") String otpEmailSubject,
                                @Value("${otp.email.length:6}") int otpEmailLength,
                                @Value("${otp.email.ttl-seconds:300}") long otpEmailTtlSeconds) {
        this.workerFactory = workerFactory;
        this.defaultWorkerOptions = defaultWorkerOptions;
        this.restTemplate = restTemplate;
        this.faciaClient = faciaClient;
        this.sullisClient = sullisClient;
        this.onboardingKeycloakClient = onboardingKeycloakClient;
        this.recorder = recorder;
        this.downstream = downstream;
        this.documentStorageClient = documentStorageClient;
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
        this.globalProfileUrl = globalProfileUrl;
        this.riskServiceUrl = riskServiceUrl;
        this.canadaTaskQueue = canadaTaskQueue;
        this.canadaCustomerRole = canadaCustomerRole;
        this.guestTaskQueue = guestTaskQueue;
        this.foreignTaskQueue = foreignTaskQueue;
        this.defaultTenantId = defaultTenantId;
        this.emailOtpConfig = new EmailOtpConfig(otpEmailEnabled, otpEmailFrom, otpEmailFromName,
                otpEmailSubject, otpEmailLength, otpEmailTtlSeconds);
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
                new AmlRiskScoringActivityImpl(restTemplate, riskServiceUrl),
                new GeneralScoringActivityImpl(restTemplate, riskServiceUrl)
        );
        log.info("Registered RiskDecisionActivity + AmlRiskScoringActivity + GeneralScoringActivity on queue: {}", TaskQueue.RISK_ASSESSMENT_QUEUE);

        // Canada onboarding worker — registers the full Canada workflow and shares the
        // Facia + DualOtp + Profile activity implementations.
        Worker canadaWorker = workerFactory.newWorker(canadaTaskQueue, defaultWorkerOptions);
        canadaWorker.registerWorkflowImplementationTypes(CanadaOnboardingWorkflowImpl.class);
        canadaWorker.registerActivitiesImplementations(
                new SullisActivityImpl(sullisClient, recorder, documentStorageClient, defaultTenantId),
                new DualOtpActivityImpl(recorder, mailSender, redisTemplate, emailOtpConfig),
                new OnboardingProfileActivityImpl(onboardingKeycloakClient, recorder, downstream, canadaCustomerRole)
        );
        log.info("Registered Canada onboarding workflow on queue: {} (Sullis KYC)", canadaTaskQueue);

        // Country-agnostic guest worker — minimal Keycloak-only flow, onboardingComplete=false.
        // Reuses the OTP + Profile activity implementations (Facia not needed for guest).
        Worker guestWorker = workerFactory.newWorker(guestTaskQueue, defaultWorkerOptions);
        guestWorker.registerWorkflowImplementationTypes(GuestOnboardingWorkflowImpl.class);
        guestWorker.registerActivitiesImplementations(
                new DualOtpActivityImpl(recorder, mailSender, redisTemplate, emailOtpConfig),
                new OnboardingProfileActivityImpl(onboardingKeycloakClient, recorder, downstream, canadaCustomerRole)
        );
        log.info("Registered guest onboarding workflow on queue: {}", guestTaskQueue);

        // Foreign national worker — passport-only KYC via Sullis (session flow), plus
        // the shared OTP + Profile stack. (Canada still uses Facia above.)
        Worker foreignWorker = workerFactory.newWorker(foreignTaskQueue, defaultWorkerOptions);
        foreignWorker.registerWorkflowImplementationTypes(ForeignOnboardingWorkflowImpl.class);
        foreignWorker.registerActivitiesImplementations(
                new SullisActivityImpl(sullisClient, recorder, documentStorageClient, defaultTenantId),
                new DualOtpActivityImpl(recorder, mailSender, redisTemplate, emailOtpConfig),
                new OnboardingProfileActivityImpl(onboardingKeycloakClient, recorder, downstream, canadaCustomerRole)
        );
        log.info("Registered foreign onboarding workflow on queue: {} (Sullis KYC)", foreignTaskQueue);

        workerFactory.start();
        log.info("Temporal workers started: {} (KSA), {} (RiskDecision), {} (Canada), {} (Guest), {} (Foreign)",
                TaskQueue.ONBOARDING_QUEUE, TaskQueue.RISK_ASSESSMENT_QUEUE,
                canadaTaskQueue, guestTaskQueue, foreignTaskQueue);
    }
}
