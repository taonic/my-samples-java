# Simple Approve Signal Demo

A Temporal workflow implementation demonstrating basic signal handling for approval processes. The workflow waits for an approval signal before proceeding with notification.

## Components

### ApprovalWorkflow
A workflow that:
- Waits for an approval signal using `Workflow.await()`
- Sends email notification once approved
- Demonstrates blocking workflow execution until signal received

### EmailActivities
Activity interface for sending notifications with a simple console output implementation.

## Usage

Run the sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.simpleApproveSignal.ApproveSignal
```

To send approval signal, use the Temporal CLI or create a separate signaler:
```bash
temporal workflow signal --workflow-id HelloSignalWorkflow --name approve
```

The workflow will:
1. Start and wait for approval signal
2. Block execution until `approve()` signal is received
3. Send email notification: "Your request is approved"
4. Complete workflow execution

## Technical Notes
- Task queue: "HelloSignalTaskQueue"
- Workflow ID: "HelloSignalWorkflow"
- Uses `Workflow.await()` to block until signal received
- Activity timeout: 2 seconds