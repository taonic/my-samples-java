# Signal Watcher Demo

A Temporal workflow implementation demonstrating signal handling with timeout monitoring. The demo consists of two components: a Runner and a Signaler.

## Components

### Runner
Starts a workflow that:
- Monitors for signals every 3 seconds
- Tracks timeouts using metrics
- Stops when it receives a resolution signal

### Signaler
A separate process that sends a resolution signal to a running workflow.

## Usage

1. Start the workflow:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.signalWatcher.Runner
```

2. In another terminal, send the signal:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.signalWatcher.Signaler
```

The workflow will:
1. Start monitoring for signals
2. Log timeout events
3. Terminate when it receives the resolution signal
4. Output "done"

## Technical Notes
- Uses Temporal's local service stubs for testing
- Task queue: "SignalWatcherTaskQueue"
- Default workflow ID: "SignalWatcherWorkflow"
- Timeout check interval: 3 seconds