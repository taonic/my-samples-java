package io.temporal.samples.workerDeployment.domain.payments;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface PaymentsWorkflow {
  @WorkflowMethod
  String charge(String orderId);
}
