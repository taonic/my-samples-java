# Sequenced Update Sample

This sample demonstrates how to use Temporal workflow updates to handle ordered updates to workflow state, with support for concurrent processing and validation.

## Key Features

- Update method execution with ordered processing
- Concurrent update handling with workflow queue
- Update validation and failure handling
- Activity execution within updates
- Completion signaling using workflow signals

## Usage

Run the sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.sequencedUpdate.Starter
```

## How It Works

1. Update Queue:
```java
private final WorkflowQueue<QueuedCommand> commandQueue = 
    Workflow.newWorkflowQueue(10);
```

2. Update Method:
```java
@UpdateMethod
public String addGreeting(String name) {
    if (name.isEmpty()) {
        throw ApplicationFailure.newFailure("Cannot greet someone with an empty name", "Failure");
    }
    CompletablePromise<String> promise = Workflow.newPromise();
    commandQueue.put(new QueuedCommand(name, promise));
    return promise.get();
}
```

3. Processing Loop:
```java
while (true) {
    if (exit && commandQueue.peek() == null) {
        return receivedMessages;
    }
    QueuedCommand queuedCommand = commandQueue.take();
    String result = activities.composeGreeting("Hello", queuedCommand.value);
    receivedMessages.add(result);
    queuedCommand.promise.complete(result);
}
```

## Requirements

- Temporal server with updates feature enabled
  - Set `frontend.enableUpdateWorkflowExecution=true` in your Temporal config
- Temporal Java SDK with updates support
