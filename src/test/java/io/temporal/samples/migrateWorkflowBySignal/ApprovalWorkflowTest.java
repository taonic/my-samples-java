package io.temporal.samples.migrateWorkflowBySignal;

import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;

public class ApprovalWorkflowTest {

    private TestWorkflowEnvironment testEnv;
    private Worker worker;
    private WorkflowClient client;

    @BeforeEach
    public void setUp() {
        testEnv = TestWorkflowEnvironment.newInstance();
        worker = testEnv.newWorker("test-task-queue");
        worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
        worker.registerActivitiesImplementations(new TestApprovalActivitiesImpl());
        testEnv.start();
        client = testEnv.getWorkflowClient();
    }

    @AfterEach
    public void tearDown() {
        testEnv.close();
    }

    @Test
    public void testApprovalWorkflowApproved() {
        ApprovalWorkflow workflow = client.newWorkflowStub(ApprovalWorkflow.class,
                io.temporal.client.WorkflowOptions.newBuilder()
                        .setTaskQueue("test-task-queue")
                        .build());

        ApprovalTypes.WorkflowState initialState = new ApprovalTypes.WorkflowState();
        
        testEnv.registerDelayedCallback(
                Duration.ofSeconds(1),
                () -> workflow.submitApprovalAction(
                        new ApprovalTypes.ApprovalAction(true, "approver1@company.com", "Approved")
                )
        );

        workflow.execute(initialState);
    }

    @Test
    public void testApprovalWorkflowRejected() {
        ApprovalWorkflow workflow = client.newWorkflowStub(ApprovalWorkflow.class,
                io.temporal.client.WorkflowOptions.newBuilder()
                        .setTaskQueue("test-task-queue")
                        .build());

        ApprovalTypes.WorkflowState initialState = new ApprovalTypes.WorkflowState();
        
        testEnv.registerDelayedCallback(
                Duration.ofSeconds(1),
                () -> workflow.submitApprovalAction(
                        new ApprovalTypes.ApprovalAction(false, "approver2@company.com", "Rejected")
                )
        );

        workflow.execute(initialState);
    }

    private static class TestApprovalActivitiesImpl implements ApprovalActivities {
        
        @Override
        public ApprovalTypes.CandidateApprovers getCandidateApprovers(ApprovalTypes.GetCandidateApproversRequest request) {
            return new ApprovalTypes.CandidateApprovers(
                Arrays.asList("approver1@company.com", "approver2@company.com")
            );
        }
        
        @Override
        public ApprovalTypes.ApproverTask createApproverTask(ApprovalTypes.CandidateApprovers candidateApprovers) {
            return new ApprovalTypes.ApproverTask(
                "test-task-123",
                candidateApprovers.getApprovers(),
                "PENDING"
            );
        }
        
        @Override
        public ApprovalTypes.ApprovalResult submitApproverTask(ApprovalTypes.ApprovalAction approvalAction) {
            return new ApprovalTypes.ApprovalResult(
                approvalAction.isApproved(),
                approvalAction.getApprover(),
                System.currentTimeMillis()
            );
        }
        
        @Override
        public ApprovalTypes.ExecutionCompletion completeApprovalExecutionData(ApprovalTypes.ApprovalResult approvalResult) {
            return new ApprovalTypes.ExecutionCompletion(
                "COMPLETED",
                System.currentTimeMillis()
            );
        }
        
        @Override
        public ApprovalTypes.ApproverTask createApproverTaskV2(ApprovalTypes.CandidateApprovers candidateApprovers) {
            return new ApprovalTypes.ApproverTask(
                "test-task-v2-123",
                candidateApprovers.getApprovers(),
                "PENDING"
            );
        }
        
        @Override
        public void migrateWorkflow(String namespace, ApprovalTypes.WorkflowState state) {
            // Test implementation - just log the migration
            System.out.println("Test: Migrating workflow to namespace: " + namespace);
        }
    }
}