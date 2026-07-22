package io.temporal.samples.formValidation.handler;

import io.temporal.activity.ActivityOptions;
import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;
import io.temporal.samples.formValidation.service.FormRules;
import io.temporal.samples.formValidation.service.SubmitResult;
import io.temporal.workflow.Workflow;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;

public class ApplicationWorkflowImpl implements ApplicationWorkflow {

  private static final Logger logger = Workflow.getLogger(ApplicationWorkflowImpl.class);

  private final ApplicationActivities activities =
      Workflow.newActivityStub(
          ApplicationActivities.class,
          ActivityOptions.newBuilder().setStartToCloseTimeout(Duration.ofSeconds(30)).build());

  private boolean initDone = false;
  private String applicationId;
  private RuntimeException initError = null;

  @Override
  public SubmitResult processScreen(SubmitScreenInput input) {
    // Initialization: defensive re-validation followed by minting the application ID. Whatever
    // happens here is what the early-return submit Update reports back to the caller. (The Nexus
    // handler already validated, but re-checking here guards against a client that bypasses it.)
    try {
      List<String> errors = FormRules.validateScreen(input.getScreen(), input.getFields());
      if (!errors.isEmpty()) {
        throw ApplicationFailure.newNonRetryableFailure(
            "Application failed validation: " + errors, "InvalidApplication");
      }
      applicationId = activities.reserveApplicationId(input);
    } catch (RuntimeException e) {
      initError = e;
    } finally {
      initDone = true;
    }

    if (initError != null) {
      // Nothing to provision — the Workflow ends immediately with a rejected result.
      return new SubmitResult("", "Submission rejected.");
    }

    // The early return has already happened by now; carry on with the slow work.
    activities.provisionAccount(applicationId, input);
    return new SubmitResult(applicationId, "Application processed successfully.");
  }

  @Override
  public SubmitResult submit() {
    Workflow.await(() -> initDone);
    if (initError != null) {
      logger.info("Submit rejected during initialization.");
      throw initError;
    }
    return new SubmitResult(applicationId, "Submission accepted.");
  }
}
