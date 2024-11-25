# Timeout Handler Sample

This sample demonstrates how to implement a custom timeout handler in Temporal workflows to monitor and respond to long-running activities.

## Key Features

- Implements a recurring timeout watcher that runs in parallel with main workflow activities
- Monitors slow activities using a configurable duration (3 seconds in this example)
- Logs timeout events and increments metrics counter when timeouts are detected
- Executes 5 slow activities sequentially (each taking 2 seconds)

## Implementation Details

The workflow uses Temporal's `Async.function()` to run a timeout watcher concurrently with the main activities. When a timeout is detected:
- Logs the timeout event
- Increments a metrics counter
- Recursively starts the next timeout watcher

## Usage

To run the sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.timeoutHandler.TimeoutHandler  
```

This pattern is useful for:
- Monitoring long-running activities
- Implementing custom timeout handling logic
- Collecting metrics on activity execution times
- Detecting potential performance issues in workflows

Note: The actual activities in this sample are simulated slow operations, but the pattern can be applied to any scenario requiring timeout monitoring.