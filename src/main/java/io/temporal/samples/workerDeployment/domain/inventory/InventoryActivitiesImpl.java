package io.temporal.samples.workerDeployment.domain.inventory;

import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// @Profile keeps this bean (and therefore the inventory worker) out of other domains' deployments.
@Component
@Profile({"all", "inventory"})
@ActivityImpl(taskQueues = InventoryConstants.TASK_QUEUE)
public class InventoryActivitiesImpl implements InventoryActivities {
  private static final Logger log = LoggerFactory.getLogger(InventoryActivitiesImpl.class);

  @Override
  public String decrementStock(String orderId) {
    log.info("[inventory worker] reserving stock for order {}", orderId);
    return "stock reserved for order " + orderId;
  }
}
