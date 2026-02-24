package io.temporal.samples.dedicatedMigrateWorkflowBySignal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {
    
    @WorkflowMethod
    ApprovalTypes.WorkflowState execute(int requiredApprovals);
    
    @SignalMethod
    void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction);
    
    @SignalMethod
    void migrateWorkflow(String namespace, String taskQueue);
}