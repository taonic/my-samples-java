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

package io.temporal.samples.groupedActivities;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.*;
import io.temporal.workflow.*;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static picocli.CommandLine.Command;

@Command(name = "grouped_activities", description = "")
public class GroupedActivities implements Runnable {
    static final String TASK_QUEUE = "GroupedActivitiesTaskQueue";
    static final String WORKFLOW_ID = "GroupedActivitiesWorkflow";
    @WorkflowInterface
    public interface GroupedActivitiesWorkflow {
        @WorkflowMethod
        String run();
    }

    @ActivityInterface
    public interface GreetingActivities {
        @ActivityMethod
        String composeGreeting(String greeting, String name);
    }

    public static class GroupedActivitiesWorkflowImpl implements GroupedActivitiesWorkflow {
        private static final Logger logger = Workflow.getLogger(GroupedActivitiesWorkflow.class);

        private final GreetingActivities activities =
                Workflow.newActivityStub(GreetingActivities.class,
                        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(5)).build());
        @Override
        public String run() {
            List<String> results = new ArrayList<>();
            List<Promise<String>> promises = new ArrayList<>();
            promises.add(Async.function(this::activityGroup1));
            promises.add(Async.function(this::activityGroup2));
            for (Promise<String> promise : promises) {
                if (promise.getFailure() == null) {
                    results.add(promise.get());
                } else {
                    // handle failed activity
                    logger.error("Failed activity", promise.getFailure());
                }
            }
            return String.join(", ", results);
        }

        private String activityGroup1() {
            String result = activities.composeGreeting("hello", "world");
            String output = activities.composeGreeting(result, "again");
            return output;
        }

        private String activityGroup2() {
            List<String> results = new ArrayList<>();
            List<Promise<String>> promises = new ArrayList<>();
            promises.add(Async.function(activities::composeGreeting, "hello", "world1"));
            promises.add(Async.function(activities::composeGreeting, "hello", "world2"));
            for (Promise<String> promise : promises) {
                if (promise.getFailure() == null) {
                    results.add(promise.get());
                } else {
                    // handle failed activity
                    logger.error("Failed activity", promise.getFailure());
                }
            }
            return String.join(", ", results);
        }
    }

    static class GreetingActivitiesImpl implements GreetingActivities {
        @Override
        public String composeGreeting(String greeting, String name) {
            // conditionally fail an activity
            if (name.equals("world2")) {
                throw ApplicationFailure.newNonRetryableFailure("test", "test");
            }
            return greeting + " " + name;
        }
    }

    @Override
    public void run() {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        // Create factory & worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(GroupedActivitiesWorkflowImpl.class);
        worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
        factory.start();

        // Setup WF stub
        GroupedActivitiesWorkflow workflow = client.newWorkflowStub(
            GroupedActivitiesWorkflow.class,
            WorkflowOptions.newBuilder()
                    .setWorkflowId(WORKFLOW_ID + UUID.randomUUID())
                    .setTaskQueue(TASK_QUEUE)
                    .build()
        );
        String output = workflow.run();
        System.out.println(output);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new GroupedActivities()).execute(args);
    }
}
