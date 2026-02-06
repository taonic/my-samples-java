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
import feign.FeignException;
import feign.Feign;
import feign.RequestLine;
import feign.Param;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import java.time.Duration;

public class CustomExceptionSample {

    static final String TASK_QUEUE = "CustomExceptionTaskQueue";

    interface HttpBinClient {
        @RequestLine("GET /status/{code}")
        String getStatus(@Param("code") int code);
    }

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
        String findItem(String itemId) throws FeignException.BadRequest;
    }

    static class ItemActivityImpl implements ItemActivity {
        private final HttpBinClient httpClient = Feign.builder()
            .encoder(new GsonEncoder())
            .decoder(new GsonDecoder())
            .target(HttpBinClient.class, "https://httpbin.org");

        @Override
        public String findItem(String itemId) throws FeignException.BadRequest {
            if ("missing".equals(itemId)) {
                // Make real HTTP request that returns 400 Bad Request
                httpClient.getStatus(400);
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
                    .setDoNotRetry(FeignException.BadRequest.class.getName())
                    .build())
                .build()
        );

        @Override
        public String processItem(String itemId) {
            return activity.findItem(itemId);
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
            System.out.println("Outer error: " + e.getCause().toString());
        }

        System.exit(0);
    }
}
