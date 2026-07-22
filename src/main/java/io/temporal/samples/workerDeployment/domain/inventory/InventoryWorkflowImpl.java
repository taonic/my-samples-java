package io.temporal.samples.workerDeployment.domain.inventory;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = InventoryConstants.TASK_QUEUE)
public class InventoryWorkflowImpl implements InventoryWorkflow {

  private final InventoryActivities activities =
      Workflow.newActivityStub(
          InventoryActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String reserve(String orderId) {
    return activities.decrementStock(orderId);
  }
}
