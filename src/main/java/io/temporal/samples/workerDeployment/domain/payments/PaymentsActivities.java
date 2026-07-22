package io.temporal.samples.workerDeployment.domain.payments;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface PaymentsActivities {
  String capture(String orderId);
}
