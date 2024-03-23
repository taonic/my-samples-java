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

package io.temporal.samples.workertuning;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.serviceclient.SimpleSslContextBuilder;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.*;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

/**
 * This example demonstrates a number of worker tuning techniques based a range of resource contention scenarios
 */
public class Starter {
    static final boolean useCloud = true;
    private static final Logger log =
            LoggerFactory.getLogger(Starter.class);

    private static HttpServer scrapeEndpoint;

    static String task_queue;

    // Define the workflow unique id
    static final String WORKFLOW_ID = "WorkerTuningWorkflow";

    static final Duration graphPadding = Duration.ofSeconds(5);

    @ActivityInterface
    public interface SlowActivities {
        @ActivityMethod(name = "slowActivity")
        byte[] slowActivity(int seconds);

        @ActivityMethod(name= "CPUIntensiveActivity")
        void CPUIntensiveActivity();

        @ActivityMethod(name= "largeActivity")
        byte[] largeActivity();
    }

    static class DifficultActivitiesImpl implements SlowActivities {
        private static final Logger log =
                LoggerFactory.getLogger(DifficultActivitiesImpl.class);

        private int fibRecursion(int count) {
            if (count == 0) {
                return 0;
            } else if (count == 1 || count == 2) {
                return 1;
            } else {
                return fibRecursion(count - 1) + fibRecursion(count - 2);
            }
        }
        @Override
        public void CPUIntensiveActivity() {
            long start = System.currentTimeMillis();
            fibRecursion(42); // takes about 2-3s
            log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
        }
        @Override
        public byte[] slowActivity(int seconds) {
            long start = System.currentTimeMillis();
            try {
                Random random = new Random();
                Thread.sleep(seconds * 1000 + random.nextInt(100));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
            return new byte[1024];
        }

        @Override
        public byte[] largeActivity() {
            long start = System.currentTimeMillis();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
            return new byte[10*1024]; // 10KiB
        }
    }

    @WorkflowInterface
    public interface WorkerTuningWorkflow {
        @WorkflowMethod
        void doWork(int concurrency);
    }

    // Define the workflow implementation which implements the getGreetings workflow method.
    public static class WorkerTuningWorkflowImpl implements WorkerTuningWorkflow {
        private final SlowActivities activities =
                Workflow.newActivityStub(
                        SlowActivities.class,
                        ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

        @Override
        public void doWork(int concurrency) {
            List<Promise<byte[]>> promises = new ArrayList<>();
            for (int i=0; i<concurrency;i++) {
                promises.add(Async.function(activities::slowActivity, 1));
                //promises.add(Async.procedure(activities::CPUIntensiveActivity));
                //promises.add(Async.function(activities::largeActivity));
            }
            for (Promise<byte[]> promise : promises) {
                String base64EncodedString = Base64.getEncoder().encodeToString(promise.get());
                log.debug(base64EncodedString);
            }
        }
    }

    private static WorkflowClient runWorker(WorkerFactoryOptions factoryOptions, WorkerOptions workerOptions) throws FileNotFoundException, SSLException {
        // Prepare Prometheus endpoint
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new JvmThreadMetrics().bindTo(registry);
        new JvmMemoryMetrics().bindTo(registry);
        new ProcessorMetrics().bindTo(registry);
        Scope scope =
                new RootScopeBuilder()
                        .reporter(new MicrometerClientStatsReporter(registry))
                        .reportEvery(com.uber.m3.util.Duration.ofSeconds(1));
        scrapeEndpoint = MetricsUtils.startPrometheusScrapeEndpoint(registry, 8077);

        // Build workflow client
        WorkflowServiceStubsOptions.Builder wfServiceOptionsBuilder = WorkflowServiceStubsOptions.newBuilder()
                .setMetricsScope(scope);
        WorkflowClientOptions.Builder wfClientBuilder = WorkflowClientOptions.newBuilder();
        if (useCloud) {
            InputStream clientCert = new FileInputStream(System.getenv("TEMPORAL_CLIENT_CERT"));
            InputStream clientKey = new FileInputStream(System.getenv("TEMPORAL_CLIENT_KEY"));
            String targetEndpoint = System.getenv("TEMPORAL_ENDPOINT");
            wfServiceOptionsBuilder
                    .setSslContext(SimpleSslContextBuilder.forPKCS8(clientCert, clientKey).build())
                    .setTarget(targetEndpoint);
            wfClientBuilder.setNamespace(System.getenv("TEMPORAL_NAMESPACE"));
        }
        WorkflowServiceStubs service = WorkflowServiceStubs.newServiceStubs(wfServiceOptionsBuilder.build());
        WorkflowClient client = WorkflowClient.newInstance(service, wfClientBuilder.build());

        // Build worker
        WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
        Worker worker = factory.newWorker(task_queue, workerOptions);
        worker.registerWorkflowImplementationTypes(WorkerTuningWorkflowImpl.class);
        worker.registerActivitiesImplementations(new DifficultActivitiesImpl());
        factory.start();
        return client;
    }

    private static void runConcurrentWorkflow(WorkflowClient client, int workflows, int activitiesPerWF) throws Exception {
        log.info("Running {} workflows concurrently with {} activities each", workflows, activitiesPerWF);
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < workflows; i++ ) {
            WorkflowOptions workflowOptions =
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(task_queue)
                            .setWorkflowId(WORKFLOW_ID + RandomStringUtils.randomAlphanumeric(6).toUpperCase())
                            .build();
            WorkerTuningWorkflow workflow = client.newWorkflowStub(WorkerTuningWorkflow.class, workflowOptions);
            futures.add(WorkflowClient.execute(workflow::doWork, activitiesPerWF));
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public static void main(String[] args) throws Exception {
        int[][] combinations = {
                // { activity pollers, activity exec slots }
                {5, 200},
                {5, 800},
                {80, 800},
                {80, 1600},
        };
        for (int i = 0; i < combinations.length; i++) {
            int activityPollers = combinations[i][0];
            int activityExecSlots = combinations[i][1];
            task_queue = String.format("%d/Pollers%d/Slots%d", i, activityPollers, activityExecSlots);
            WorkerFactoryOptions factoryOptions = WorkerFactoryOptions.newBuilder()
                    .build();
            WorkerOptions workerOptions = WorkerOptions.newBuilder()
                    .setMaxConcurrentActivityTaskPollers(activityPollers) // default 5
                    .setMaxConcurrentActivityExecutionSize(activityExecSlots) // default 200
                    .build();
            log.info("Worker options: {}", workerOptions);
            WorkflowClient client = runWorker(factoryOptions, workerOptions);
            Thread.sleep(graphPadding.toMillis()); // to show 10s inactivity to pad the graph
            runConcurrentWorkflow(client, 50, 50);
            log.info("Completed");
            Thread.sleep(graphPadding.toMillis());
            scrapeEndpoint.stop(0);
        }
        System.exit(0);
   }
}