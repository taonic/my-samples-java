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

import com.sun.net.httpserver.HttpServer;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.RetryOptions;
import io.temporal.common.converter.*;
import io.temporal.samples.proto.AddressBook;
import io.temporal.samples.proto.Person;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.*;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.net.ssl.SSLException;
import java.io.FileNotFoundException;

import static picocli.CommandLine.*;
import java.time.Duration;

@Command(name = "proto_workflow_example", description = "")
public class ProtoWorkflowRunner implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(ProtoWorkflowRunner.class);

    private static HttpServer scrapeEndpoint;

    // Define the task queue name
    static final String TASK_QUEUE = "ProtoWorkflowTaskQueue";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "ProtoWorkflow";

    @WorkflowInterface
    public interface ProtoWorkflow {
        @WorkflowMethod
        AddressBook getGreeting(Person person);
    }

    public static class ProtoWorkflowImpl implements ProtoWorkflow {
        @Override
        public AddressBook getGreeting(Person person) {
            System.out.println(person.getEmail());
            return AddressBook.newBuilder().addPeople(person).build();
        }
    }

    private static WorkflowClient runWorker() throws FileNotFoundException, SSLException {
        // Build workflow client
        WorkflowServiceStubsOptions.Builder wfServiceOptionsBuilder = WorkflowServiceStubsOptions.newBuilder();
        WorkflowClientOptions.Builder wfClientBuilder = WorkflowClientOptions.newBuilder();
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(wfServiceOptionsBuilder.build());
        WorkflowClient client = WorkflowClient.newInstance(service, wfClientBuilder
                .setDataConverter(new DefaultDataConverter(
                    new NullPayloadConverter(),
                    new ByteArrayPayloadConverter(),
                    new ProtobufPayloadConverter(),
                    new ProtobufJsonPayloadConverter()
                )).build());

        // Build worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        WorkflowImplementationOptions options =
                WorkflowImplementationOptions.newBuilder()
                        .setFailWorkflowExceptionTypes(NonDeterministicException.class)
                        .build();
        worker.registerWorkflowImplementationTypes(options, ProtoWorkflowImpl.class);
        factory.start();
        return client;
    }

    @Override
    public void run() {
        WorkflowClient client;
        try {
            client = runWorker();
        } catch (FileNotFoundException | SSLException e) {
            throw new RuntimeException("Failed to start worker", e);
        }

        // Set retry policy on workflow. This is usually not recommended, but we are demonstrating a special case
        // that forces workflow re-run on a non-deterministic error.
        ProtoWorkflow workflow =
                client.newWorkflowStub(
                        ProtoWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setRetryOptions(RetryOptions.newBuilder().build())
                                .setWorkflowTaskTimeout(Duration.ofSeconds(1))
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        Person person = Person.newBuilder().setEmail("tao-binary@temporal.io").build();
        AddressBook addressbook = workflow.getGreeting(person);
        System.out.println(addressbook);
        try {
            Thread.sleep(1000000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        new CommandLine(new ProtoWorkflowRunner()).execute(args);
    }
}