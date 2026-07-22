package io.temporal.samples.formValidation.handler;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;

@ActivityInterface
public interface ApplicationActivities {

  /** Mints a durable application ID for a submission. Fast — this is the early-return value. */
  @ActivityMethod
  String reserveApplicationId(SubmitScreenInput input);

  /** The real, potentially slow processing that happens after the early return. */
  @ActivityMethod
  void provisionAccount(String applicationId, SubmitScreenInput input);
}
