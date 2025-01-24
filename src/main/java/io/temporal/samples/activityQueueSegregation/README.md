## Temporal Activity Queue Segregation Sample

This repository demonstrates how to use Temporal Workflow and Activity to execute activities on separate task queues. The sample includes the implementation of a simple workflow that calls two activities, each assigned to distinct task queues, to showcase queue-based segregation of tasks.

### How It Works
1.	Workflow:
- The workflow orchestrates two activities:
  - composeGreeting: Generates a greeting message.
  - intro: Generates an introduction message.
- Both activities are invoked via activity stubs tied to specific task queues.
2.	Activities:
- Activities contain the actual logic and can perform long-running or non-deterministic tasks.
- Each activity is assigned to its own task queue for isolated processing.
3.	Worker Setup:
- A worker handles workflow tasks on the main task queue (HelloActivityTaskQueue).
- Two additional workers handle activity tasks on separate task queues (HelloActivityTaskQueueActivity1 and HelloActivityTaskQueueActivity2).

### Getting Started

#### Prerequisites
- Java 8 or later
- Temporal Java SDK
- Temporal Dev Server

#### Steps to Run

Start a dev server:
```
temporal server start-dev
```

Run the sample:
```
./gradlew -q execute -PmainClass=io.temporal.samples.activityQueueSegregation.Starter
```

The application will execute the workflow and display the final greeting message in the console.

#### Example Output
```
Composing greeting...
Intro...
Hello World! I'm Temporal.
```