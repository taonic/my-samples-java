package io.temporal.samples.customException;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;

public class CustomExceptionSample {

    static final String TASK_QUEUE = "CustomExceptionTaskQueue";

    // Custom exception types
    public static class ItemNotFoundException extends Exception {
        public ItemNotFoundException(String message) {
            super(message);
        }
    }

    public static class WorkflowItemNotFoundException extends Exception {
        public WorkflowItemNotFoundException(String message) {
            super(message);
        }
    }

    @ActivityInterface
    public interface ItemActivity {
        @ActivityMethod
        String findItem(String itemId);
    }

    static class ItemActivityImpl implements ItemActivity {
        @Override
        public String findItem(String itemId) {
            if ("missing".equals(itemId)) {
                throw ApplicationFailure.newFailure(
                    "Item with ID " + itemId + " not found.",
                    ItemNotFoundException.class.getSimpleName()
                );
            }
            return "Found item: " + itemId;
        }
    }

    @WorkflowInterface
    public interface ItemWorkflow {
        @WorkflowMethod
        String processItem(String itemId);
    }

    public static class ItemWorkflowImpl implements ItemWorkflow {
        private final ItemActivity activity = Workflow.newActivityStub(
            ItemActivity.class,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofSeconds(10))
                .setRetryOptions(RetryOptions.newBuilder()
                    .setDoNotRetry(ItemNotFoundException.class.getSimpleName())
                    .build())
                .build()
        );

        @Override
        public String processItem(String itemId) {
            try {
                return activity.findItem(itemId);
            } catch (ActivityFailure e) {
                if (e.getCause() instanceof ApplicationFailure) {
                    ApplicationFailure appFailure = (ApplicationFailure) e.getCause();
                    if (ItemNotFoundException.class.getSimpleName().equals(appFailure.getType())) {
                        throw ApplicationFailure.newNonRetryableFailure(
                            "Workflow: " + appFailure.getOriginalMessage(),
                            WorkflowItemNotFoundException.class.getSimpleName()
                        );
                    }
                }
                throw e;
            }
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ItemWorkflowImpl.class);
        worker.registerActivitiesImplementations(new ItemActivityImpl());

        factory.start();

        ItemWorkflow workflow = client.newWorkflowStub(
            ItemWorkflow.class,
            WorkflowOptions.newBuilder()
                .setTaskQueue(TASK_QUEUE)
                .setWorkflowId("CustomExceptionWorkflow")
                .build()
        );

        try {
            String result = workflow.processItem("missing");
            System.out.println("Result: " + result);
        } catch (Exception e) {
            if (e.getCause() instanceof ApplicationFailure) {
                ApplicationFailure appFailure = (ApplicationFailure) e.getCause();
                if (WorkflowItemNotFoundException.class.getSimpleName().equals(appFailure.getType())) {
                    System.out.println("Not found error: " + appFailure.getOriginalMessage());
                }
            }
        }

        System.exit(0);
    }
}
