# My Temporal Java SDK Samples

A collection of samples demonstrating various features and patterns using the Temporal Java SDK.

## Samples

### [Activity Queue Segregation](src/main/java/io/temporal/samples/activityQueueSegregation)
Demonstrates how to execute activities on separate task queues by configuring different workers for different activity types.

### [Context Aware Codec](src/main/java/io/temporal/samples/contextAwareCodec)
Shows how to implement context-aware payload encryption where encryption keys are determined dynamically based on activity type.

### [Context Propagation](src/main/java/io/temporal/samples/contextPropagation)
Illustrates how to propagate context (like MDC) through workflow execution across multiple activities.

### [Custom Exception](src/main/java/io/temporal/samples/customException)
Demonstrates handling custom exceptions in activities and workflows, including proper exception propagation and retry configuration.

### [Grouped Activities](src/main/java/io/temporal/samples/groupedActivities)
Demonstrates patterns for organizing and executing related activities in groups, both sequentially and in parallel.

### [Interface Inheritance](src/main/java/io/temporal/samples/interfaceInheritance)
Shows how to use interface inheritance with Temporal workflows to share common workflow methods.

### [Manual Activity Retry](src/main/java/io/temporal/samples/manualActivityRetry)
Demonstrates manual activity retry using workflow signals instead of automatic retry policies.

### [Protocol Buffers](src/main/java/io/temporal/samples/protoPayload)
Demonstrates using Protocol Buffers (protobuf) with Temporal for efficient data serialization.

### [Retry NDE (Non-Deterministic Errors)](src/main/java/io/temporal/samples/retrynde)
Shows how to handle and retry workflows that encounter non-deterministic errors during execution.

### [Sequenced Update](src/main/java/io/temporal/samples/sequencedUpdate)
Demonstrates using workflow queue to sequence workflow updates, useful for handling signals in order.

### [Signal Watcher](src/main/java/io/temporal/samples/signalWatcher)
Implements a pattern for monitoring workflow signals with timeout detection and metrics tracking.

### [Timeout Handler](src/main/java/io/temporal/samples/timeoutHandler)
Shows how to implement custom timeout handling for long-running activities within workflows.

### [Worker Tuning](src/main/java/io/temporal/samples/workerTuning)
Provides examples of worker tuning techniques for different resource contention scenarios, with Prometheus metrics integration.

## Building and Running

Each sample can be run using Gradle:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.<package>.Starter
```

Replace `<package>` with the sample package name you want to run.

## Prerequisites

- Java 8 or later
- Temporal server running locally or a Temporal Cloud account
- Gradle

For samples using metrics (like Worker Tuning):
- Prometheus
- Grafana
