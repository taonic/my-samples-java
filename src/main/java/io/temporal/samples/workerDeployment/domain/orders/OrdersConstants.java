package io.temporal.samples.workerDeployment.domain.orders;

/** One Task Queue per domain. The name is the domain's contract with the rest of the platform. */
public final class OrdersConstants {
  public static final String TASK_QUEUE = "orders";

  private OrdersConstants() {}
}
