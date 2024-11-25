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

package io.temporal.samples.timeoutHandler;

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.samples.workertuning.Starter;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.*;
import io.temporal.workflow.*;
import io.temporal.workflow.Functions.*;
import org.slf4j.Logger;
import picocli.CommandLine;
import static picocli.CommandLine.*;
import java.time.Duration;
import java.util.UUID;

@Command(name = "timeout_handler_example", description = "")
public class TimeoutHandler implements Runnable {
    static final String TASK_QUEUE = "TimeoutHandlerTaskQueue";
    static final String WORKFLOW_ID = "TimeoutHandlerWorkflow";

    @WorkflowInterface
    public interface TimeoutHandlerWorkflow {
        @WorkflowMethod
        String getGreeting();
    }

    public static class TimeoutHandlerWorkflowImpl implements TimeoutHandlerWorkflow {
        private static final Logger logger = Workflow.getLogger(TimeoutHandlerWorkflow.class);

        private final Starter.SlowActivities activities =
                Workflow.newActivityStub(Starter.SlowActivities.class,
                        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());
        @Override
        public String getGreeting() {
            Async.function(this::timeoutWatcher, Duration.ofSeconds(3));
            activities.slowActivity(8);
            return "done";
        }

        private Func<Void> timeoutWatcher(Duration duration) {
            Workflow.sleep(duration);
            logger.info("Timeout detected after " + duration);
            Workflow.getMetricsScope().counter("timeout_detected").inc(1);
            // Do something about the timeout
            // Start the next timeout watcher
            return timeoutWatcher(duration);
        }
    }

    @Override
    public void run() {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create factory & worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(TimeoutHandler.TimeoutHandlerWorkflowImpl.class);
        worker.registerActivitiesImplementations(new Starter.SlowActivitiesImpl());
        factory.start();

        // Setup WF stub
        TimeoutHandlerWorkflow workflow =
                client.newWorkflowStub(
                        TimeoutHandlerWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(WORKFLOW_ID + UUID.randomUUID())
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        String output = workflow.getGreeting();
        System.out.println(output);
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new TimeoutHandler()).execute(args);
    }
}
