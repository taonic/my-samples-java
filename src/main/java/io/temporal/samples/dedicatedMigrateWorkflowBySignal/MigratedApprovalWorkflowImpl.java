package io.temporal.samples.dedicatedMigrateWorkflowBySignal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.time.Duration;

public class MigratedApprovalWorkflowImpl implements MigratedApprovalWorkflow {
    
    private static final String CREATE_APPROVER_TASK_VERSION = "createApproverTaskVersion";
    
    private final ApprovalActivities activities = Workflow.newActivityStub(
        ApprovalActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .build()
    );
    
    private ApprovalTypes.WorkflowState state;
    
    @Override
    public ApprovalTypes.WorkflowState execute(ApprovalTypes.WorkflowState initialState) {
        this.state = initialState;
        
        // Get current version and update state
        int createApproverTaskVersion = Workflow.getVersion(
            CREATE_APPROVER_TASK_VERSION,
            Workflow.DEFAULT_VERSION,
            state.getVersionFor(CREATE_APPROVER_TASK_VERSION)
        );
        state.getVersionMap().put(CREATE_APPROVER_TASK_VERSION, createApproverTaskVersion);
        
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
        return state;
    }
    
    @Override
    public void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction) {
        state.setApprovalAction(approvalAction);
    }
}
