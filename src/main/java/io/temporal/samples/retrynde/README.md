# Retryable Non-deterministic Workflow

For some idempotent Workflows, instead of dealing with determinism, one may want the entire workflow to retry from
scratch when a code change introduces an non-deterministic error during Workflow replay.

This sample fabricates a NDE while leveraging [setFailWorkflowExceptionTypes](https://javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/worker/WorkflowImplementationOptions.Builder.html#setFailWorkflowExceptionTypes(java.lang.Class...))
and `NonDeterministicException.class` to trigger a Workflow execution failure. It also set the default retry policy to drive the desired retry behaviour.

To run the sample locally:
```
./gradlew -q execute -PmainClass=io.temporal.samples.retrynde.TimeoutHandler
```

Or against Temporal Cloud:
```
./gradlew -q execute -PmainClass=io.temporal.samples.retrynde.TimeoutHandler --args=" \
--client-cert-path=<path-to-cert> --client-key-path=<path-to-key> \
--temporal-endpoint=<namespace>.<account>.tmprl.cloud:7233 --temporal-namespace=<namespace>.<account>"
```

1. Wait for the message - "You have 10 seconds to restart the worker to trigger an NDE" then rerun the sample.

1. Once the Worker is restarted, you should see "Forcing an NDE to fail the workflow".

1. On the UI, you should see a failed Workflow with ID: `RetryNDEWorkflow` and a newly retried one in the running state.