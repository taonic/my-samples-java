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

package io.temporal.samples.signalWatcher;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import io.temporal.workflow.Functions.Func;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.time.Duration;
import java.util.UUID;

import static picocli.CommandLine.Command;

@Command
public class Runner implements Runnable {
    static final String TASK_QUEUE = "SignalWatcherTaskQueue";
    static final String WORKFLOW_ID = "SignalWatcherWorkflow";

    @WorkflowInterface
    public interface SignalWatcherWorkflow {
        @WorkflowMethod
        String getGreeting();

        @SignalMethod
        void resolve(String name);
    }

    public static class SignalWatcherWorkflowImpl implements SignalWatcherWorkflow {
        private static final Logger logger = Workflow.getLogger(SignalWatcherWorkflow.class);

        private boolean resolved;

        private CancellationScope watcherScope;

        @Override
        public String getGreeting() {
            Async.function(this::signalWatcher, Duration.ofSeconds(3));
            Workflow.await(() -> resolved);
            watcherScope.cancel();
            return "done";
        }

        @Override
        public void resolve(String name) {
            resolved = true;
        }

        private Func<Void> signalWatcher(Duration duration) {
            watcherScope = Workflow.newCancellationScope(
                () -> {
                    Workflow.sleep(duration);
                    logger.info("Timeout detected after " + duration);
                    Workflow.getMetricsScope().counter("timeout_detected").inc(1);
                    // Do something about the timeout
                    // Start the next timeout watcher
                });
            watcherScope.run();
            return signalWatcher(duration);
        }
    }

    @Override
    public void run() {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create factory & worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(SignalWatcherWorkflowImpl.class);
        factory.start();

        // Setup WF stub
        SignalWatcherWorkflow workflow =
                client.newWorkflowStub(
                        SignalWatcherWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        String output = workflow.getGreeting();
        System.out.println(output);
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new Runner()).execute(args);
    }
}
