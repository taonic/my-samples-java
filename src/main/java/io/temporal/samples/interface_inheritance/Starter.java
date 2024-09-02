package io.temporal.samples.interface_inheritance;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.UUID;

public class Starter {
    // Define the task queue name
    static final String TASK_QUEUE = "InterfaceInheritance";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "InterfaceInheritance";

    @ActivityInterface
    public interface GreetingActivities {
        String composeGreeting(String greeting, String name);
    }

    public static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            return greeting + " " + name + "!";
        }
    }

    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreetings();
    }

    @WorkflowInterface
    public interface GreetingWorkflow1 extends GreetingWorkflow {
    }

    @WorkflowInterface
    public interface GreetingWorkflow2 extends GreetingWorkflow {
    }

    public static class GreetingWorkflowImpl1 implements GreetingWorkflow1 {
        private final GreetingActivities activities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(2))
                                .build());

        @Override
        public String getGreetings() {
            return activities.composeGreeting("Hello", "world 1");
        }
    }
    public static class GreetingWorkflowImpl2 implements GreetingWorkflow2 {
        private final GreetingActivities activities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(2))
                                .build());

        @Override
        public String getGreetings() {
            return activities.composeGreeting("Hello", "world 2");
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
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl1.class);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl2.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        factory.start();

        WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID + UUID.randomUUID())
                        .build();
        GreetingWorkflow1 workflow1 = client.newWorkflowStub(GreetingWorkflow1.class, workflowOptions);
        GreetingWorkflow2 workflow2 = client.newWorkflowStub(GreetingWorkflow2.class, workflowOptions);
        System.out.println(workflow1.getGreetings());
        System.out.println(workflow2.getGreetings());
        System.exit(0);
    }
}