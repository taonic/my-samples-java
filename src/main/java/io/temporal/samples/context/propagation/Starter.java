package io.temporal.samples.context.propagation;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.api.common.v1.Payload;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.context.ContextPropagator;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import picocli.CommandLine;

import java.time.Duration;
import java.util.*;


public class Starter {
    static final String TASK_QUEUE = "ContextPropagation";
    static final String WORKFLOW_ID = "ContextPropagation";

    @WorkflowInterface
    public interface GreetingWorkflow {
        @WorkflowMethod
        void getGreetings();
    }

    public static class GreetingWorkflowImpl implements GreetingWorkflow {
        private final GreetingActivities activities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());
        @Override
        public void getGreetings() {
            activities.composeGreeting("hello", "world");
        }
    }

    @ActivityInterface
    public interface GreetingActivities {
        @ActivityMethod(name = "greet")
        String composeGreeting(String greeting, String name);
    }

    static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            //log.info("Composing greeting...");
            return greeting + " " + name + "!";
        }
    }

    public static class MyContextPropagator implements ContextPropagator {
        static final String KEY = "my_context";
        @Override
        public String getName() {
            return this.getClass().getName();
        }

        @Override
        public Map<String, Payload> serializeContext(Object context) {
            String strContext = (String) context;
            if (strContext != null) {
                return Collections.singletonMap(
                        KEY, DefaultDataConverter.STANDARD_INSTANCE.toPayload(strContext).get());
            } else {
                return Collections.emptyMap();
            }
        }

        @Override
        public Object deserializeContext(Map<String, Payload> context) {
            if (context.containsKey(KEY)) {
                return DefaultDataConverter.STANDARD_INSTANCE.fromPayload(
                        context.get(KEY), String.class, String.class);

            } else {
                return null;
            }
        }

        @Override
        public Object getCurrentContext() {
            return MDC.get(KEY);
        }

        @Override
        public void setCurrentContext(Object context) {
            MDC.put(KEY, String.valueOf(context));
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client = WorkflowClient.newInstance(
                service,
                WorkflowClientOptions.newBuilder().setContextPropagators(
                        Collections.singletonList(new MyContextPropagator())).build());
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        factory.start();

        WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID + UUID.randomUUID())
                        .build();
        GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);
        workflow.getGreetings();
    }
}
