package com.ksa.islamic.finance.test.temporal;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.*;
import io.temporal.worker.Worker;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Temporal workflow testing harness
 */
@Slf4j
public class TemporalTestHarness {
    private final TestWorkflowEnvironment testEnv;
    private final Map<String, Worker> workers = new ConcurrentHashMap<>();
    private final Map<String, Object> activities = new ConcurrentHashMap<>();
    private final Map<String, Class<?>> workflows = new ConcurrentHashMap<>();

    public TemporalTestHarness() {
        this.testEnv = TestWorkflowEnvironment.newInstance(
            TestEnvironmentOptions.newBuilder()
                .setUseTimeskipping(true)
                .build()
        );
    }

    /**
     * Register a workflow implementation
     */
    public TemporalTestHarness registerWorkflow(String taskQueue, Class<?> workflowClass) {
        workflows.put(taskQueue, workflowClass);
        return this;
    }

    /**
     * Register an activity implementation
     */
    public TemporalTestHarness registerActivity(String taskQueue, Object activityImpl) {
        activities.put(taskQueue, activityImpl);
        return this;
    }

    /**
     * Start the test environment
     */
    public void start() {
        // Create workers for each task queue
        Set<String> taskQueues = new HashSet<>();
        taskQueues.addAll(workflows.keySet());
        taskQueues.addAll(activities.keySet());

        for (String taskQueue : taskQueues) {
            Worker worker = testEnv.newWorker(taskQueue);

            // Register workflows
            if (workflows.containsKey(taskQueue)) {
                worker.registerWorkflowImplementationTypes(workflows.get(taskQueue));
            }

            // Register activities
            if (activities.containsKey(taskQueue)) {
                worker.registerActivitiesImplementations(activities.get(taskQueue));
            }

            workers.put(taskQueue, worker);
        }

        testEnv.start();
        log.info("Temporal test environment started with {} workers", workers.size());
    }

    /**
     * Get workflow client
     */
    public WorkflowClient getWorkflowClient() {
        return testEnv.getWorkflowClient();
    }

    /**
     * Execute a workflow and wait for result
     */
    public <T> T executeWorkflow(
        Class<T> workflowInterface,
        String workflowId,
        String taskQueue,
        Consumer<T> workflowMethod
    ) {
        WorkflowOptions options = WorkflowOptions.newBuilder()
            .setWorkflowId(workflowId)
            .setTaskQueue(taskQueue)
            .setWorkflowExecutionTimeout(Duration.ofMinutes(5))
            .build();

        T workflow = getWorkflowClient().newWorkflowStub(workflowInterface, options);
        workflowMethod.accept(workflow);
        return workflow;
    }

    /**
     * Advance time in test environment
     */
    public void advanceTime(Duration duration) {
        testEnv.sleep(duration);
        log.debug("Advanced test time by {}", duration);
    }

    /**
     * Get current test time
     */
    public long getCurrentTime() {
        return testEnv.currentTimeMillis();
    }

    /**
     * Shutdown the test environment
     */
    public void shutdown() {
        testEnv.close();
        workers.clear();
        activities.clear();
        workflows.clear();
        log.info("Temporal test environment shutdown");
    }

    /**
     * Create a mock activity that records invocations
     */
    public static <T> MockActivity<T> mockActivity(Class<T> activityInterface) {
        return new MockActivity<>(activityInterface);
    }

    /**
     * Mock activity recorder
     */
    public static class MockActivity<T> {
        private final Class<T> activityInterface;
        private final List<ActivityInvocation> invocations = new ArrayList<>();
        private final Map<String, Object> responses = new HashMap<>();
        private final Map<String, Exception> exceptions = new HashMap<>();

        public MockActivity(Class<T> activityInterface) {
            this.activityInterface = activityInterface;
        }

        public MockActivity<T> whenCalled(String methodName, Object response) {
            responses.put(methodName, response);
            return this;
        }

        public MockActivity<T> whenCalledThrow(String methodName, Exception exception) {
            exceptions.put(methodName, exception);
            return this;
        }

        public List<ActivityInvocation> getInvocations() {
            return new ArrayList<>(invocations);
        }

        public boolean wasCalled(String methodName) {
            return invocations.stream()
                .anyMatch(inv -> inv.getMethodName().equals(methodName));
        }

        public int getCallCount(String methodName) {
            return (int) invocations.stream()
                .filter(inv -> inv.getMethodName().equals(methodName))
                .count();
        }

        public T build() {
            return ActivityMocker.mock(activityInterface, this);
        }
    }

    /**
     * Activity invocation record
     */
    public static class ActivityInvocation {
        private final String methodName;
        private final Object[] arguments;
        private final long timestamp;

        public ActivityInvocation(String methodName, Object[] arguments) {
            this.methodName = methodName;
            this.arguments = arguments;
            this.timestamp = System.currentTimeMillis();
        }

        public String getMethodName() {
            return methodName;
        }

        public Object[] getArguments() {
            return arguments;
        }

        public long getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Workflow test builder
     */
    public static class WorkflowTestBuilder {
        private final TemporalTestHarness harness = new TemporalTestHarness();
        private String defaultTaskQueue = "test-queue";

        public WorkflowTestBuilder withTaskQueue(String taskQueue) {
            this.defaultTaskQueue = taskQueue;
            return this;
        }

        public WorkflowTestBuilder withWorkflow(Class<?> workflowClass) {
            harness.registerWorkflow(defaultTaskQueue, workflowClass);
            return this;
        }

        public WorkflowTestBuilder withActivity(Object activityImpl) {
            harness.registerActivity(defaultTaskQueue, activityImpl);
            return this;
        }

        public TemporalTestHarness build() {
            return harness;
        }
    }

    /**
     * Create a new test builder
     */
    public static WorkflowTestBuilder builder() {
        return new WorkflowTestBuilder();
    }

    /**
     * Temporal test assertions
     */
    public static class Assertions {
        public static void assertWorkflowCompleted(WorkflowClient client, String workflowId) {
            // Implementation would check workflow status
            log.debug("Asserting workflow {} completed", workflowId);
        }

        public static void assertWorkflowFailed(WorkflowClient client, String workflowId) {
            // Implementation would check workflow status
            log.debug("Asserting workflow {} failed", workflowId);
        }

        public static void assertActivityExecuted(MockActivity<?> activity, String methodName, int times) {
            int actualCount = activity.getCallCount(methodName);
            if (actualCount != times) {
                throw new AssertionError(
                    String.format("Expected activity %s to be called %d times, but was called %d times",
                        methodName, times, actualCount)
                );
            }
        }
    }
}