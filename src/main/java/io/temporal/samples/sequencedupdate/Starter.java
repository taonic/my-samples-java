/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.sequencedupdate;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample Temporal workflow that demonstrates how to use workflow update methods to update a
 * workflow execution from external sources. Workflow update is another way to interact with a
 * running workflow along with signals and queries. Workflow update combines aspects of signals and
 * queries. Like signals, workflow update can mutate workflow state. Like queries, workflow update
 * can return a value.
 *
 * <p>Note: Make sure to set {@code frontend.enableUpdateWorkflowExecution=true} in your Temporal
 * config to enabled update.
 */
public class Starter {

    // Define the task queue name
    static final String TASK_QUEUE = "HelloSequencedUpdateTaskQueue";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "HelloSequencedUpdateWorkflow";

    @ActivityInterface
    public interface GreetingActivities {

        // Define your activity method which can be called during workflow execution
        @ActivityMethod(name = "greet")
        String composeGreeting(String greeting, String name);
    }

    static class GreetingActivitiesImpl implements GreetingActivities {
        private static final Logger log =
                LoggerFactory.getLogger(GreetingActivitiesImpl.class);

        @Override
        public String composeGreeting(String greeting, String name) {
            Random rand = new Random();
            try {
                Thread.sleep(rand.nextInt(2000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.info("Composing greeting...");
            return greeting + " " + name + "!";
        }
    }

    /**
     * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
     *
     * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
     * code, network calls, database operations, etc. Those things should be handled by the
     * Activities.
     *
     * @see WorkflowInterface
     * @see WorkflowMethod
     */
    @WorkflowInterface
    public interface GreetingWorkflow {
        /**
         * This is the method that is executed when the Workflow Execution is started. The Workflow
         * Execution completes when this method finishes execution.
         */
        @WorkflowMethod
        List<String> getGreetings();

        /*
         * Define the workflow addGreeting update method. This method is executed when the workflow
         * receives an update request.
         */
        @UpdateMethod
        String addGreeting(String name);

        // Define the workflow exit signal method. This method is executed when the workflow receives a
        // signal.
        @SignalMethod
        void exit();
    }

    // Define the workflow implementation which implements the getGreetings workflow method.
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        private static class QueuedCommand {
            public String value;
            public CompletablePromise<String> promise;

            public QueuedCommand(String value, CompletablePromise<String> promise) {
                this.value = value;
                this.promise = promise;
            }
        }
        // messageQueue holds up to 10 messages (received from updates)
        private final WorkflowQueue<QueuedCommand> commandQueue = Workflow.newWorkflowQueue(10);
        private final List<String> receivedMessages = new ArrayList<>(10);
        private boolean exit = false;

        private final GreetingActivities activities =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(2)).build());

        @Override
        public List<String> getGreetings() {
            while (true) {
                if (exit && commandQueue.peek() == null) {
                    return receivedMessages;
                }
                QueuedCommand queuedCommand =
                        commandQueue.take(); // It will block at here if the queue is empty
                String result = activities.composeGreeting("Hello", queuedCommand.value);
                receivedMessages.add(result);
                queuedCommand.promise.complete(
                        result); // complete the command promise so update (addGreeting) can return the result
            }
        }

        @Override
        public String addGreeting(String value) {
            if (value.isEmpty()) {
                /*
                 * Updates can fail by throwing a TemporalFailure. All other exceptions cause the workflow
                 * task to fail and potentially retried.
                 *
                 * Note: A check like this could (and should) belong in the validator, this is just to demonstrate failing an
                 * update.
                 */
                throw ApplicationFailure.newFailure("Cannot greet someone with an empty name", "Failure");
            }
            CompletablePromise<String> promise = Workflow.newPromise();
            QueuedCommand queuedCommand = new QueuedCommand(value, promise);
            this.commandQueue.put(queuedCommand);
            return queuedCommand.promise.get();
        }

        @Override
        public void exit() {
            exit = true;
        }
    }

    /**
     * With the Workflow and Activities defined, we can now start execution. The main method starts
     * the worker and then the workflow.
     */
    public static void main(String[] args) throws Exception {

        // Get a Workflow service stub.
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

        /*
         * Get a Workflow service client which can be used to start, Signal, and Query Workflow Executions.
         */
        WorkflowClient client = WorkflowClient.newInstance(service);

        /*
         * Define the workflow factory. It is used to create workflow workers for a specific task queue.
         */
        WorkerFactory factory = WorkerFactory.newInstance(client);

        /*
         * Define the workflow worker. Workflow workers listen to a defined task queue and process
         * workflows and activities.
         */
        Worker worker =
                factory.newWorker(
                        TASK_QUEUE,
                        WorkerOptions.newBuilder().setDefaultDeadlockDetectionTimeout(100000).build());

        /*
         * Register the workflow implementation with the worker.
         * Workflow implementations must be known to the worker at runtime in
         * order to dispatch workflow tasks.
         */
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

        /*
         * Register our Activity Types with the Worker. Since Activities are stateless and thread-safe,
         * the Activity Type is a shared instance.
         */
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());

        /*
         * Start all the workers registered for a specific task queue.
         * The started workers then start polling for workflows and activities.
         */
        factory.start();

        // Create the workflow options
        WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(TASK_QUEUE)
                        .setWorkflowId(WORKFLOW_ID + UUID.randomUUID())
                        .build();

        // Create the workflow client stub. It is used to start the workflow execution.
        GreetingWorkflow workflow = client.newWorkflowStub(GreetingWorkflow.class, workflowOptions);

        // Start workflow asynchronously and call its getGreeting workflow method
        WorkflowClient.start(workflow::getGreetings);

        // Send updates concurrently
        final int concurrency = 10;
        CompletableFuture<String>[] futures = new CompletableFuture[concurrency];
        WorkflowStub untypedWorkflowStub = WorkflowStub.fromTyped(workflow);

        for (int i = 0; i < concurrency; i++) {
            Thread.sleep(50); // space out the requests to make sure Updates is received in order
            CompletableFuture<String> f =
                    untypedWorkflowStub
                            .startUpdate("addGreeting", String.class, "Again" + i)
                            .getResultAsync();
            futures[i] = f;
        }

        // Now let's send our exit signal to the workflow
        workflow.exit();

        /*
         * We now call our getGreetings workflow method synchronously after our workflow has started.
         * This reconnects our workflowById workflow stub to the existing workflow and blocks until
         * a result is available. Note that this behavior assumes that WorkflowOptions are not configured
         * with WorkflowIdReusePolicy.AllowDuplicate. If they were, this call would fail with the
         * WorkflowExecutionAlreadyStartedException exception.
         */
        List<String> greetings = workflow.getGreetings();

        // Print our two greetings which were sent by signals
        System.out.println(greetings);
        System.exit(0);
    }
}
