package io.temporal.samples.workerDeployment.domain.orders;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

/**
 * The {@code taskQueues} attribute is what wires this workflow to a worker. Auto-discovery creates a
 * worker that polls the {@code orders} Task Queue and registers this implementation on it — but only
 * when the {@code orders} package is included in an active profile's {@code workers-auto-discovery}
 * list. This is how the same jar runs as different domain workers.
 */
@WorkflowImpl(taskQueues = OrdersConstants.TASK_QUEUE)
public class OrdersWorkflowImpl implements OrdersWorkflow {

  private final OrdersActivities activities =
      Workflow.newActivityStub(
          OrdersActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String processOrder(String orderId) {
    return activities.fulfill(orderId);
  }
}
