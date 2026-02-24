package io.temporal.samples.dedicatedMigrateWorkflowBySignal;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class ApprovalWorkflowImpl implements ApprovalWorkflow {

    private static final String CREATE_APPROVER_TASK_VERSION = "createApproverTaskVersion";

    private final ApprovalActivities activities = Workflow.newActivityStub(
        ApprovalActivities.class,
        ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5))
            .build()
    );

    private String newNamespace;
    private String newTaskQueue;
    private int createApproverTaskVersion = Workflow.DEFAULT_VERSION;
    private ApprovalTypes.ApprovalAction approvalAction;
    private final List<ApprovalTypes.ApprovalResult> approvalResults = new ArrayList<>();
    private ApprovalTypes.CandidateApprovers candidateApprovers;
    private ApprovalTypes.ApproverTask approverTask;
    private ApprovalTypes.CandidateApprovers approvers;    

    @Override
    public ApprovalTypes.WorkflowState execute(int requiredApprovals) {

        Promise<Void> migrationPromise = Async.function(() -> {
            // Block until a migrateWorkflow signal is received with the target namespace
            Workflow.await(() -> newNamespace != null);

            // Snapshot current workflow state to carry over to the new namespace
            ApprovalTypes.WorkflowState state = new ApprovalTypes.WorkflowState();
            state.setCandidateApprovers(candidateApprovers);
            state.setApproverTask(approverTask);
            state.setApprovalResults(approvalResults);
            state.setRequiredApprovals(requiredApprovals);
            state.setMigrated(true);
            state.getVersionMap().put(CREATE_APPROVER_TASK_VERSION, createApproverTaskVersion);
            
            // Trigger migration activity; the new workflow in the target namespace resumes from this state
            activities.migrateWorkflow(newNamespace, newTaskQueue, state);
            return null;
        });

        Promise<Void> approvalPromise = Async.function(() -> {
            // Original workflow logic
            
            // Get current version and update state
            createApproverTaskVersion = Workflow.getVersion(
                CREATE_APPROVER_TASK_VERSION,
                Workflow.DEFAULT_VERSION,
                1
            );

            approvers = activities.getCandidateApprovers(new ApprovalTypes.GetCandidateApproversRequest());
            
            if (createApproverTaskVersion >= 2) {
                approverTask = activities.createApproverTaskV2(approvers);
            } else {
                approverTask = activities.createApproverTask(approvers);
            }

            while (approvalResults.size() < requiredApprovals) {
                Workflow.await(() -> approvalAction != null);

                ApprovalTypes.ApprovalResult result = activities.submitApproverTask(approvalAction);
                approvalResults.add(result);

                if (!result.isApproved()) break;

                approvalAction = null;
            }

            ApprovalTypes.ApprovalResult last = approvalResults.isEmpty() ? null : approvalResults.get(approvalResults.size() - 1);
            activities.completeApprovalExecutionData(last);
            return null;
        });

        Promise.anyOf(migrationPromise, approvalPromise).get();

        return null;
    }

    @Override
    public void submitApprovalAction(ApprovalTypes.ApprovalAction approvalAction) {
        this.approvalAction = approvalAction;
    }

    @Override
    public void migrateWorkflow(String namespace, String taskQueue) {
        this.newNamespace = namespace;
        this.newTaskQueue = taskQueue;
    }
}
