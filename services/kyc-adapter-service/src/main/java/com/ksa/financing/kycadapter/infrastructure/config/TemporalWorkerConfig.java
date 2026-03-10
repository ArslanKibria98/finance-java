package com.ksa.financing.kycadapter.infrastructure.config;

import com.ksa.financing.kycadapter.domain.port.in.*;
import com.ksa.financing.kycadapter.workflow.activity.impl.*;
import com.ksa.islamic.orchestration.common.TaskQueue;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TemporalWorkerConfig {

    private final WorkerFactory workerFactory;
    private final WorkerOptions defaultWorkerOptions;

    private final VerifyMobileUseCase verifyMobileUseCase;
    private final SendOtpUseCase sendOtpUseCase;
    private final VerifyOtpUseCase verifyOtpUseCase;
    private final InitiateNafathUseCase initiateNafathUseCase;
    private final VerifyIdentityUseCase verifyIdentityUseCase;
    private final ScreenSanctionsUseCase screenSanctionsUseCase;
    private final ScreenPepUseCase screenPepUseCase;
    private final FetchSalaryUseCase fetchSalaryUseCase;

    @PostConstruct
    public void startWorker() {
        log.info("Starting Temporal worker on queue: {}", TaskQueue.KYC_QUEUE);

        Worker worker = workerFactory.newWorker(TaskQueue.KYC_QUEUE, defaultWorkerOptions);

        worker.registerActivitiesImplementations(
                new MobileVerificationActivityImpl(verifyMobileUseCase),
                new OtpSendActivityImpl(sendOtpUseCase),
                new OtpVerifyActivityImpl(verifyOtpUseCase),
                new NafathVerificationActivityImpl(initiateNafathUseCase),
                new YakeenVerificationActivityImpl(verifyIdentityUseCase),
                new SanctionsScreeningActivityImpl(screenSanctionsUseCase),
                new PepScreeningActivityImpl(screenPepUseCase),
                new SalaryFetchActivityImpl(fetchSalaryUseCase)
        );

        workerFactory.start();
        log.info("Temporal worker started on queue: {} with 8 activity implementations", TaskQueue.KYC_QUEUE);
    }
}
