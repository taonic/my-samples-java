package io.temporal.samples.workerDeployment.domain.orders;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface OrdersWorkflow {
  @WorkflowMethod
  String processOrder(String orderId);
}
