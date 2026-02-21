package io.temporal.samples.migrateWorkflowBySignal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {
    
    private static final String CREATE_APPROVER_TASK_VERSION = "createApproverTaskVersion";
    
    private final ApprovalActivities activities = Workflow.newActivityStub(
        ApprovalActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .build()
    );
    
    private ApprovalTypes.WorkflowState state;
    private String newNamespace;
    
    @Override
    public ApprovalTypes.WorkflowState execute(ApprovalTypes.WorkflowState initialState) {
        this.state = initialState != null ? initialState : new ApprovalTypes.WorkflowState();
        
        // Get current version and update state
        int createApproverTaskVersion = Workflow.getVersion(
            CREATE_APPROVER_TASK_VERSION,
            Workflow.DEFAULT_VERSION,
            state.getVersionFor(CREATE_APPROVER_TASK_VERSION)
        );
        state.getVersionMap().put(CREATE_APPROVER_TASK_VERSION, createApproverTaskVersion);
        
        // Start migration watcher and approval process in parallel
        Promise<Void> migrationPromise = Async.function(() -> {
            Workflow.await(() -> newNamespace != null);
            state.setMigrated(true);
            activities.migrateWorkflow(newNamespace, state);
            return null;
        });
        
        Promise<Void> approvalPromise = Async.function(() -> {
            // Get candidate approvers if not loaded
            if (state.getCandidateApprovers() == null) {
                ApprovalTypes.GetCandidateApproversRequest request = new ApprovalTypes.GetCandidateApproversRequest();
                ApprovalTypes.CandidateApprovers candidateApprovers = activities.getCandidateApprovers(request);
                state.setCandidateApprovers(candidateApprovers);
            }
            
            // Create approver task if not exists
            if (state.getApproverTask() == null) {
                ApprovalTypes.ApproverTask approverTask;
                if (createApproverTaskVersion >= 2) {
                    approverTask = activities.createApproverTaskV2(state.getCandidateApprovers());
                } else {
                    approverTask = activities.createApproverTask(state.getCandidateApprovers());
                }
                state.setApproverTask(approverTask);
            }

            // Collect required approvals
            while (state.getApprovalResults().size() < state.getRequiredApprovals()) {
                Workflow.await(() -> state.getApprovalAction() != null);
                
                ApprovalTypes.ApprovalResult result = activities.submitApproverTask(state.getApprovalAction());
                state.getApprovalResults().add(result);
                
                if (!result.isApproved()) break;
                
                state.setApprovalAction(null);
            }
            
            // Complete execution
            ApprovalTypes.ExecutionCompletion executionCompletion;
            if (createApproverTaskVersion >= 2) {
                executionCompletion = activities.completeApprovalExecutionData(
                    state.getApprovalResults().isEmpty() ? null : state.getApprovalResults().get(state.getApprovalResults().size() - 1)
                );
            } else {
                executionCompletion = activities.completeApprovalExecutionData(null);
            }
            state.setExecutionCompletion(executionCompletion);
            return null;
        });
        
        // Wait for either migration start or approval completion
        Promise.anyOf(migrationPromise, approvalPromise).get();
        
        return state;
    }
    
    @Override
    public void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction) {
        state.setApprovalAction(approvalAction);
    }
    
    @Override
    public void migrateWorkflow(String namespace) {
        this.newNamespace = namespace;
    }
}