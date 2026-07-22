package io.temporal.samples.workerDeployment.domain.payments;

import io.temporal.activity.ActivityOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Workflow;
import java.time.Duration;

@WorkflowImpl(taskQueues = PaymentsConstants.TASK_QUEUE)
public class PaymentsWorkflowImpl implements PaymentsWorkflow {

  private final PaymentsActivities activities =
      Workflow.newActivityStub(
          PaymentsActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

  @Override
  public String charge(String orderId) {
    return activities.capture(orderId);
  }
}
