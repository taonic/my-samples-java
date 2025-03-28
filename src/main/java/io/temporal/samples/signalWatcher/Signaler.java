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
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import io.temporal.workflow.Functions.Func;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import picocli.CommandLine;

import java.time.Duration;
import java.util.UUID;

import static picocli.CommandLine.Command;

@Command
public class Signaler implements Runnable {
    @SneakyThrows
    @Override
    public void run() {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkflowStub workflow = client.newUntypedWorkflowStub(Runner.WORKFLOW_ID);
        System.out.println("Sending signal 1");
        workflow.signal("resolve", 1);
        Thread.sleep(5000);
        System.out.println("Sending signal 2");
        workflow.signal("resolve", 2);
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new Signaler()).execute(args);
    }
}
