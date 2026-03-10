package com.ksa.financing.wallet.unit.workflow;

import com.ksa.financing.wallet.adapter.temporal.activity.FineractSyncActivity;
import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflow;
import com.ksa.financing.wallet.adapter.temporal.workflow.FineractSyncWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowFailedException;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FineractSyncWorkflow Tests")
class FineractSyncWorkflowTest {

    private static final String TASK_QUEUE = "test-fineract-sync";

    private TestWorkflowEnvironment testEnv;
    private WorkflowClient workflowClient;
    private StubFineractSyncActivity stubActivity;

    @BeforeEach
    void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        Worker worker = testEnv.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(FineractSyncWorkflowImpl.class);

        stubActivity = new StubFineractSyncActivity();
        worker.registerActivitiesImplementations(stubActivity);

        workflowClient = testEnv.getWorkflowClient();
        testEnv.start();
    }

    @AfterEach
    void tearDown() {
        testEnv.close();
    }

    private FineractSyncWorkflow createWorkflowStub(String workflowId) {
        return workflowClient.newWorkflowStub(
                FineractSyncWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setWorkflowId(workflowId)
                        .setTaskQueue(TASK_QUEUE)
                        .build());
    }

    @Test
    @DisplayName("Should complete full sync flow successfully")
    void shouldCompleteSyncSuccessfully() {
        // Given
        stubActivity.clientIdToReturn = 10L;
        stubActivity.savingsIdToReturn = 100L;

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-123", "WLT0001", "SAR");

        // When
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-success");
        workflow.syncWalletToFineract(input);

        // Then
        assertThat(workflow.getStatus()).isEqualTo("COMPLETED");
        assertThat(stubActivity.calledMethods).containsExactly(
                "lookupFineractClient",
                "createSavingsAccount",
                "approveSavingsAccount",
                "activateSavingsAccount",
                "linkWalletToFineract"
        );
        assertThat(stubActivity.calledMethods).doesNotContain("deleteSavingsAccount");
    }

    @Test
    @DisplayName("Should compensate when approve fails after savings creation")
    void shouldCompensateWhenApproveFails() {
        // Given
        stubActivity.clientIdToReturn = 20L;
        stubActivity.savingsIdToReturn = 200L;
        stubActivity.failOnMethod = "approveSavingsAccount";

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-456", "WLT0002", "SAR");

        // When & Then
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-approve-fail");
        assertThatThrownBy(() -> workflow.syncWalletToFineract(input))
                .isInstanceOf(WorkflowFailedException.class);

        // Verify compensation: savings should be deleted
        assertThat(stubActivity.calledMethods).contains("deleteSavingsAccount");
        assertThat(stubActivity.calledMethods).doesNotContain("activateSavingsAccount");
        assertThat(stubActivity.calledMethods).doesNotContain("linkWalletToFineract");
        assertThat(stubActivity.deletedSavingsId).isEqualTo(200L);
    }

    @Test
    @DisplayName("Should compensate when activate fails after savings creation")
    void shouldCompensateWhenActivateFails() {
        // Given
        stubActivity.clientIdToReturn = 30L;
        stubActivity.savingsIdToReturn = 300L;
        stubActivity.failOnMethod = "activateSavingsAccount";

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-789", "WLT0003", "SAR");

        // When & Then
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-activate-fail");
        assertThatThrownBy(() -> workflow.syncWalletToFineract(input))
                .isInstanceOf(WorkflowFailedException.class);

        // Verify compensation
        assertThat(stubActivity.calledMethods).contains("approveSavingsAccount", "deleteSavingsAccount");
        assertThat(stubActivity.calledMethods).doesNotContain("linkWalletToFineract");
        assertThat(stubActivity.deletedSavingsId).isEqualTo(300L);
    }

    @Test
    @DisplayName("Should create Fineract client when lookup returns null and complete sync")
    void shouldCreateClientWhenLookupReturnsNull() {
        // Given - lookup returns null so activity should auto-create
        stubActivity.clientIdToReturn = null;
        stubActivity.createdClientIdToReturn = 99L;
        stubActivity.savingsIdToReturn = 900L;

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-unknown", "WLT0004", "SAR");

        // When
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-create-client");
        workflow.syncWalletToFineract(input);

        // Then - full flow completes via created client
        assertThat(workflow.getStatus()).isEqualTo("COMPLETED");
        assertThat(stubActivity.calledMethods).containsExactly(
                "lookupFineractClient",
                "createSavingsAccount",
                "approveSavingsAccount",
                "activateSavingsAccount",
                "linkWalletToFineract"
        );
    }

    @Test
    @DisplayName("Should NOT compensate when savings creation fails")
    void shouldNotCompensateWhenCreateFails() {
        // Given
        stubActivity.clientIdToReturn = 40L;
        stubActivity.failOnMethod = "createSavingsAccount";

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-111", "WLT0005", "SAR");

        // When & Then
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-create-fail");
        assertThatThrownBy(() -> workflow.syncWalletToFineract(input))
                .isInstanceOf(WorkflowFailedException.class);

        // No compensation - savingsId is null
        assertThat(stubActivity.calledMethods).doesNotContain("deleteSavingsAccount");
    }

    @Test
    @DisplayName("Should NOT compensate when link step fails (savings already active)")
    void shouldNotCompensateWhenLinkFails() {
        // Given
        stubActivity.clientIdToReturn = 50L;
        stubActivity.savingsIdToReturn = 500L;
        stubActivity.failOnMethod = "linkWalletToFineract";

        var input = new FineractSyncWorkflow.FineractSyncInput(
                "customer-222", "WLT0006", "SAR");

        // When & Then
        FineractSyncWorkflow workflow = createWorkflowStub("test-sync-link-fail");
        assertThatThrownBy(() -> workflow.syncWalletToFineract(input))
                .isInstanceOf(WorkflowFailedException.class);

        // Savings was fully activated - don't delete it
        assertThat(stubActivity.calledMethods).contains(
                "approveSavingsAccount", "activateSavingsAccount");
        assertThat(stubActivity.calledMethods).doesNotContain("deleteSavingsAccount");
    }

    /**
     * Manual activity stub for Temporal workflow testing.
     * Temporal SDK doesn't accept Mockito mocks because @ActivityMethod
     * annotations on mock proxy classes cause validation errors.
     */
    static class StubFineractSyncActivity implements FineractSyncActivity {

        Long clientIdToReturn = 1L;
        Long createdClientIdToReturn = 50L;
        Long savingsIdToReturn = 100L;
        String failOnMethod = null;
        Long deletedSavingsId = null;
        final List<String> calledMethods = new ArrayList<>();

        @Override
        public Long lookupFineractClient(String customerId) {
            calledMethods.add("lookupFineractClient");
            if ("lookupFineractClient".equals(failOnMethod)) {
                throw new RuntimeException("Lookup failed");
            }
            // Simulate lookup-or-create: if null, return created client ID
            return clientIdToReturn != null ? clientIdToReturn : createdClientIdToReturn;
        }

        @Override
        public Long createSavingsAccount(Long fineractClientId, String walletNumber) {
            calledMethods.add("createSavingsAccount");
            if ("createSavingsAccount".equals(failOnMethod)) {
                throw new RuntimeException("Create failed");
            }
            return savingsIdToReturn;
        }

        @Override
        public void approveSavingsAccount(Long savingsId) {
            calledMethods.add("approveSavingsAccount");
            if ("approveSavingsAccount".equals(failOnMethod)) {
                throw new RuntimeException("Approve failed");
            }
        }

        @Override
        public void activateSavingsAccount(Long savingsId) {
            calledMethods.add("activateSavingsAccount");
            if ("activateSavingsAccount".equals(failOnMethod)) {
                throw new RuntimeException("Activate failed");
            }
        }

        @Override
        public void linkWalletToFineract(String walletNumber, Long savingsId) {
            calledMethods.add("linkWalletToFineract");
            if ("linkWalletToFineract".equals(failOnMethod)) {
                throw new RuntimeException("Link failed");
            }
        }

        @Override
        public void deleteSavingsAccount(Long savingsId) {
            calledMethods.add("deleteSavingsAccount");
            deletedSavingsId = savingsId;
        }
    }
}
