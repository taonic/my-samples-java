package io.temporal.samples.feignHttpClient;

import feign.Feign;
import feign.Logger;
import feign.RequestLine;
import feign.gson.GsonDecoder;
import feign.slf4j.Slf4jLogger;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
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
import org.slf4j.LoggerFactory;

public class Starter {
    static final String TASK_QUEUE = "FeignHttpClientTaskQueue";
    static final String WORKFLOW_ID = "FeignHttpClientWorkflow";

    // Feign client interface for JSONPlaceholder API
    interface JsonPlaceholderClient {
        @RequestLine("GET /posts/{id}")
        Post getPost(String id);
    }

    // Simple POJO for the response
    static class Post {
        public int userId;
        public int id;
        public String title;
        public String body;

        @Override
        public String toString() {
            return String.format("Post{id=%d, title='%s'}", id, title);
        }
    }

    @ActivityInterface
    public interface HttpActivities {
        @ActivityMethod
        String fetchPost(int postId);
    }

    static class HttpActivitiesImpl implements HttpActivities {
        private static final org.slf4j.Logger log = LoggerFactory.getLogger(HttpActivitiesImpl.class);
        
        private final JsonPlaceholderClient client;

        public HttpActivitiesImpl() {
            log.info("Initializing Feign HTTP client");
            
            this.client = Feign.builder()
                    .decoder(new GsonDecoder())
                    .logger(new Slf4jLogger())
                    .logLevel(Logger.Level.FULL)
                    .target(JsonPlaceholderClient.class, "https://jsonplaceholder.typicode.com");
            
            log.info("Feign client initialized successfully");
        }

        @Override
        public String fetchPost(int postId) {
            log.info("Fetching post with ID: {}", postId);
            
            try {
                Post post = client.getPost(String.valueOf(postId));
                log.info("Successfully fetched post: {}", post);
                return post.toString();
            } catch (Exception e) {
                log.error("Failed to fetch post {}: {}", postId, e.getMessage());
                throw e;
            }
        }
    }

    @WorkflowInterface
    public interface HttpWorkflow {
        @WorkflowMethod
        String processPost(int postId);
    }

    public static class HttpWorkflowImpl implements HttpWorkflow {
        private static final org.slf4j.Logger log = Workflow.getLogger(HttpWorkflowImpl.class);
        
        private final HttpActivities activities = 
                Workflow.newActivityStub(
                        HttpActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(30))
                                .build());

        @Override
        public String processPost(int postId) {
            log.info("Starting workflow to process post ID: {}", postId);
            
            String result = activities.fetchPost(postId);
            
            log.info("Workflow completed successfully for post ID: {}", postId);
            return result;
        }
    }

    public static void main(String[] args) {
        // Setup Temporal client and worker
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Register workflow and activity implementations
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(HttpWorkflowImpl.class);
        worker.registerActivitiesImplementations(new HttpActivitiesImpl());

        // Start worker to begin processing tasks
        factory.start();
        System.out.println("Worker started, processing tasks on queue: " + TASK_QUEUE);

        // Create workflow stub and execute
        HttpWorkflow workflow = client.newWorkflowStub(
                HttpWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID)
                        .build());

        // Execute workflow and print result
        String result = workflow.processPost(1);
        System.out.println("Workflow result: " + result);

        System.exit(0);
    }
}