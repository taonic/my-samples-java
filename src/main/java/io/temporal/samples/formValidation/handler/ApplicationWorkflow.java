package io.temporal.samples.formValidation.handler;

import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;
import io.temporal.samples.formValidation.service.SubmitResult;
import io.temporal.workflow.UpdateMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * The one Workflow for a genuine submission. It is kicked off exactly once — by the Nexus handler,
 * via Update-with-Start, and only after validation has passed.
 */
@WorkflowInterface
public interface ApplicationWorkflow {

  /**
   * Runs the full submission: mint an application ID (the early-return value), then do the slow
   * provisioning. The submitted screen data is supplied by the {@code WithStartWorkflowOperation}.
   */
  @WorkflowMethod
  SubmitResult processScreen(SubmitScreenInput input);

  /**
   * The early-return Update. It blocks only until the application ID has been minted, then returns
   * it to the caller while {@link #processScreen} carries on provisioning in the background.
   */
  @UpdateMethod(name = "submit")
  SubmitResult submit();
}
