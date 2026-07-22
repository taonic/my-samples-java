# Update-with-Start (with a rejected validator)

This sample demonstrates [Update-with-Start](https://docs.temporal.io/sending-messages#update-with-start)
and, in particular, what happens when the Update is **rejected by its validator**.

Update-with-Start sends an Update request and starts the Workflow in the same call, if the Workflow
isn't already running. A `WorkflowIdConflictPolicy` is required. It's a great fit for latency
sensitive, lazy-initialization use cases such as a shopping cart, where the client wants to add an
item without first knowing whether the cart (the Workflow) exists.

## The key takeaway

**Update-with-Start is _not_ atomic.** Unlike Signal-with-Start, if the Update is rejected by its
validator, the Workflow is **still started**. The client receives a `WorkflowUpdateException`, but a
new, running Workflow Execution is left behind.

A rejected Update leaves no trace in Workflow history (no `WorkflowExecutionUpdateAccepted` event),
exactly like a rejected Query — so the started Workflow's state is untouched by the rejected request.

## What the sample does

1. **Scenario 1 — rejected Update.** Calls `executeUpdateWithStart` with an invalid quantity (`0`).
   The `validateAddItem` validator throws, so the Update is rejected and the client gets a
   `WorkflowUpdateException`. The sample then Queries the Workflow to prove it started anyway, with
   an empty cart.
2. **Scenario 2 — valid Update.** Calls `executeUpdateWithStart` again with a valid quantity. Because
   the Workflow from scenario 1 is already running and the conflict policy is `USE_EXISTING`, this
   attaches to the existing Workflow, and the Update is accepted and applied.

## Update handler and validator

```java
@UpdateMethod
int addItem(String item, int quantity);

// A validator must return void, take the same arguments as the Update,
// must not mutate Workflow state, and must not block. Throwing any exception rejects the Update.
@UpdateValidatorMethod(updateName = "addItem")
void validateAddItem(String item, int quantity);
```

## Sending the Update-with-Start

```java
WithStartWorkflowOperation<List<String>> startOp =
    new WithStartWorkflowOperation<>(workflow::shop);

int cartSize =
    WorkflowClient.executeUpdateWithStart(
        workflow::addItem,
        "banana",
        3,
        UpdateOptions.<Integer>newBuilder()
            .setWaitForStage(WorkflowUpdateStage.COMPLETED)
            .build(),
        startOp);
```

The `WorkflowIdConflictPolicy` is set on the Workflow stub's options:

```java
WorkflowOptions.newBuilder()
    .setTaskQueue(TASK_QUEUE)
    .setWorkflowId(WORKFLOW_ID)
    .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
    .build();
```

## Usage

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.updateWithStart.Starter
```

## Requirements

- Temporal server with the update feature enabled
  - Set `frontend.enableUpdateWorkflowExecution=true` in your Temporal config
- Temporal Server 1.28+ is recommended for Update-with-Start
