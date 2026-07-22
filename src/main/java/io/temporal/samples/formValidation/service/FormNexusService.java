package io.temporal.samples.formValidation.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.nexusrpc.Operation;
import io.nexusrpc.Service;
import java.util.Map;

/**
 * Nexus service for a multi-screen application form.
 *
 * <p>It declares a single operation, {@code submitScreen}. The handler <b>validates first</b> and
 * only <b>kicks off the Workflow if validation succeeds</b>. Invalid input is rejected by the
 * handler before any Workflow is started — so a user who never submits valid data, or who abandons
 * the form, leaves zero Workflow Executions behind. That is the fix for the
 * entity-workflow-per-application volume problem: the Nexus handler performs validation to stop the
 * Workflow from being executed.
 */
@Service
public interface FormNexusService {

  /** Input to {@link #submitScreen}: which screen, and the field values entered on it. */
  class SubmitScreenInput {
    private final String screen;
    private final Map<String, String> fields;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public SubmitScreenInput(
        @JsonProperty("screen") String screen,
        @JsonProperty("fields") Map<String, String> fields) {
      this.screen = screen;
      this.fields = fields;
    }

    @JsonProperty("screen")
    public String getScreen() {
      return screen;
    }

    @JsonProperty("fields")
    public Map<String, String> getFields() {
      return fields;
    }
  }

  /**
   * Validates the submitted screen and, on success, kicks off the application Workflow. Returns the
   * (early) {@link SubmitResult} — the application ID minted at kickoff — while the Workflow keeps
   * running to finish processing. If validation fails, the operation is rejected and no Workflow is
   * started.
   */
  @Operation
  SubmitResult submitScreen(SubmitScreenInput input);
}
