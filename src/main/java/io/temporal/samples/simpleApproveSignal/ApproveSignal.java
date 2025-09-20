package io.temporal.samples.simpleApproveSignal;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.time.Duration;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

/**
 * Sample Temporal workflow that demonstrates how to use workflow signal methods to signal from
 * external sources.
 */
public class ApproveSignal {

  // Define the task queue name
  static final String TASK_QUEUE = "HelloSignalTaskQueue";

  // Define the workflow unique id
  static final String WORKFLOW_ID = "HelloSignalWorkflow";

  /**
   * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
   *
   * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
   * code, network calls, database operations, etc. Those things should be handled by the
   * Activities.
   *
   * @see io.temporal.workflow.WorkflowInterface
   * @see io.temporal.workflow.WorkflowMethod
   */
  @ActivityInterface
  public interface EmailActivities {
    @ActivityMethod
    void notify(String message);
  }

  public static class EmailActivitiesImpl implements EmailActivities {
    @Override
    public void notify(String message) {
      System.out.println("Email sent: " + message);
    }
  }

  @WorkflowInterface
  public interface ApprovalWorkflow {
    @WorkflowMethod
    void startProcess();

    @SignalMethod
    void approve();
  }

  // Define the workflow implementation which implements the getGreetings workflow method.
  public static class ApprovalWorkflowImpl implements ApprovalWorkflow {
    private boolean approved = false;

    private final EmailActivities emailActivities =
    Workflow.newActivityStub(EmailActivities.class,
        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

    @Override
    public void startProcess() {
      // Block current thread until the unblocking condition is evaluated to true
      Workflow.await(() -> approved);
      emailActivities.notify("Your request is approved");
    }

    @Override
    public void approve() {
      this.approved = true;
    }
  }

  /**
   * With the Workflow and Activities defined, we can now start execution. The main method starts
   * the worker and then the workflow.
   */
  public static void main(String[] args) throws Exception {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
    worker.registerActivitiesImplementations(new EmailActivitiesImpl());
    
    factory.start();

    // Create the workflow options
    WorkflowOptions workflowOptions =
        WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).setWorkflowId(WORKFLOW_ID).build();

    // Create the workflow client stub. It is used to start the workflow execution.
    ApprovalWorkflow workflow = client.newWorkflowStub(ApprovalWorkflow.class, workflowOptions);

    // Start the workflow
    workflow.startProcess();

    System.exit(0);
  }
}
