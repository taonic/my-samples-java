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

package io.temporal.samples.retrynde;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.NonDeterministicException;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkflowImplementationOptions;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.unsafe.WorkflowUnsafe;

/**
 * Sample Temporal Workflow Definition that demonstrates retrying Workflow on non-deterministic errors.
 * How to run it:
 * You'll need to restart the program after the first appearance of "Is replaying: false" in order to trigger a replay.
 * "Is replaying: true" will be displayed immediately to indicate the workflow is being replayed.
 * On the UI, you should see a new workflow running with the old one failed due to NDE. If you don't restart the worker,
 * the newly started workflow should run to completion after 10s.
 */
public class RetryNDE {

    // Define the task queue name
    static final String TASK_QUEUE = "RetryNDETaskQueue";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "RetryNDEWorkflow";

    @WorkflowInterface
    public interface RetryNDEWorkflow {
        @WorkflowMethod
        String getGreeting();
    }

    public static class GreetingWorkflowImpl implements RetryNDEWorkflow {

        @Override
        public String getGreeting() {
            // Induce a NDE
            System.out.println("Is replaying: " + WorkflowUnsafe.isReplaying());
            if (WorkflowUnsafe.isReplaying()) {
                Workflow.sideEffect(String.class, () -> "forcing an NDE");
            }

            // Give enough time to restart the worker
            Workflow.sleep(10000);
            return "done";
        }
    }

    public static void main(String[] args) {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);

        // Tell workflow to fail on NonDeterministicExceptions
        WorkflowImplementationOptions options =
                WorkflowImplementationOptions.newBuilder()
                        .setFailWorkflowExceptionTypes(NonDeterministicException.class)
                        .build();
        worker.registerWorkflowImplementationTypes(options, GreetingWorkflowImpl.class);
        factory.start();

        // Set retry policy on workflow. This is usually not recommended, but we are demonstrating a special case
        // that forces workflow re-run on a non-deterministic error.
        RetryNDEWorkflow workflow =
                client.newWorkflowStub(
                        RetryNDEWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setRetryOptions(RetryOptions.newBuilder().build())
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        String output = workflow.getGreeting();
        System.out.println(output);
        System.exit(0);
    }
}
