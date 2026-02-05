# Custom Exception Handling

Demonstrates custom exception handling in Temporal workflows and activities using Feign HTTP client exceptions, including proper exception propagation and retry configuration.

## Key Features

- **Feign Exception Handling**: Uses FeignException.BadRequest from HTTP calls as example of external service exceptions
- **Non-Retryable Configuration**: Configures retry policy to exclude specific exception types
- **Exception Transformation**: Converting activity exceptions to workflow-specific exceptions
- **Real HTTP Integration**: Makes actual HTTP calls to httpbin.org to trigger exceptions

## How It Works

1. **HTTP Activity**: `findItem()` uses Feign client to make HTTP requests to httpbin.org
2. **Exception Trigger**: When itemId is "missing", makes HTTP call that returns 400 Bad Request
3. **Retry Policy**: Configured to not retry FeignException.BadRequest
4. **Workflow Handling**: Catches activity failure and transforms to workflow-specific exception

## Running the Sample

```bash
./gradlew -q execute -PmainClass=io.temporal.samples.customException.CustomExceptionSample
```

The sample attempts to find a "missing" item, triggering a real HTTP 400 error that demonstrates exception flow from Feign → Activity → Workflow → Client.