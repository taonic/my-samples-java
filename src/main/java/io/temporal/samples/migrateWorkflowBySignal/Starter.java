package io.temporal.samples.migrateWorkflowBySignal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.api.workflowservice.v1.RegisterNamespaceRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Duration;

public class Starter {
    
    public static final String TASK_QUEUE = "approval-task-queue";
    public static final String WORKFLOW_ID = "approval-workflow-" + System.currentTimeMillis();
    public static final String NEW_NAMESPACE = "approval-namespace";

    public static void main(String[] args) throws Exception {
        WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
        
        // Ensure the new namespace exists
        try {
            service.blockingStub().registerNamespace(
                RegisterNamespaceRequest.newBuilder()
                    .setNamespace(NEW_NAMESPACE)
                    .setWorkflowExecutionRetentionPeriod(
                        Duration.newBuilder().setSeconds(86400).build() // 1 day retention
                    )
                    .build()
            );
            System.out.println("Created namespace: " + NEW_NAMESPACE);
        } catch (Exception e) {
            // Namespace might already exist, which is fine
            System.out.println("Namespace " + NEW_NAMESPACE + " already exists or creation failed: " + e.getMessage());
        }
        
        WorkflowClient client = WorkflowClient.newInstance(service);
        WorkerFactory factory = WorkerFactory.newInstance(client);

        // Worker for default namespace
        Worker worker = factory.newWorker(TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
        worker.registerActivitiesImplementations(new ApprovalActivitiesImpl());

        // Worker for new namespace
        WorkflowClient newNamespaceClient = WorkflowClient.newInstance(service, 
            WorkflowClientOptions.newBuilder().setNamespace(NEW_NAMESPACE).build());
        WorkerFactory newNamespaceFactory = WorkerFactory.newInstance(newNamespaceClient);
        Worker newNamespaceWorker = newNamespaceFactory.newWorker(TASK_QUEUE);
        newNamespaceWorker.registerWorkflowImplementationTypes(ApprovalWorkflowImpl.class);
        newNamespaceWorker.registerActivitiesImplementations(new ApprovalActivitiesImpl());

        factory.start();
        System.out.println("Started worker factory for default namespace");
        newNamespaceFactory.start();
        System.out.println("Started worker factory for new namespace: " + NEW_NAMESPACE);

        ApprovalWorkflow workflow = client.newWorkflowStub(
            ApprovalWorkflow.class,
            WorkflowOptions.newBuilder()
                .setWorkflowId(WORKFLOW_ID)
                .setTaskQueue(TASK_QUEUE)
                .build()
        );

        ApprovalTypes.WorkflowState initialState = new ApprovalTypes.WorkflowState();
        initialState.setRequiredApprovals(2);
        
        // Start workflow asynchronously
        WorkflowClient.start(workflow::execute, initialState);
        System.out.println("Started approval workflow with ID: " + WORKFLOW_ID);
        
        // First approval
        Thread.sleep(2000);
        workflow.submitApprovalAction(
            new ApprovalTypes.ApprovalAction(true, "reviewer1@company.com", "First approval")
        );
        System.out.println("Submitted first approval action");
        
        // Signal to migrate workflow
        Thread.sleep(1000);
        workflow.migrateWorkflow(NEW_NAMESPACE);
        System.out.println("Signaled workflow to migrate to new namespace: " + NEW_NAMESPACE);
        
        // Final approval in new namespace
        Thread.sleep(2000);
        
        // Workflow stub for new namespace
        ApprovalWorkflow newNamespaceWorkflow = newNamespaceClient.newWorkflowStub(
            ApprovalWorkflow.class,
            WORKFLOW_ID
        );        
        newNamespaceWorkflow.submitApprovalAction(
            new ApprovalTypes.ApprovalAction(true, "manager@company.com", "Final approval")
        );
        System.out.println("Submitted final approval action in new namespace");

        // Get workflow execution result
        ApprovalTypes.WorkflowState result = WorkflowClient.execute(newNamespaceWorkflow::execute, null).get();
        ObjectMapper mapper = new ObjectMapper();
        String prettyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result);
        System.out.println("Workflow execution result:\n" + prettyJson);
        System.out.println("Approval workflow completed with namespace transition");
        
        System.exit(0);
    }
}