package io.temporal.samples.workerDeployment.domain.inventory;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface InventoryWorkflow {
  @WorkflowMethod
  String reserve(String orderId);
}
