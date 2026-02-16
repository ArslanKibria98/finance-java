package com.ksa.islamic.orchestration.saga;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("SAGA Orchestrator Tests")
class SagaOrchestratorTest {

    private SagaOrchestrator<String> orchestrator;
    private List<String> executionLog;
    private List<String> compensationLog;

    @BeforeEach
    void setUp() {
        executionLog = new ArrayList<>();
        compensationLog = new ArrayList<>();
        orchestrator = SagaOrchestrator.<String>builder()
                .options(SagaOrchestrator.SagaOptions.defaultOptions())
                .build();
    }

    @Test
    @DisplayName("Should execute all steps successfully when no failures occur")
    void testSuccessfulSagaExecution() {
        // Arrange
        orchestrator
                .addStep(createStep("Step1", "step1-result"))
                .addStep(createStep("Step2", "step2-result"))
                .addStep(createStep("Step3", "step3-result"));

        // Act
        SagaOrchestrator.SagaResult<String> result = orchestrator.execute("initial-input");

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo("step3-result");
        assertThat(executionLog).containsExactly("Step1", "Step2", "Step3");
        assertThat(compensationLog).isEmpty();
    }

    @Test
    @DisplayName("Should compensate previous steps when a critical step fails")
    void testSagaCompensationOnFailure() {
        // Arrange
        orchestrator
                .addStep(createStep("Step1", "step1-result"))
                .addStep(createStep("Step2", "step2-result"))
                .addStep(createFailingStep("Step3"));

        // Act
        SagaOrchestrator.SagaResult<String> result = orchestrator.execute("initial-input");

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getFailedStep()).isEqualTo("Step3");
        assertThat(result.isCompensated()).isTrue();
        assertThat(executionLog).containsExactly("Step1", "Step2", "Step3");
        assertThat(compensationLog).containsExactly("Compensate-Step2", "Compensate-Step1");
    }

    @Test
    @DisplayName("Should continue execution when non-critical step fails")
    void testNonCriticalStepFailure() {
        // Arrange
        orchestrator
                .addStep(createStep("Step1", "step1-result"))
                .addStep(createNonCriticalFailingStep("Step2"))
                .addStep(createStep("Step3", "step3-result"));

        // Act
        SagaOrchestrator.SagaResult<String> result = orchestrator.execute("initial-input");

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getResult()).isEqualTo("step3-result");
        assertThat(executionLog).containsExactly("Step1", "Step2", "Step3");
        assertThat(compensationLog).isEmpty();
    }

    @Test
    @DisplayName("Should not compensate non-compensatable steps")
    void testNonCompensatableStep() {
        // Arrange
        orchestrator
                .addStep(createStep("Step1", "step1-result"))
                .addStep(createNonCompensatableStep("Step2", "step2-result"))
                .addStep(createFailingStep("Step3"));

        // Act
        SagaOrchestrator.SagaResult<String> result = orchestrator.execute("initial-input");

        // Assert
        assertThat(result.isSuccess()).isFalse();
        assertThat(compensationLog).containsExactly("Compensate-Step1");
        // Step2 should not be compensated
        assertThat(compensationLog).doesNotContain("Compensate-Step2");
    }

    @Test
    @DisplayName("Should retry failed steps according to max retry configuration")
    void testStepRetry() {
        // Arrange
        final int[] attemptCount = {0};
        SagaStep<String, String> retryableStep = SagaStep.<String, String>builder()
                .name("RetryableStep")
                .description("Step that fails twice then succeeds")
                .action(input -> {
                    attemptCount[0]++;
                    if (attemptCount[0] < 3) {
                        throw new RuntimeException("Temporary failure");
                    }
                    return "success";
                })
                .compensation(result -> compensationLog.add("Compensate-RetryableStep"))
                .maxRetries(3)
                .build();

        orchestrator.addStep(retryableStep);

        // Act
        SagaOrchestrator.SagaResult<String> result = orchestrator.execute("input");

        // Assert
        assertThat(result.isSuccess()).isTrue();
        assertThat(attemptCount[0]).isEqualTo(3);
        assertThat(compensationLog).isEmpty();
    }

    // Helper methods

    private SagaStep<String, String> createStep(String name, String result) {
        return SagaStep.<String, String>builder()
                .name(name)
                .description("Test step: " + name)
                .action(input -> {
                    executionLog.add(name);
                    return result;
                })
                .compensation(r -> compensationLog.add("Compensate-" + name))
                .build();
    }

    private SagaStep<String, String> createFailingStep(String name) {
        return SagaStep.<String, String>builder()
                .name(name)
                .description("Failing step: " + name)
                .action(input -> {
                    executionLog.add(name);
                    throw new RuntimeException("Step " + name + " failed");
                })
                .compensation(r -> compensationLog.add("Compensate-" + name))
                .critical(true)
                .build();
    }

    private SagaStep<String, String> createNonCriticalFailingStep(String name) {
        return SagaStep.<String, String>builder()
                .name(name)
                .description("Non-critical failing step: " + name)
                .action(input -> {
                    executionLog.add(name);
                    throw new RuntimeException("Non-critical failure");
                })
                .compensation(r -> compensationLog.add("Compensate-" + name))
                .critical(false)
                .build();
    }

    private SagaStep<String, String> createNonCompensatableStep(String name, String result) {
        return SagaStep.<String, String>builder()
                .name(name)
                .description("Non-compensatable step: " + name)
                .action(input -> {
                    executionLog.add(name);
                    return result;
                })
                .compensatable(false)
                .build();
    }
}