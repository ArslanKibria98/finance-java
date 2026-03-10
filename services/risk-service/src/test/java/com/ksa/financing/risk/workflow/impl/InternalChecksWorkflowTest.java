package com.ksa.financing.risk.workflow.impl;

import com.ksa.financing.risk.domain.model.*;
import com.ksa.financing.risk.workflow.InternalChecksWorkflow;
import com.ksa.islamic.orchestration.activity.risk.*;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Timeout(30)
class InternalChecksWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;

    // Mocked activities
    private NidFormatValidationActivity nidFormatActivity;
    private CifLookupActivity cifLookupActivity;
    private DuplicateMobileCheckActivity duplicateMobileActivity;
    private BlacklistWatchlistActivity blacklistActivity;
    private FraudHistoryActivity fraudHistoryActivity;
    private DeviceFingerprintActivity deviceFingerprintActivity;
    private VelocityCheckActivity velocityCheckActivity;
    private AccountLockActivity accountLockActivity;
    private InternalSanctionsActivity internalSanctionsActivity;
    private RiskScoreActivity riskScoreActivity;

    private static final String TASK_QUEUE = "risk-assessment-test-queue";

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker(TASK_QUEUE);
        client = testEnv.getWorkflowClient();

        // Create mocks for all activities
        nidFormatActivity = mock(NidFormatValidationActivity.class);
        cifLookupActivity = mock(CifLookupActivity.class);
        duplicateMobileActivity = mock(DuplicateMobileCheckActivity.class);
        blacklistActivity = mock(BlacklistWatchlistActivity.class);
        fraudHistoryActivity = mock(FraudHistoryActivity.class);
        deviceFingerprintActivity = mock(DeviceFingerprintActivity.class);
        velocityCheckActivity = mock(VelocityCheckActivity.class);
        accountLockActivity = mock(AccountLockActivity.class);
        internalSanctionsActivity = mock(InternalSanctionsActivity.class);
        riskScoreActivity = mock(RiskScoreActivity.class);

        // Register workflow and mocked activities
        worker.registerWorkflowImplementationTypes(InternalChecksWorkflowImpl.class);
        worker.registerActivitiesImplementations(
                nidFormatActivity, cifLookupActivity, duplicateMobileActivity,
                blacklistActivity, fraudHistoryActivity, deviceFingerprintActivity,
                velocityCheckActivity, accountLockActivity, internalSanctionsActivity,
                riskScoreActivity
        );

        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    private InternalCheckRequest createTestRequest() {
        return new InternalCheckRequest(
                "1000000006", "nid-hash-123",
                "+966500000000", "mobile-hash-456",
                "device-001", "fingerprint-001",
                "192.168.1.1", "session-001", "test-tenant"
        );
    }

    private void stubAllChecksPass() {
        when(nidFormatActivity.validate(any()))
                .thenReturn(new NidFormatValidationActivity.NidValidationResult(true, "CITIZEN", null));
        when(cifLookupActivity.lookup(any()))
                .thenReturn(new CifLookupActivity.CifLookupResult(CifStatus.NEW, null, null));
        when(duplicateMobileActivity.check(any()))
                .thenReturn(new DuplicateMobileCheckActivity.DuplicateMobileResult(false, null));
        when(blacklistActivity.check(any()))
                .thenReturn(new BlacklistWatchlistActivity.BlacklistResult(CheckDecision.PASS, false, false, "CLEAR", null));
        when(fraudHistoryActivity.check(any()))
                .thenReturn(new FraudHistoryActivity.FraudHistoryResult(CheckDecision.PASS, false, false, 0, null));
        when(deviceFingerprintActivity.check(any()))
                .thenReturn(new DeviceFingerprintActivity.DeviceFingerprintResult(CheckDecision.PASS, 0, false, false, 0, null));
        when(velocityCheckActivity.check(any()))
                .thenReturn(new VelocityCheckActivity.VelocityResult(CheckDecision.PASS, false, null, 0, null));
        when(accountLockActivity.check(any()))
                .thenReturn(new AccountLockActivity.AccountLockResult(CheckDecision.PASS, false, null, null, null));
        when(internalSanctionsActivity.check(any()))
                .thenReturn(new InternalSanctionsActivity.InternalSanctionsResult(CheckDecision.PASS, false, null, null));
        when(riskScoreActivity.calculateScore(any()))
                .thenReturn(new RiskScoreActivity.RiskScoreResult(15, RiskLevel.LOW, CheckDecision.PASS, List.of("NEW_CUSTOMER"), null));
    }

    @Test
    @DisplayName("All checks pass - workflow should complete successfully")
    void allChecksPassed() {
        stubAllChecksPass();

        InternalChecksWorkflow workflow = client.newWorkflowStub(
                InternalChecksWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("test-all-pass")
                        .setTaskQueue(TASK_QUEUE)
                        .build()
        );

        InternalCheckResult result = workflow.execute(createTestRequest());

        assertEquals(RiskAssessmentStatus.COMPLETED, result.status());
        assertEquals(CheckDecision.PASS, result.overallDecision());
        assertEquals(RiskLevel.LOW, result.riskLevel());
        assertNull(result.blockReason());
        assertNull(result.failureReason());
        assertEquals(10, result.stepResults().size());
    }

    @Test
    @DisplayName("NID format validation failure should block immediately")
    void nidFormatFails() {
        when(nidFormatActivity.validate(any()))
                .thenReturn(new NidFormatValidationActivity.NidValidationResult(false, null, "Invalid format"));

        InternalChecksWorkflow workflow = client.newWorkflowStub(
                InternalChecksWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("test-nid-fail")
                        .setTaskQueue(TASK_QUEUE)
                        .build()
        );

        InternalCheckResult result = workflow.execute(createTestRequest());

        assertEquals(RiskAssessmentStatus.BLOCKED, result.status());
        assertEquals(CheckDecision.HARD_BLOCK, result.overallDecision());
        assertEquals("Invalid format", result.blockReason());
        assertEquals(0, result.stepResults().size()); // No checks completed
    }

    @Test
    @DisplayName("Blacklist hit should cause silent block")
    void blacklistHit() {
        when(nidFormatActivity.validate(any()))
                .thenReturn(new NidFormatValidationActivity.NidValidationResult(true, "CITIZEN", null));
        when(cifLookupActivity.lookup(any()))
                .thenReturn(new CifLookupActivity.CifLookupResult(CifStatus.NEW, null, null));
        when(duplicateMobileActivity.check(any()))
                .thenReturn(new DuplicateMobileCheckActivity.DuplicateMobileResult(false, null));
        when(blacklistActivity.check(any()))
                .thenReturn(new BlacklistWatchlistActivity.BlacklistResult(
                        CheckDecision.HARD_BLOCK, true, false, "BLACKLIST", null));

        InternalChecksWorkflow workflow = client.newWorkflowStub(
                InternalChecksWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("test-blacklist")
                        .setTaskQueue(TASK_QUEUE)
                        .build()
        );

        InternalCheckResult result = workflow.execute(createTestRequest());

        assertEquals(RiskAssessmentStatus.BLOCKED, result.status());
        // Silent block - generic message
        assertEquals("Unable to proceed with registration at this time.", result.blockReason());
        assertEquals(3, result.stepResults().size()); // NID + CIF + Mobile passed before blacklist
    }

    @Test
    @DisplayName("Watchlist match should add ENHANCED_MONITORING flag and continue")
    void watchlistMatch() {
        stubAllChecksPass();
        when(blacklistActivity.check(any()))
                .thenReturn(new BlacklistWatchlistActivity.BlacklistResult(
                        CheckDecision.FLAG_ENHANCED_MONITORING, false, true, "WATCHLIST", null));

        InternalChecksWorkflow workflow = client.newWorkflowStub(
                InternalChecksWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("test-watchlist")
                        .setTaskQueue(TASK_QUEUE)
                        .build()
        );

        InternalCheckResult result = workflow.execute(createTestRequest());

        assertEquals(RiskAssessmentStatus.COMPLETED, result.status());
        assertTrue(result.flags().contains("ENHANCED_MONITORING"));
    }

    @Test
    @DisplayName("Existing customer should set routeTo=LOGIN")
    void existingCustomerRoutesToLogin() {
        stubAllChecksPass();
        when(cifLookupActivity.lookup(any()))
                .thenReturn(new CifLookupActivity.CifLookupResult(CifStatus.EXISTING, "customer-123", null));

        InternalChecksWorkflow workflow = client.newWorkflowStub(
                InternalChecksWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId("test-existing")
                        .setTaskQueue(TASK_QUEUE)
                        .build()
        );

        InternalCheckResult result = workflow.execute(createTestRequest());

        assertEquals(RiskAssessmentStatus.COMPLETED, result.status());
        assertEquals("LOGIN", result.routeTo());
    }
}
