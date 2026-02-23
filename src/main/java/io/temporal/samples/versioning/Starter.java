package io.temporal.samples.versioning;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Starter {

  static final String TASK_QUEUE = "VersioningTaskQueue";
  static final String WORKFLOW_ID = "ProcessOrderWorkflow";

  @WorkflowInterface
  public interface OrderWorkflow {
    @WorkflowMethod
    void processOrder(String orderId);
  }

  @ActivityInterface
  public interface OrderActivities {
    void checkFraud(String orderId);
    void prepareShipment(String orderId);
    void charge(String orderId);
  }

  public static class OrderWorkflowImpl implements OrderWorkflow {

    private final OrderActivities activities =
        Workflow.newActivityStub(
            OrderActivities.class,
            ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(10)).build());

    @Override
    public void processOrder(String orderId) {
      activities.checkFraud(orderId);
      activities.prepareShipment(orderId);
      activities.charge(orderId);
    }
  }

  public static class OrderActivitiesImpl implements OrderActivities {
    private static final Logger log = LoggerFactory.getLogger(OrderActivitiesImpl.class);

    @Override
    public void prepareShipment(String orderId) {
      log.info("Preparing shipment for order: {}", orderId);
    }

    @Override
    public void checkFraud(String orderId) {
      log.info("Checking fraud for order: {}", orderId);
    }

    @Override
    public void charge(String orderId) {
      log.info("Charging for order: {}", orderId);
    }
  }

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);
    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(OrderWorkflowImpl.class);
    worker.registerActivitiesImplementations(new OrderActivitiesImpl());
    factory.start();

    OrderWorkflow workflow =
        client.newWorkflowStub(
            OrderWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build());

    workflow.processOrder("order-123");
    System.out.println("Order processed successfully.");
    System.exit(0);
  }
}
