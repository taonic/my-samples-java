package io.temporal.samples.heartbeatRetry;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates using activity heartbeat to maintain per-error-code retry counts across attempts.
 *
 * <p>Heartbeat details (a Map&lt;String, Integer&gt; keyed by error code) persist across activity
 * retries. Each error code has its own max attempts and exponential backoff configuration. Custom
 * retry delays are applied via {@link ApplicationFailure#newFailureWithCauseAndDelay}.
 */
public class Starter {

    static final String TASK_QUEUE = "HeartbeatRetryTaskQueue";
    static final String WORKFLOW_ID = "HeartbeatRetryWorkflow";

    // Per-error-code retry configuration
    static class ErrorRetryConfig {
        final int maxAttempts;
        final int baseBackoffSeconds;
        final double backoffFactor;

        ErrorRetryConfig(int maxAttempts, int baseBackoffSeconds, double backoffFactor) {
            this.maxAttempts = maxAttempts;
            this.baseBackoffSeconds = baseBackoffSeconds;
            this.backoffFactor = backoffFactor;
        }
    }

    static final Map<String, ErrorRetryConfig> ERROR_CONFIGS = new HashMap<>();

    static {
        ERROR_CONFIGS.put("429", new ErrorRetryConfig(5, 1, 2.0));
        ERROR_CONFIGS.put("401", new ErrorRetryConfig(2, 1, 1.5));
        ERROR_CONFIGS.put("502", new ErrorRetryConfig(3, 3, 1.1));
    }

    @ActivityInterface
    public interface ApiCallActivity {
        @ActivityMethod
        String callApi(String endpoint);
    }

    static class ApiCallActivityImpl implements ApiCallActivity {
        private static final Logger log = LoggerFactory.getLogger(ApiCallActivityImpl.class);

        // Simulated error sequence before eventual success
        private static final int[] ERROR_SEQUENCE = {429, 502, 429, 429, 502, 401, 502, 429};
        private static int callIndex = 0;

        @Override
        public String callApi(String endpoint) {
            ActivityExecutionContext ctx = Activity.getExecutionContext();

            // Retrieve heartbeat details from previous attempt (persisted across retries)
            @SuppressWarnings("unchecked")
            Map<String, Integer> retryCounts =
                    (Map<String, Integer>)
                            ctx.getHeartbeatDetails(Map.class).orElseGet(HashMap::new);

            log.info("Activity started. Retry counts from heartbeat: {}", retryCounts);

            // Simulate an API call that may return an error code
            int errorCode = simulateApiCall();

            if (errorCode == 0) {
                log.info("API call to {} succeeded!", endpoint);
                return "Success: Response from " + endpoint;
            }

            String errorKey = String.valueOf(errorCode);
            ErrorRetryConfig config = ERROR_CONFIGS.get(errorKey);
            if (config == null) {
                throw ApplicationFailure.newNonRetryableFailure(
                        "Unhandled error code: " + errorCode, "UnhandledError");
            }

            // Increment counter for this specific error code
            int count = retryCounts.getOrDefault(errorKey, 0) + 1;
            retryCounts.put(errorKey, count);

            // Heartbeat with updated map - this persists to the next retry attempt
            ctx.heartbeat(retryCounts);

            log.info(
                    "HTTP {} error (count {}/{} for this code). All counts: {}",
                    errorCode,
                    count,
                    config.maxAttempts,
                    retryCounts);

            // Check if max attempts exceeded for this error code
            if (count >= config.maxAttempts) {
                throw ApplicationFailure.newNonRetryableFailure(
                        String.format(
                                "Exceeded %d retries for HTTP %d", config.maxAttempts, errorCode),
                        "MaxRetriesExceeded");
            }

            // Exponential backoff: base * factor^(count-1)
            Duration backoff =
                    Duration.ofSeconds(
                            config.baseBackoffSeconds * (long) Math.pow(config.backoffFactor, count - 1));
            log.info("Scheduling retry with backoff {} for HTTP {}", backoff, errorCode);

            // Throw retryable failure with custom next retry delay
            throw ApplicationFailure.newFailureWithCauseAndDelay(
                    String.format(
                            "HTTP %d (attempt %d/%d for this error)",
                            errorCode, count, config.maxAttempts),
                    "HttpError",
                    null,
                    backoff);
        }

        private synchronized int simulateApiCall() {
            if (callIndex < ERROR_SEQUENCE.length) {
                return ERROR_SEQUENCE[callIndex++];
            }
            return 0; // success after all simulated errors
        }
    }

    @WorkflowInterface
    public interface ApiWorkflow {
        @WorkflowMethod
        String callExternalApi(String endpoint);
    }

    public static class ApiWorkflowImpl implements ApiWorkflow {
        private static final Logger log = Workflow.getLogger(ApiWorkflowImpl.class);

        private final ApiCallActivity activity =
                Workflow.newActivityStub(
                        ApiCallActivity.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofMinutes(5))
                                .setHeartbeatTimeout(Duration.ofSeconds(30))
                                .setRetryOptions(
                                        RetryOptions.newBuilder()
                                                // High max so the activity controls termination
                                                // via heartbeat counts, not the retry policy
                                                .setMaximumAttempts(20)
                                                .build())
                                .build());

        @Override
        public String callExternalApi(String endpoint) {
            log.info("Starting API call to: {}", endpoint);
            return activity.callApi(endpoint);
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ApiWorkflowImpl.class);
        worker.registerActivitiesImplementations(new ApiCallActivityImpl());

        factory.start();

        String workflowId =
                WORKFLOW_ID + "-" + Integer.toHexString(new Random().nextInt(0x1000000));
        ApiWorkflow workflow =
                client.newWorkflowStub(
                        ApiWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setTaskQueue(TASK_QUEUE)
                                .setWorkflowId(workflowId)
                                .build());

        System.out.println("Starting workflow: " + workflowId);

        try {
            String result = workflow.callExternalApi("/api/data");
            System.out.println("Result: " + result);
        } catch (Exception e) {
            System.out.println("Workflow failed: " + e.getMessage());
        }

        System.exit(0);
    }
}
