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

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.samples.workertuning.MetricsUtils;
import io.temporal.samples.workertuning.Starter;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.*;
import io.temporal.workflow.*;
import io.temporal.workflow.Functions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import static picocli.CommandLine.*;
import java.time.Duration;

@Command(name = "timeout_handler_example", description = "")
public class TimeoutHandler implements Runnable {

    private static final Logger log =
            LoggerFactory.getLogger(TimeoutHandler.class);

    private static HttpServer scrapeEndpoint;

    // Define the task queue name
    static final String TASK_QUEUE = "TimeoutHandlerTaskQueue";

    // Define the workflow unique id
    static final String WORKFLOW_ID = "TimeoutHandlerWorkflow";

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
            for (int i = 0; i< 5; i++) {
                activities.slowActivity(2);
            }
            return "done";
        }

        private Func<Void> timeoutWatcher(Duration duration) {
            Workflow.sleep(duration);
            logger.info("Timeout detected after " + duration);
            Workflow.getMetricsScope().counter("timeout_detected").inc(1);
            return timeoutWatcher(duration);
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
                        .setFailWorkflowExceptionTypes(NonDeterministicException.class)
                        .build();
        worker.registerWorkflowImplementationTypes(options, TimeoutHandler.TimeoutHandlerWorkflowImpl.class);
        worker.registerActivitiesImplementations(new Starter.SlowActivitiesImpl());
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

        TimeoutHandlerWorkflow workflow =
                client.newWorkflowStub(
                        TimeoutHandlerWorkflow.class,
                        WorkflowOptions.newBuilder()
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
        new CommandLine(new TimeoutHandler()).execute(args);
    }
}
