package io.temporal.samples.formValidation;

import io.temporal.client.NexusClient;
import io.temporal.client.NexusClientOptions;
import io.temporal.client.NexusOperationException;
import io.temporal.client.NexusServiceClient;
import io.temporal.client.StartNexusOperationOptions;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowStub;
import io.temporal.samples.formValidation.handler.ApplicationWorkflow;
import io.temporal.samples.formValidation.service.FormNexusService;
import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;
import io.temporal.samples.formValidation.service.FormRules;
import io.temporal.samples.formValidation.service.SubmitResult;
import io.temporal.serviceclient.WorkflowServiceStubs;
import java.time.Duration;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drives the form the way a browser front-end would: every submission goes
 * through the single
 * {@code submitScreen} Nexus operation (standalone Nexus — no caller Workflow
 * on the client side).
 *
 * <p>
 * The handler validates first and only kicks off the Workflow when validation
 * passes. So:
 *
 * <ul>
 * <li>Scenario 1 submits invalid data. The operation is rejected and <b>no
 * Workflow is created</b>
 * — proven by the running-Workflow count staying at 0.
 * <li>Scenario 2 submits valid data. The operation kicks off exactly one
 * Workflow and returns the
 * application ID early, while the Workflow finishes processing in the
 * background.
 * <li>Scenario 3 submits data that passes the synchronous validation but fails
 * an <b>activity-based</b> verification (a local activity calling an external
 * service). That check runs during initialization, so its failure propagates
 * out of the submit Update — the submit is rejected with no application ID.
 * Unlike Scenario 1, a Workflow is started by the with-start, but it completes
 * in the rejected state.
 * </ul>
 */
public class FormClient {

    private static final Logger log = LoggerFactory.getLogger(FormClient.class);

    // Must match the Nexus endpoint configured on the server (see README).
    private static final String ENDPOINT_NAME = "form-validation-endpoint";

    public static void main(String[] args) throws Exception {
        WorkflowServiceStubs stubs = WorkflowServiceStubs.newLocalServiceStubs();
        WorkflowClient workflowClient = WorkflowClient.newInstance(stubs);
        String namespace = workflowClient.getOptions().getNamespace();

        NexusClient nexusClient = NexusClient.newInstance(
                stubs, NexusClientOptions.newBuilder().setNamespace(namespace).build());
        NexusServiceClient<FormNexusService> formClient = nexusClient.newNexusServiceClient(FormNexusService.class,
                ENDPOINT_NAME);

        // -------------------------------------------------------------------------------------------
        // Scenario 1: invalid submission. The Nexus handler rejects it; no Workflow is
        // started.
        // -------------------------------------------------------------------------------------------
        log.info("=== Scenario 1: invalid submission ===");
        try {
            SubmitResult r = submit(
                    formClient,
                    "invalid-applicant",
                    FormRules.SCREEN_APPLICANT,
                    Map.of("fullName", "", "email", "not-an-email"));
            log.error("Expected rejection, but submit returned: {}", r);
        } catch (NexusOperationException e) {
            log.info("Submission rejected by the Nexus handler (no Workflow started): {}", e.getMessage());
        }
        log.info(
                "RUNNING ApplicationWorkflow executions after the invalid submission: {} (expected 0)",
                countRunningApplications(workflowClient));

        // -------------------------------------------------------------------------------------------
        // Scenario 2: valid submission. Validation passes, so the handler kicks off the
        // Workflow.
        // -------------------------------------------------------------------------------------------
        log.info("=== Scenario 2: valid submission ===");
        String email = "grace@example.com";
        SubmitResult early = submit(
                formClient,
                "valid-applicant",
                FormRules.SCREEN_APPLICANT,
                Map.of("fullName", "Grace Hopper", "email", email));
        log.info(
                "Submit returned EARLY: {} (applicationId={}). Workflow keeps running.",
                early.getStatus(),
                early.getApplicationId());
        log.info(
                "RUNNING ApplicationWorkflow executions right after the valid submission: {} (expected 1)",
                countRunningApplications(workflowClient));

        // Wait for the background processing to finish, addressing the Workflow the
        // handler started.
        ApplicationWorkflow started = workflowClient.newWorkflowStub(ApplicationWorkflow.class, "application-" + email);
        SubmitResult finalResult = WorkflowStub.fromTyped(started).getResult(SubmitResult.class);
        log.info(
                "Workflow completed: {} (applicationId={})",
                finalResult.getStatus(),
                finalResult.getApplicationId());

        // -------------------------------------------------------------------------------------------
        // Scenario 3: passes synchronous validation, but fails an activity-based verification. That
        // verification runs (as a local activity) during initialization, so its failure propagates
        // out of the submit Update — the handler's executeUpdateWithStart throws and the caller's
        // submit is rejected, with NO application ID returned. Unlike Scenario 1, a Workflow IS
        // started by the with-start; it simply completes in the rejected state.
        // -------------------------------------------------------------------------------------------
        log.info("=== Scenario 3: valid form, but fails activity-based verification ===");
        String blockedEmail = "mallory@blocked.com";
        try {
            SubmitResult r = submit(
                    formClient,
                    "blocked-applicant",
                    FormRules.SCREEN_APPLICANT,
                    Map.of("fullName", "Mallory", "email", blockedEmail));
            log.error("Expected verification to fail the submit, but it returned: {}", r);
        } catch (NexusOperationException e) {
            log.info("Submit rejected: activity-based verification failed the Update: {}", e.getMessage());
        }

        System.exit(0);
    }

    /**
     * Submit one screen via the Nexus operation. The handler validates and, if
     * valid, kicks off.
     */
    private static SubmitResult submit(
            NexusServiceClient<FormNexusService> client,
            String opId,
            String screen,
            Map<String, String> fields) {
        return client.execute(
                FormNexusService::submitScreen,
                StartNexusOperationOptions.newBuilder()
                        .setId("submit-" + opId)
                        .setScheduleToCloseTimeout(Duration.ofSeconds(30))
                        .build(),
                new SubmitScreenInput(screen, fields));
    }

    private static long countRunningApplications(WorkflowClient client) {
        return client
                .countWorkflows("WorkflowType = 'ApplicationWorkflow' AND ExecutionStatus = 'Running'")
                .getCount();
    }
}
