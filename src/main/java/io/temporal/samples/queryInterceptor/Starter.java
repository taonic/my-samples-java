package io.temporal.samples.queryInterceptor;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.interceptors.*;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates calling Workflow.getInfo() inside a query handler's interceptor.
 *
 * <p>Workflow.getInfo() cannot be called directly from a @QueryMethod handler (it throws
 * "Called from non workflow or workflow callback thread"). However, it CAN be called
 * inside a WorkflowInboundCallsInterceptor's handleQuery method, because interceptor
 * code runs on the workflow thread.
 *
 * <p>This sample uses an interceptor to log workflow info (workflowId, runId, workflowType)
 * each time a query is handled.
 */
public class Starter {
    static final String TASK_QUEUE = "QueryInterceptor";
    static final String WORKFLOW_ID = "QueryInterceptor";

    private static final Logger log = LoggerFactory.getLogger(Starter.class);

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        String getGreeting(String name);

        @QueryMethod
        String getStatus();
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private String status = "started";

        @Override
        public String getGreeting(String name) {
            status = "processing";
            Workflow.sleep(5000);
            status = "done";
            return "Hello, " + name + "!";
        }

        @Override
        public String getStatus() {
            return status;
        }
    }

    /**
     * Worker interceptor that creates a workflow inbound calls interceptor
     * to intercept query handling. Extends WorkerInterceptorBase which provides
     * default pass-through implementations for interceptActivity and interceptNexusOperation.
     */
    public static class QueryLoggingWorkerInterceptor extends WorkerInterceptorBase {
        @Override
        public WorkflowInboundCallsInterceptor interceptWorkflow(
                WorkflowInboundCallsInterceptor next) {
            return new QueryLoggingWorkflowInterceptor(next);
        }
    }

    /**
     * Intercepts query handling and calls Workflow.getInfo() to log workflow metadata.
     *
     * <p>This works because interceptor code executes on the workflow thread, unlike
     * the query handler itself which runs in a restricted context.
     */
    public static class QueryLoggingWorkflowInterceptor
            extends WorkflowInboundCallsInterceptorBase {

        private static final Logger logger =
                Workflow.getLogger(QueryLoggingWorkflowInterceptor.class);

        public QueryLoggingWorkflowInterceptor(WorkflowInboundCallsInterceptor next) {
            super(next);
        }

        @Override
        public QueryOutput handleQuery(QueryInput input) {
            // Print call stack to see which thread dispatches query handling
            new Exception("Query handleQuery call stack").printStackTrace(System.out);

            // Workflow.getInfo() is accessible here in the interceptor
            WorkflowInfo info = Workflow.getInfo();

            logger.info(
                    "Query '{}' received for workflow: id={}, runId={}, type={}",
                    input.getQueryName(),
                    info.getWorkflowId(),
                    info.getRunId(),
                    info.getWorkflowType());

            return super.handleQuery(input);
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactoryOptions factoryOptions =
                WorkerFactoryOptions.newBuilder()
                        .setWorkerInterceptors(new QueryLoggingWorkerInterceptor())
                        .build();
        WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);

        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        factory.start();

        WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID)
                        .build();
        GreetingWorkflow workflow =
                client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

        // Start workflow async so we can query it while it's running
        WorkflowClient.start(workflow::getGreeting, "World");
        log.info("Workflow started");

        // Query the running workflow — the interceptor will log workflow info
        String status = workflow.getStatus();
        log.info("Query result: {}", status);

        // Wait for workflow to complete
        String result = client.newUntypedWorkflowStub(WORKFLOW_ID).getResult(String.class);
        log.info("Workflow result: {}", result);

        System.exit(0);
    }
}
