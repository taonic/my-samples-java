package io.temporal.samples.localActivityWithFallback;

// Demonstrates local activity fallback pattern with retry behavior:
//
// 1. EXECUTION FLOW:
//    - Default: Local activities fail → retry 3 times → fallback to normal activities → succeed
//    - With --succeed-local: Local activities succeed immediately (no fallback needed)
//
// 2. RETRY MECHANISM: Local activities are configured with RetryOptions (max 3 attempts, 100ms interval)
//    - When local activities fail, Temporal automatically retries them up to the configured limit
//    - Each retry attempt is logged showing the attempt number
//
// 3. FALLBACK BEHAVIOR: After local activity retries are exhausted, workflow catches the exception
//    - Workflow automatically falls back to executing the same operation as a normal activity
//    - This provides resilience when local activities consistently fail
//    - For details read: https://community.temporal.io/t/local-activity-vs-activity/290/3
//
// This pattern is useful for optimizing performance (local activities are faster) while maintaining
// reliability through fallback to normal activities when local execution is problematic.

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.activity.LocalActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
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

    static final String TASK_QUEUE = "LocalActivityWithFallbackTaskQueue";
    static final String WORKFLOW_ID = "LocalActivityWithFallbackWorkflow";
    // Flag to control whether local activities should succeed (use --succeed-local to enable)
    static boolean SUCCEED_LOCAL_ACTIVITIES = false;

    @ActivityInterface
    public interface GreetingActivities {
        @ActivityMethod
        String getGreeting(String name);

        @ActivityMethod
        String formatMessage(String greeting);
    }

    // Activity implementation that can simulate failures for local activities
    static class GreetingActivitiesImpl implements GreetingActivities {
        private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

        @Override
        public String getGreeting(String name) {
            // Get activity execution context to check if it's local and get attempt number
            var info = Activity.getExecutionContext().getInfo();
            int attempt = info.getAttempt();
            log.info("getGreeting attempt: {}", attempt);
            // Fail if this is a local activity and success flag is disabled
            if (info.isLocal() && !SUCCEED_LOCAL_ACTIVITIES) {
                throw new RuntimeException("Simulated failure on LA attempt " + attempt);
            }
            return "Hello, " + name;
        }

        @Override
        public String formatMessage(String greeting) {
            var info = Activity.getExecutionContext().getInfo();
            int attempt = Activity.getExecutionContext().getInfo().getAttempt();
            log.info("formatMessage attempt: {}", attempt);
            // Same failure logic for consistency
            if (info.isLocal() && !SUCCEED_LOCAL_ACTIVITIES) {
                throw new RuntimeException("Simulated failure on LA attempt " + attempt);
            }
            return greeting + " 🌏";
        }
    }

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);
    }

    // Workflow implementation demonstrating fallback from local to normal activities
    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private static final Logger log = LoggerFactory.getLogger(GreetingWorkflowImpl.class);
        // Local activity stub with short timeout and retry policy
        private final GreetingActivities localActivities =
                Workflow.newLocalActivityStub(
                        GreetingActivities.class,
                        LocalActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(2))
                                .setRetryOptions(RetryOptions.newBuilder()
                                        .setMaximumAttempts(3)
                                        .setInitialInterval(Duration.ofMillis(50))
                                        .setBackoffCoefficient(1)
                                        .build())
                                .build());

        // Normal activity stub as fallback with longer timeout
        private final GreetingActivities normalActivities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(10))
                                .build());

        @Override
        public String getGreeting(String name) {
            // Try local activity first, fallback to normal activity on retry exhaustion
            String greeting;
            try {
                greeting = localActivities.getGreeting(name);
            } catch (ActivityFailure e) {
                log.warn("Local activity {} failed, falling back to normal activity: {}", e.getActivityType(), e.getMessage());
                greeting = normalActivities.getGreeting(name);
            }

            String result;
            try {
                result = localActivities.formatMessage(greeting);
            } catch (ActivityFailure e) {
                log.warn("Local activity {} failed, falling back to normal activity: {}", e.getActivityType(), e.getMessage());
                result = normalActivities.formatMessage(greeting);
            }

            return result;
        }
    }

    public static void main(String[] args) {
        // Parse command line flag to enable local activity success
        if (args.length > 0 && "--succeed-local".equals(args[0])) {
            SUCCEED_LOCAL_ACTIVITIES = true;
        }
        
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
        GreetingWorkflow workflow = client.newWorkflowStub(
                GreetingWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID)
                        .build());

        // Execute workflow and print result
        String result = workflow.getGreeting("World");
        System.out.println(result);

        System.exit(0);
    }
}
