Here's a concise README description:

# Grouped Activities Sample

This sample demonstrates how to execute grouped activities in parallel using Temporal workflows. It showcases a pattern for organizing and running related activities together while maximizing concurrency.

The workflow executes two activity groups in parallel:
- Group 1: Sequential activities that chain greetings together
- Group 2: Parallel activities that compose multiple greetings simultaneously

Each group processes its activities differently (sequential vs parallel), but both groups themselves run concurrently, showing how to mix different execution patterns in a single workflow.

Key concepts demonstrated:
- Parallel execution using `Async.function()`
- Promise-based activity coordination
- Error handling for concurrent activities
- Activity timeout configuration
- Mixed sequential and parallel execution patterns

To run the sample:
```bash
./gradlew -q execute -PmainClass=io.temporal.samples.groupedActivities.GroupedActivities
```

This is a simplified example using greeting composition, but the pattern can be applied to any business scenario requiring grouped activity execution.