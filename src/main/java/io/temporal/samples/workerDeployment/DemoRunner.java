package io.temporal.samples.workerDeployment;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Starts one workflow per configured Task Queue so you can watch the routing work. It is only active
 * when {@code sample.demo.enabled=true} (set by the {@code all} profile), so it never runs inside a
 * real per-domain worker deployment.
 *
 * <p>It uses untyped stubs so a single generic runner can start any domain's workflow purely by
 * (workflow type, Task Queue) — the client does not need a compile-time dependency on every domain.
 * The Temporal Server routes each start to whichever worker is polling that Task Queue, which is the
 * mechanism that lets 20 domains share one namespace without stepping on each other.
 */
@Component
@ConditionalOnProperty(name = "sample.demo.enabled", havingValue = "true")
public class DemoRunner implements ApplicationRunner {
  private static final Logger log = LoggerFactory.getLogger(DemoRunner.class);

  // Task Queue -> Workflow type name to start on it.
  private static final Map<String, String> WORKFLOW_BY_TASK_QUEUE = new LinkedHashMap<>();

  static {
    WORKFLOW_BY_TASK_QUEUE.put("orders", "OrdersWorkflow");
    WORKFLOW_BY_TASK_QUEUE.put("payments", "PaymentsWorkflow");
    WORKFLOW_BY_TASK_QUEUE.put("inventory", "InventoryWorkflow");
  }

  private final WorkflowClient client;

  @Value("${sample.demo.task-queues:orders,payments,inventory}")
  private String[] taskQueues;

  public DemoRunner(WorkflowClient client) {
    this.client = client;
  }

  @Override
  public void run(org.springframework.boot.ApplicationArguments args) {
    // The Spring Boot starter starts the WorkerFactory on ApplicationReadyEvent, which fires only
    // after all runners return. So the demo must NOT block the main thread here — otherwise the
    // workers never start and every getResult() hangs. Run it on a background thread instead.
    Thread t = new Thread(this::startWorkflows, "worker-deployment-demo");
    t.setDaemon(true);
    t.start();
  }

  private void startWorkflows() {
    // A per-run suffix keeps Workflow IDs unique so the demo can be run repeatedly.
    String runSuffix = Long.toString(System.currentTimeMillis());
    for (String taskQueue : taskQueues) {
      String workflowType = WORKFLOW_BY_TASK_QUEUE.get(taskQueue.trim());
      if (workflowType == null) {
        log.warn("No workflow mapped for task queue '{}', skipping", taskQueue);
        continue;
      }
      String orderId = "ORD-" + taskQueue.trim().toUpperCase() + "-" + runSuffix;
      WorkflowStub stub =
          client.newUntypedWorkflowStub(
              workflowType,
              WorkflowOptions.newBuilder()
                  .setTaskQueue(taskQueue.trim())
                  .setWorkflowId(workflowType + "-" + orderId)
                  .build());
      stub.start(orderId);
      log.info("Started {} on task queue '{}' -> {}", workflowType, taskQueue.trim(), orderId);
      String result = stub.getResult(String.class);
      log.info("Result from '{}' worker: {}", taskQueue.trim(), result);
    }
    log.info(
        "Demo complete. Domain workers keep polling — press Ctrl+C to stop the process.");
  }
}
