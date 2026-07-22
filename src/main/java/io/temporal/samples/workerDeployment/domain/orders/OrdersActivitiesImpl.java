package io.temporal.samples.workerDeployment.domain.orders;

import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Activity implementations must be Spring beans ({@code @Component}) so auto-discovery can find them.
 * The {@code taskQueues} attribute registers this bean on the same {@code orders} worker as the
 * workflow above.
 *
 * <p>{@code @Profile} is what keeps a per-domain deployment isolated: {@code @Component} beans are
 * created by Spring's component scan regardless of the {@code workers-auto-discovery} package list,
 * so without this gate the {@code orders} process would also spin up payments/inventory workers just
 * because those beans exist. The bean loads only under the {@code orders} profile (its own
 * deployment) or the {@code all} profile (single-JVM local dev).
 */
@Component
@Profile({"all", "orders"})
@ActivityImpl(taskQueues = OrdersConstants.TASK_QUEUE)
public class OrdersActivitiesImpl implements OrdersActivities {
  private static final Logger log = LoggerFactory.getLogger(OrdersActivitiesImpl.class);

  @Override
  public String fulfill(String orderId) {
    log.info("[orders worker] fulfilling order {}", orderId);
    return "order " + orderId + " fulfilled";
  }
}
