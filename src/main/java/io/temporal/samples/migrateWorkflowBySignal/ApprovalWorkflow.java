package io.temporal.samples.migrateWorkflowBySignal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface ApprovalWorkflow {
    
    @WorkflowMethod
    ApprovalTypes.WorkflowState execute(ApprovalTypes.WorkflowState state);
    
    @SignalMethod
    void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction);
    
    @SignalMethod
    void migrateWorkflow(String namespace);
}