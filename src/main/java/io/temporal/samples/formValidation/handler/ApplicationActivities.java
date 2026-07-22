package io.temporal.samples.formValidation.handler;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;

@ActivityInterface
public interface ApplicationActivities {

  /** Mints a durable application ID for a submission. Fast — this is the early-return value. */
  @ActivityMethod
  String reserveApplicationId(SubmitScreenInput input);

  /**
   * Activity-based validation invoked during initialization (as a local activity), before the submit
   * Update returns. Unlike the synchronous {@link
   * io.temporal.samples.formValidation.service.FormRules} checks, this one calls an external
   * verification service (fraud/credit), so it can't run in the Nexus handler. If it fails it throws,
   * failing the submit Update and rejecting the caller before any application ID is minted.
   */
  @ActivityMethod
  void verifyApplicant(SubmitScreenInput input);

  /** The real, potentially slow processing that happens after the early return. */
  @ActivityMethod
  void provisionAccount(String applicationId, SubmitScreenInput input);
}
