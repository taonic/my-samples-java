package io.temporal.samples.manualActivityRetry;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.failure.ActivityFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.SignalMethod;
import io.temporal.common.SearchAttributeKey;
import java.time.Duration;
import java.util.Random;
import org.slf4j.Logger;

public class Starter {
    static final String TASK_QUEUE = "ManualActivityRetryTaskQueue";
    static final String WORKFLOW_ID = "ManualActivityRetryWorkflow";

    @ActivityInterface
    public interface GreetingActivities {
        @ActivityMethod
        String getGreeting(String name);

        @ActivityMethod
        String formatMessage(String greeting);
    }

    // Activity implementation that fails when input string is prefixed with "invalid"
    static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String getGreeting(String input) {
            if (input.startsWith("invalid")) {
                throw ApplicationFailure.newNonRetryableFailure("invalid input", "ValidationFailure");
            }
            return "Hi " + input;
        }

        @Override
        public String formatMessage(String input) {
            if (input.startsWith("invalid")) {
                throw ApplicationFailure.newNonRetryableFailure("invalid input", "ValidationFailure");
            }
            return input + "!";
        }
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
        
        @SignalMethod
        void retry(String payload);
    }

    // Workflow implementation demonstrating manual retry with signal
    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private static final Logger log = Workflow.getLogger(GreetingWorkflowImpl.class);
        
        private final GreetingActivities activities = 
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(10))
                                .build());
        
        private String retryPayload = null;

        
        /**
         * Manual retry pattern: waits for external signal instead of automatic retries.
         * Tracks failed activities via search attributes for monitoring.
         */
        private <T> T executeWithManualRetry(java.util.function.Function<String, T> activityCall, String activityName) {
            while (true) {
                try {
                    String input = retryPayload != null ? retryPayload : "invalid-input";
                    T result = activityCall.apply(input);

                    // Clear failure state
                    Workflow.setCurrentDetails("");
                    Workflow.upsertTypedSearchAttributes(SearchAttributeKey.forText("FailedActivity").valueUnset());
                    retryPayload = null;
                    return result;
                } catch (ActivityFailure e) {
                    log.warn("Activity {} failed: {}", activityName, e.getMessage());

                    // Set failure state
                    Workflow.setCurrentDetails("Got error " + e.getMessage() + ", waiting on user to send signal 'retry'");
                    Workflow.upsertTypedSearchAttributes(SearchAttributeKey.forText("FailedActivity").valueSet(activityName));

                    // Wait for manual retry
                    Workflow.await(() -> retryPayload != null);
                }
            }
        }

        @Override
        public String getGreeting(String name) {
            String greet = executeWithManualRetry(input -> activities.getGreeting(input), "getGreeting");
            String message = executeWithManualRetry(input -> activities.formatMessage(input), "formatMessage");
            
            return String.format("%s %s", greet, message);
        }
        
        @Override
        public void retry(String payload) {
            retryPayload = payload;
        }
    }

    public static void main(String[] args) {
        // Setup Temporal client and worker
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Register workflow and activity implementations
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

        // Start worker to begin processing tasks
        factory.start();

        // Create workflow stub and execute
        String workflowId = WORKFLOW_ID + "-" + Integer.toHexString(new Random().nextInt(0x1000000));
        GreetingWorkflow workflow = client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(workflowId)
                        .build());

        // Execute workflow and print result
        String result = workflow.getGreeting("World");
        System.out.println(result);

        System.exit(0);
    }
}
