package io.temporal.samples.formValidation.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The value returned to the client from the submit Update. The {@code applicationId} is minted
 * during initialization and returned <b>early</b> (via Update-with-Start), while the Workflow keeps
 * running to finish provisioning.
 */
public final class SubmitResult {
  private final String applicationId;
  private final String status;

  @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
  public SubmitResult(
      @JsonProperty("applicationId") String applicationId, @JsonProperty("status") String status) {
    this.applicationId = applicationId;
    this.status = status;
  }

  @JsonProperty("applicationId")
  public String getApplicationId() {
    return applicationId;
  }

  @JsonProperty("status")
  public String getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return String.format("SubmitResult{applicationId='%s', status='%s'}", applicationId, status);
  }
}
