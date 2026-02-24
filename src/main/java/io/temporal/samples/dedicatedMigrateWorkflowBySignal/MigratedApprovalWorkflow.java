package io.temporal.samples.dedicatedMigrateWorkflowBySignal;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MigratedApprovalWorkflow {

    @WorkflowMethod
    ApprovalTypes.WorkflowState execute(ApprovalTypes.WorkflowState state);
    
    @SignalMethod
    void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction);
}
