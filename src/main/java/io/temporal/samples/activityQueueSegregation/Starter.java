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

package io.temporal.samples.activityQueueSegregation;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/** Sample Temporal Workflow Definition that executes Activities on separate Task Queues. */
public class Starter {

    // Define the task queue name
    static final String TASK_QUEUE = "HelloActivityTaskQueue";

    // Define our workflow unique id
    static final String WORKFLOW_ID = "HelloActivityWorkflow";

    /**
     * The Workflow Definition's Interface must contain one method annotated with @WorkflowMethod.
     *
     * <p>Workflow Definitions should not contain any heavyweight computations, non-deterministic
     * code, network calls, database operations, etc. Those things should be handled by the
     * Activities.
     *
     * @see io.temporal.workflow.WorkflowInterface
     * @see io.temporal.workflow.WorkflowMethod
     */
    @WorkflowInterface
    public interface GreetingWorkflow {

        /**
         * This is the method that is executed when the Workflow Execution is started. The Workflow
         * Execution completes when this method finishes execution.
         */
        @WorkflowMethod
        String getGreeting(String name);
    }

    /**
     * This is the Activity Definition's Interface. Activities are building blocks of any Temporal
     * Workflow and contain any business logic that could perform long running computation, network
     * calls, etc.
     *
     * <p>Annotating Activity Definition methods with @ActivityMethod is optional.
     *
     * @see io.temporal.activity.ActivityInterface
     * @see io.temporal.activity.ActivityMethod
     */
    @ActivityInterface
    public interface GreetingActivities {
        // Define your activity method which can be called during workflow execution
        @ActivityMethod(name = "greet")
        String composeGreeting(String greeting, String name);

        @ActivityMethod(name = "intro")
        String intro(String name);
    }

    // Define the workflow implementation which implements our getGreeting workflow method.
    public static class GreetingWorkflowImpl implements GreetingWorkflow {

        /**
         * Create two activity stubs with specific task queues and execution options
         * to invoke activities from within the workflow.
         */
        private final GreetingActivities activities1 =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(20))
                                .setTaskQueue(TASK_QUEUE + "Activity1")
                                .build());

        private final GreetingActivities activities2 =
                Workflow.newActivityStub(
                        GreetingActivities.class,
                        ActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(20))
                                .setTaskQueue(TASK_QUEUE + "Activity2")
                                .build());

        @Override
        public String getGreeting(String name) {
            String result = activities1.composeGreeting("Hello", name);
            result += " " + activities2.intro("Temporal");
            return result;
        }
    }

    /** Simple activity implementation, that concatenates two strings. */
    static class GreetingActivitiesImpl implements GreetingActivities {
        private static final Logger log = LoggerFactory.getLogger(GreetingActivitiesImpl.class);

        @Override
        public String composeGreeting(String greeting, String name) {
            log.info("Composing greeting...");
            return greeting + " " + name + "!";
        }

        @Override
        public String intro(String name) {
            log.info("Intro...");
            return "I'm " + name + ".";
        }
    }

    /**
     * With our Workflow and Activities defined, we can now start execution. The main method starts
     * the worker and then the workflow.
     */
    public static void main(String[] args) {

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
         * Set up a Temporal worker to handle workflow tasks and two separate Activity workers, each
         * assigned to different task queues, to execute activities using the specified implementations.
         */
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

        Worker activityWorker1 = factory.newWorker(TASK_QUEUE + "Activity1");
        activityWorker1.registerActivitiesImplementations(new GreetingActivitiesImpl());

        Worker activityWorker2 = factory.newWorker(TASK_QUEUE + "Activity2");
        activityWorker2.registerActivitiesImplementations(new GreetingActivitiesImpl());

        /*
         * Start all the workers
         */
        factory.start();

        // Create the workflow client stub. It is used to start our workflow execution.
        GreetingWorkflow workflow =
                client.newWorkflowStub(
                        GreetingWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        /*
         * Execute our workflow and wait for it to complete. The call to our getGreeting method is
         * synchronous.
         *
         * See {@link io.temporal.samples.hello.HelloSignal} for an example of starting workflow
         * without waiting synchronously for its result.
         */
        String greeting = workflow.getGreeting("World");

        // Display workflow execution results
        System.out.println(greeting);
        System.exit(0);
    }
}
