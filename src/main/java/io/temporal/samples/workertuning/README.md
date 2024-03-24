# Worker Tuning

## Prerequisites
* Install Grafana: https://grafana.com/docs/grafana/latest/setup-grafana/installation/
* Install Prometheus: https://prometheus.io/docs/prometheus/latest/installation/
* Update Grafana's config (grafana.ini > [dashboards]) to include `min_refresh_interval = 200ms`
* Update Promethue's config according to this [sample](/src/main/java/io/temporal/samples/workertuning/config/prometheus.yml)
  * Make sure `scrape_interval` and `evaluation_interval` are both set at 1s
* Import the [sample SDK Metrics dashboard](/src/main/java/io/temporal/samples/workertuning/dashboard/sdk_metrics.yaml) to Grafana.

## Example setup

The sample code runs 50 Workflows asynchronously with each schedules 50 Activies in parallel. This configuration produces 2500 Activities simultaneously that would typically overwhelm Workers running on default settings.

We step through a number of Worker setting combinations that update `MaxConcurrentActivityTaskExecutionSize` and `MaxConcurrentActivityTaskPollers` one at a time while observing the outcome.

After tuning, the Workflow's end-to-end execution latency is decreased from 28.6s to 1.79s.

## Run example
```
export TEMPORAL_CLIENT_CERT="<path_to_client_cert>"
export TEMPORAL_CLIENT_KEY="<path_to_client_key>"
export TEMPORAL_ENDPOINT="<cloud_host_and_port>"
export TEMPORAL_NAMESPACE="<temporal_namespace>"
```
```
./gradlew -PmainClass=io.temporal.samples.workertuning.Starter run
```

## Tunning steps

### Run #1 (default settings)
- 5 Activity Pollers
- 200 Activity Execution Slots

#### Analysis
- The 28s "Activity Schedule To Start Latency" serves as a clear indication that the worker is overwhelmed by the volume of the Activity tasks.
- Insufficient "Activity Slots Available" is evident.
- Next step: Increase `setMaxConcurrentActivityExecutionSize`

![](/src/main/java/io/temporal/samples/workertuning/assets/5x200.png)

### Run #2
- 5 Activity Pollers
- 800 Activity Execution Slots

#### Analysis
- The "Activity Schedule To Start Latency" has improved to 22s but remains high.
- Plenty of Activity Slots are Available (>600) through out the test.
- Based on the 35ms gRPC request latency from the "Long Poll Latency" graph (not included in screenshot), max throughput for 5 poller is 140 RPS (`(1000ms/35ms) * 5`). We can tell from the "Polled Activity Tasks Per Second" graph that we are maxing out the poller capacity.
- Next step: Increase `MaxConcurrentActivityTaskPollers`

![](/src/main/java/io/temporal/samples/workertuning/assets/5x800.png)

### Run #3
- 80 Activity Pollers
- 800 Activity Execution Slots

#### Analysis
- The "Activity Schedule To Start Latency" has improved to 1.7s
- However, the "Activity Slots Available" dropped to 0 once again
- Next step: Further increase `setMaxConcurrentActivityExecutionSize`

![](/src/main/java/io/temporal/samples/workertuning/assets/80x800.png)


### Run #4
- 80 Activity Pollers
- 1600 Activity Execution Slots

#### Analysis
- The "Activity Schedule To Start Latency" has improved to 250ms
- Overall Workflow end-to-end execution latency dropped from 28s to 1.79s
- CPU consumption remains minimal

![](/src/main/java/io/temporal/samples/workertuning/assets/80x1600.png)
