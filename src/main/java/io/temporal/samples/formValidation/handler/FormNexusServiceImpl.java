package io.temporal.samples.formValidation.handler;

import io.nexusrpc.handler.HandlerException;
import io.nexusrpc.handler.OperationHandler;
import io.nexusrpc.handler.OperationImpl;
import io.nexusrpc.handler.ServiceImpl;
import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.UpdateOptions;
import io.temporal.client.WithStartWorkflowOperation;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.nexus.Nexus;
import io.temporal.samples.formValidation.service.FormNexusService;
import io.temporal.samples.formValidation.service.FormRules;
import io.temporal.samples.formValidation.service.SubmitResult;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implements {@link FormNexusService}.
 *
 * <p>
 * {@code submitScreen} is a <b>synchronous</b> operation
 * ({@link OperationHandler#sync}) that
 * does two things, in order:
 *
 * <ol>
 * <li><b>Validate.</b> If the screen is invalid, it throws a
 * {@link HandlerException} with {@code
 *       BAD_REQUEST}. The operation is rejected and <b>no Workflow is ever
 * started</b> — this is the
 * whole point: the Nexus handler performs validation to stop the Workflow from
 * being executed.
 * <li><b>Kick off the Workflow.</b> Only on successful validation does it start
 * the one {@link
 * ApplicationWorkflow}, via Update-with-Start. The {@code submit} Update
 * returns the minted
 * application ID <b>early</b>, which the handler returns to the caller, while
 * the Workflow
 * keeps running to finish processing.
 * </ol>
 */
@ServiceImpl(service = FormNexusService.class)
public class FormNexusServiceImpl {

  private static final Logger logger = LoggerFactory.getLogger(FormNexusServiceImpl.class);

  @OperationImpl
  public OperationHandler<FormNexusService.SubmitScreenInput, SubmitResult> submitScreen() {
    return OperationHandler.sync(
        (ctx, details, input) -> {
          // 1. Validate. On failure, reject before any Workflow is started.
          List<String> errors = FormRules.validateScreen(input.getScreen(), input.getFields());
          if (!errors.isEmpty()) {
            logger.info(
                "Rejected screen '{}' — no Workflow started: {}", input.getScreen(), errors);
            throw new HandlerException(
                HandlerException.ErrorType.BAD_REQUEST, "Validation failed: " + errors);
          }

          // 2. Validation passed: kick off the one Workflow via Update-with-Start.
          WorkflowClient client = Nexus.getOperationContext().getWorkflowClient();
          ApplicationWorkflow workflow =
              client.newWorkflowStub(
                  ApplicationWorkflow.class,
                  WorkflowOptions.newBuilder()
                      .setTaskQueue(HandlerWorker.TASK_QUEUE_NAME)
                      .setWorkflowId(
                          "application-" + input.getFields().getOrDefault("email", "unknown"))
                      .setWorkflowIdConflictPolicy(
                          WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_FAIL)
                      .build());

          SubmitResult early =
              WorkflowClient.executeUpdateWithStart(
                  workflow::submit,
                  UpdateOptions.<SubmitResult>newBuilder().build(),
                  new WithStartWorkflowOperation<SubmitResult>(workflow::processScreen, input));
          logger.info(
              "Validation passed for screen '{}'; kicked off Workflow, returning early result {}",
              input.getScreen(),
              early);
          return early;
        });
  }
}
