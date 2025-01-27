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
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.samples.workerTuning.MetricsUtils;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.*;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import static picocli.CommandLine.*;
import java.time.Duration;

/**
 * Sample Temporal Workflow Definition that demonstrates retrying Workflow on non-deterministic errors.
 */
@Command(name = "retry_nde_example", description = "")
public class RetryNDE implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(io.temporal.samples.retrynde.RetryNDE.class);

    private static HttpServer scrapeEndpoint;

    // Define the task queue name
    static final String TASK_QUEUE = "RetryNDETaskQueue";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "RetryNDEWorkflow";

    @CommandLine.Option(names = "--temporal-namespace", description = "The Temporal namespace to connect to")
    static String temporal_namespace;

    @CommandLine.Option(names = "--temporal-endpoint", description = "The Temporal endpoint to connect to")
    static String temporal_endpoint;

    @CommandLine.Option(names = "--client-key-path", description = "The mTLS client key for authenticating to the namespace")
    static String client_key_path;

    @CommandLine.Option(names = "--client-cert-path", description = "The mTLS client certificate for authenticating to the namespace")
    static String client_cert_path;

    @CommandLine.Option(names = "--metric-port", description = "The port used to expose OpenMetrics", defaultValue = "8077")
    static int metric_port;

    @WorkflowInterface
    public interface RetryNDEWorkflow {
        @WorkflowMethod
        String getGreeting();

        @SignalMethod
        void can();
    }

    public static class RetryNDEWorkflowImpl implements RetryNDEWorkflow {
        @Override
        public String getGreeting() {
            try {
                if (WorkflowUnsafe.isReplaying()) {
                    // Create a side effect only during replay to induce an non-deterministic error.
                    System.out.println("Forcing an NDE to fail the workflow");
                    Workflow.sideEffect(String.class, () -> "I'm a side effect to cause an NDE");
                } else {
                    if (Workflow.getInfo().getAttempt() == 1) {
                        System.out.println("You have 10 seconds to restart the worker to trigger an NDE");
                    } else {
                        System.out.println("This is a retried workflow. Wait for 10 seconds until it completes.");
                    }
                }

                // Give enough time to restart the worker
                Workflow.sleep(10000);
                return "done";
            } catch (NonDeterministicException err) {
                System.out.println("caught NDE");
                System.out.println(err);
                return err.toString();
            }
        }

        @Override
        public void can() {
            RetryNDEWorkflow continueAsNew = Workflow.newContinueAsNewStub(RetryNDEWorkflow.class);
            continueAsNew.getGreeting();
        }
    }

    private static WorkflowClient runWorker() throws FileNotFoundException, SSLException {
        // Prepare Prometheus endpoint
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        Scope scope =
                new RootScopeBuilder()
                        .reporter(new MicrometerClientStatsReporter(registry))
                        .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
        scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, metric_port);
        log.info("Starting Prometheus scrape endpoint at: http://localhost:" + scrapeEndpoint.getAddress().getPort() + "/metrics");

        // Build workflow client
        WorkflowServiceStubsOptions.Builder wfServiceOptionsBuilder = WorkflowServiceStubsOptions.newBuilder()
                .setMetricsScope(scope);
        WorkflowClientOptions.Builder wfClientBuilder = WorkflowClientOptions.newBuilder();
        if (temporal_namespace != null) {
            InputStream clientCert = new FileInputStream(client_cert_path);
            InputStream clientKey = new FileInputStream(client_key_path);
            wfServiceOptionsBuilder
                    .setSslContext(SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build())
                    .setTarget(temporal_endpoint);
            wfClientBuilder.setNamespace(temporal_namespace);
        }
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(wfServiceOptionsBuilder.build());
        WorkflowClient client = WorkflowClient.newInstance(service, wfClientBuilder.build());

        // Build worker
        WorkerFactory factory = WorkerFactory.newInstance(client);
        Worker worker = factory.newWorker(TASK_QUEUE);
        WorkflowImplementationOptions options =
                WorkflowImplementationOptions.newBuilder()
                        //.setFailWorkflowExceptionTypes(NonDeterministicException.class)
                        .build();
        worker.registerWorkflowImplementationTypes(options, io.temporal.samples.retrynde.RetryNDE.RetryNDEWorkflowImpl.class);
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
        RetryNDEWorkflow workflow =
                client.newWorkflowStub(
                        RetryNDEWorkflow.class,
                        WorkflowOptions.newBuilder()
                                //.setRetryOptions(RetryOptions.newBuilder().build())
                                .setWorkflowTaskTimeout(Duration.ofSeconds(1))
                                .setWorkflowId(WORKFLOW_ID)
                                .setTaskQueue(TASK_QUEUE)
                                .build());

        String output = workflow.getGreeting();

        try {
            // leave some time for inspecting metrics
            Thread.sleep(Duration.ofSeconds(60).toMillis());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(output);
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new io.temporal.samples.retrynde.RetryNDE()).execute(args);
    }
}
