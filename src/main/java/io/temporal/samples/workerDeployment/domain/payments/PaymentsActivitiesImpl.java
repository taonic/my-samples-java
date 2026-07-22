package io.temporal.samples.workerDeployment.domain.payments;

import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

// @Profile keeps this bean (and therefore the payments worker) out of other domains' deployments.
@Component
@Profile({"all", "payments"})
@ActivityImpl(taskQueues = PaymentsConstants.TASK_QUEUE)
public class PaymentsActivitiesImpl implements PaymentsActivities {
  private static final Logger log = LoggerFactory.getLogger(PaymentsActivitiesImpl.class);

  @Override
  public String capture(String orderId) {
    log.info("[payments worker] capturing payment for order {}", orderId);
    return "payment for order " + orderId + " captured";
  }
}
