# Manual Activity Retry

Demonstrates manual activity retry using workflow signals to handle permanently failed Activities

## Pattern

Activities fail with non-retryable errors, workflow waits for retry signal, then re-executes with modified input.

## Key Features

- Signal-based retry control
- Search attributes for failed activity tracking
- Custom retry logic in workflow

## Implementation

The `executeWithManualRetry` method:
- Executes activities with invalid input initially
- Catches failures and sets search attributes for filtering failed Activities
- Waits for retry signal with corrected input

## Usage

Make sure your local Temporal server is running with the required Custom Search Attribute:

```bash
temporal server start-dev --search-attribute "FailedActivity=Keyword"
```

Run the sample:

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.manualActivityRetry.Starter
```

The newly started Workflow will fail at the first Activity. To find those Workflows, you can query by a custom Search Attribute:

```bash
temporal workflow list --query "ExecutionStatus = 'Running' AND FailedActivity IS NOT NULL"
```

Or filter by a particular Activity
```bash
temporal workflow list --query "ExecutionStatus = 'Running' AND FailedActivity = 'getGreeting'"
```

To retry failed activities, send signal with valid input from either UI or CLI, e.g.
```bash
temporal workflow signal --workflow-id <replace with Workflow ID> --name "retry" --input '"hello"'
```

This will retry and succeed the first Activity then fail the second one - `formatMessage`. You can send another Signal to retry, e.g.
```bash
temporal workflow signal --workflow-id <replace with Workflow ID> --name "retry" --input '"world"'
```

The Workflow should complete with:

```bash
Hi hello world!
```
