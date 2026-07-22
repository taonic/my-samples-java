package io.temporal.samples.workerDeployment.domain.orders;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrdersActivities {
  String fulfill(String orderId);
}
