package io.temporal.samples.dedicatedMigrateWorkflowBySignal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface ApprovalActivities {
    
    @ActivityMethod
    ApprovalTypes.CandidateApprovers getCandidateApprovers(ApprovalTypes.GetCandidateApproversRequest request);
    
    @ActivityMethod
    ApprovalTypes.ApproverTask createApproverTask(ApprovalTypes.CandidateApprovers candidateApprovers);
    
    @ActivityMethod
    ApprovalTypes.ApproverTask createApproverTaskV2(ApprovalTypes.CandidateApprovers candidateApprovers);
    
    @ActivityMethod
    ApprovalTypes.ApprovalResult submitApproverTask(ApprovalTypes.ApprovalAction approvalAction);
    
    @ActivityMethod
    ApprovalTypes.ExecutionCompletion completeApprovalExecutionData(ApprovalTypes.ApprovalResult approvalResult);
    
    @ActivityMethod
    void migrateWorkflow(String namespace, String taskQueue, ApprovalTypes.WorkflowState state);
}