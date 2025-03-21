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

package io.temporal.samples.workerTuning;

import com.sun.net.httpserver.HttpServer;
import com.uber.m3.tally.RootScopeBuilder;
import com.uber.m3.tally.Scope;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.temporal.activity.*;
import io.temporal.client.ActivityCompletionException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.common.converter.CodecDataConverter;
import io.temporal.common.converter.DefaultDataConverter;
import io.temporal.common.reporter.MicrometerClientStatsReporter;
import io.temporal.failure.ActivityFailure;
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
import picocli.CommandLine;

import javax.net.ssl.SSLException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import static picocli.CommandLine.*;

/**
 * This example demonstrates a number of worker tuning techniques based a range of resource contention scenarios
 */
@Command(name = "worker_tuning_example", description = "Runs Temporal Worker tuning example")
public class Starter implements Runnable {
    static final boolean useCloud = true;
    private static final Logger log =
            LoggerFactory.getLogger(Starter.class);

    private static HttpServer scrapeEndpoint;

    static String task_queue;

    // Define the workflow unique id
    static final String WORKFLOW_ID = "WorkerTuningWorkflow";

    static final Duration graphPadding = Duration.ofSeconds(5);

    @Option(names = "--activity-pollers", description = "The number of Activity pollers", defaultValue = "5")
    static int activityPollers;

    @Option(names = "--activity-slots", description = "The number of Activity execution slots", defaultValue = "200")
    static int activityExecSlots;

    @Option(names = "--workflows", description = "The number of concurrent Workflows", defaultValue = "100")
    static int workflows;

    @Option(names = "--activities-per-workflow", description = "The number of Activities per Workflow", defaultValue = "10")
    static int activitiesPerWF;
    @Option(names = "--temporal-namespace", description = "The Temporal namespace to connect to")
    static String temporal_namespace;

    @Option(names = "--temporal-endpoint", description = "The Temporal endpoint to connect to")
    static String temporal_endpoint;

    @Option(names = "--client-key-path", description = "The mTLS client key for authenticating to the namespace")
    static String client_key_path;

    @Option(names = "--client-cert-path", description = "The mTLS client certificate for authenticating to the namespace")
    static String client_cert_path;

    @Option(names = "--failure-ratio", description = "The ratio for forced Workflow Task failures. Value between 0 - 100", defaultValue = "0")
    static int failureRatio;

    @ActivityInterface
    public interface SlowActivities {
        @ActivityMethod(name = "slowActivity")
        byte[] slowActivity(double seconds);

        @ActivityMethod(name = "CPUIntensiveActivity")
        byte[] CPUIntensiveActivity();

        @ActivityMethod(name = "largeActivity")
        byte[] largeActivity();
    }

    public static class SlowActivitiesImpl implements SlowActivities {
        private static final Logger log =
                LoggerFactory.getLogger(SlowActivitiesImpl.class);

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
        public byte[] CPUIntensiveActivity() {
            long start = System.currentTimeMillis();
            fibRecursion(52); // takes about 2-3s
            log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
            return new byte[1];
        }

        @Override
        public byte[] slowActivity(double seconds) {
            ActivityExecutionContext context = Activity.getExecutionContext();
            long start = System.currentTimeMillis();
            try {
                Thread.sleep((long) (1000 * seconds));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
            return new byte[1];
        }

        @Override
        public byte[] largeActivity() {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //log.debug("Time elapsed: {}", System.currentTimeMillis() - start);
            return new byte[10 * 1024]; // 10KiB
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
                        ActivityOptions.newBuilder()
                                .setHeartbeatTimeout(Duration.ofSeconds(2))
                                .setCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
                                .setStartToCloseTimeout(Duration.ofSeconds(30))
                                .build());

        private final SlowActivities localActivities =
                Workflow.newLocalActivityStub(
                        SlowActivities.class,
                        LocalActivityOptions.newBuilder()
                                .setStartToCloseTimeout(Duration.ofSeconds(30))
                                .build());

        @Override
        public void doWork(int concurrency) {
            List<Promise<byte[]>> promisesActivities = new ArrayList<>();
            List<Promise<byte[]>> promisesLocalActivities = new ArrayList<>();
            Random random = new Random();
            long randomness = random.nextInt(100);
            if (randomness > 100 - failureRatio) {
                throw new RuntimeException("Simulate failed Workflow Tasks");
            }
            for (int i = 0; i < concurrency; i++) {
                promisesActivities.add(Async.function(activities::slowActivity, 2.0));
            }
            for (Promise<byte[]> promise : promisesActivities) {
                promise.get();
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
        wfClientBuilder.setDataConverter(
                        new CodecDataConverter(
                                DefaultDataConverter.newDefaultInstance(),
                                Collections.singletonList(new CryptCodec())));
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
        WorkerFactory factory = WorkerFactory.newInstance(client, factoryOptions);
        Worker worker = factory.newWorker(task_queue, workerOptions);
        worker.registerWorkflowImplementationTypes(WorkerTuningWorkflowImpl.class);
        worker.registerActivitiesImplementations(new SlowActivitiesImpl());
        factory.start();
        return client;
    }

    private static void runConcurrentWorkflow(WorkflowClient client, int workflows, int activitiesPerWF) throws Exception {
        log.info("Running {} workflows concurrently with {} activities each", workflows, activitiesPerWF);
        ArrayList<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < workflows; i++) {
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

    @Override
    public void run() {
        task_queue = String.format("Pollers%d/Slots%d", activityPollers, activityExecSlots);
        WorkerFactoryOptions factoryOptions = WorkerFactoryOptions.newBuilder()
                .setWorkflowCacheSize(1)
                .build();
        WorkerOptions workerOptions = WorkerOptions.newBuilder()
                .setMaxConcurrentActivityTaskPollers(activityPollers) // default 5
                .setMaxConcurrentActivityExecutionSize(activityExecSlots) // default 200
                .build();
        log.info("Worker options: {}", workerOptions);
        try {
            WorkflowClient client = runWorker(factoryOptions, workerOptions);
            Thread.sleep(graphPadding.toMillis()); // to show 10s inactivity to pad the graph
            for(int i=0; i< 50;i++) {
                runConcurrentWorkflow(client, workflows, activitiesPerWF);
            }
            log.info("Completed");
            Thread.sleep(graphPadding.toMillis());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        scrapeEndpoint.stop(0);
        System.exit(0);
    }

    public static void main(String[] args) {
        new CommandLine(new Starter()).execute(args);
    }
}