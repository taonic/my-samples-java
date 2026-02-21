package io.temporal.samples.migrateWorkflowBySignal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.activity.Activity;
import java.util.Arrays;

public class ApprovalActivitiesImpl implements ApprovalActivities {
    
    @Override
    public ApprovalTypes.CandidateApprovers getCandidateApprovers(ApprovalTypes.GetCandidateApproversRequest request) {
        return new ApprovalTypes.CandidateApprovers(
            Arrays.asList("approver1@company.com", "approver2@company.com")
        );
    }
    
    @Override
    public ApprovalTypes.ApproverTask createApproverTask(ApprovalTypes.CandidateApprovers candidateApprovers) {
        return new ApprovalTypes.ApproverTask(
            "task-" + System.currentTimeMillis(),
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
            "task-v2-" + System.currentTimeMillis(),
            candidateApprovers.getApprovers(),
            "PENDING"
        );
    }
    
    @Override
    public void migrateWorkflow(String namespace, ApprovalTypes.WorkflowState state) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(
            WorkflowServiceStubsOptions.newBuilder().setTarget("localhost:7233").build()
        );
        WorkflowClient client = WorkflowClient.newInstance(service, 
            WorkflowClientOptions.newBuilder().setNamespace(namespace).build());
        
        ApprovalWorkflow workflow = client.newWorkflowStub(
            ApprovalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(Activity.getExecutionContext().getInfo().getWorkflowId())
                .setTaskQueue(Activity.getExecutionContext().getInfo().getActivityTaskQueue())
                .build()
        );
        
        WorkflowClient.start(workflow::execute, state);
    }
}