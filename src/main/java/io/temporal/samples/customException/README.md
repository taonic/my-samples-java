# Custom Exception Handling

Demonstrates how to handle custom exceptions in Temporal workflows and activities, including proper exception propagation and non-retryable failures.

## Key Features

- **Custom Exception Types**: Defines domain-specific exceptions for activities and workflows
- **Non-Retryable Failures**: Shows how to mark exceptions as non-retryable using `ApplicationFailure`
- **Exception Transformation**: Demonstrates converting activity exceptions to workflow-specific exceptions
- **Retry Policy Configuration**: Configures retry options to exclude specific exception types

## How It Works

1. **Activity Exception**: `ItemActivityImpl.findItem()` throws a non-retryable `ApplicationFailure` when item is not found
2. **Exception Handling**: Workflow catches the activity failure and transforms it to a workflow-specific exception
3. **Client Handling**: Client code demonstrates how to handle the final workflow exception

## Running the Sample

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.customException.CustomExceptionSample
```

The sample will attempt to find a "missing" item, demonstrating the exception flow from activity to workflow to client.