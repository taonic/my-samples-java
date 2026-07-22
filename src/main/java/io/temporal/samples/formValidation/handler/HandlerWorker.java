package io.temporal.samples.formValidation.handler;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

/**
 * Worker that hosts everything for the formValidation sample on a single task queue:
 *
 * <ul>
 *   <li>the {@link FormNexusServiceImpl} Nexus service (synchronous, workflow-free validation), and
 *   <li>the {@link ApplicationWorkflowImpl} plus its activities (the one Workflow started at
 *       submit).
 * </ul>
 *
 * The task queue must match the target task queue of the Nexus endpoint (see README).
 */
public class HandlerWorker {

  public static final String TASK_QUEUE_NAME = "form-validation-queue";

  public static void main(String[] args) {
    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE_NAME);
    worker.registerWorkflowImplementationTypes(ApplicationWorkflowImpl.class);
    worker.registerActivitiesImplementations(new ApplicationActivitiesImpl());
    worker.registerNexusServiceImplementation(new FormNexusServiceImpl());

    factory.start();
    System.out.println("HandlerWorker started on task queue '" + TASK_QUEUE_NAME + "'");
  }
}
