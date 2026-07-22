package io.temporal.samples.workerDeployment.domain.inventory;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface InventoryActivities {
  String decrementStock(String orderId);
}
