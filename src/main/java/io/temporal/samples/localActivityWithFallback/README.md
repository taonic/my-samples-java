# Local Activity with Fallback

Demonstrates local activity fallback pattern with retry behavior in Temporal workflows.

![Local Activity Fallback Pattern](/src/main/java/io/temporal/samples/localActivityWithFallback/local-activity-fallback.png)

## Overview

This sample shows how to implement a resilient pattern where workflows first attempt to execute activities as local activities (for performance), but automatically fall back to normal activities when local activities fail.

## Execution Flow

### Default Behavior (Local Activities Fail)
1. Local activities fail immediately
2. Temporal retries local activities up to 3 times (100ms intervals)
3. After retries are exhausted, workflow catches the exception
4. Workflow falls back to executing the same operation as normal activities
5. Normal activities succeed

### With `--succeed-local` Flag
1. Local activities succeed immediately
2. No fallback needed

## Key Features

- **Retry Mechanism**: Local activities configured with RetryOptions (max 3 attempts, 100ms interval)
- **Fallback Pattern**: Automatic fallback from local to normal activities on failure
- **Activity Context**: Uses `Activity.getExecutionContext().getInfo()` to detect activity type and attempt number
- **Configurable Behavior**: Command line flag to control success/failure scenarios

## Running the Sample

**Default (local activities fail, demonstrate fallback):**
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.localActivityWithFallback.Starter
```

**Local activities succeed (no fallback):**
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.localActivityWithFallback.Starter -Pargs="--succeed-local"
```

## Expected Output

### Default Run
```
getGreeting attempt: 1
getGreeting attempt: 2  
getGreeting attempt: 3
getGreeting attempt: 1
formatMessage attempt: 1
formatMessage attempt: 2
formatMessage attempt: 3
formatMessage attempt: 1
Hello, World!
```

### With --succeed-local
```
getGreeting attempt: 1
formatMessage attempt: 1
Hello, World!
```

## Use Case

This pattern is useful for optimizing performance (local activities are faster) while maintaining reliability through fallback to normal activities when local execution is problematic.

## Reference

For more details about local vs normal activities: https://community.temporal.io/t/local-activity-vs-activity/290/3