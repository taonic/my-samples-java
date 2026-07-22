package io.temporal.samples.formValidation.handler;

import io.temporal.failure.ApplicationFailure;
import io.temporal.samples.formValidation.service.FormNexusService.SubmitScreenInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationActivitiesImpl implements ApplicationActivities {

  private static final Logger logger = LoggerFactory.getLogger(ApplicationActivitiesImpl.class);

  @Override
  public String reserveApplicationId(SubmitScreenInput input) {
    String id = "APP-" + Long.toString(Math.abs(input.getFields().hashCode()), 36).toUpperCase();
    logger.info("Reserved application ID {}", id);
    return id;
  }

  @Override
  public void verifyApplicant(SubmitScreenInput input) {
    String email = input.getFields().getOrDefault("email", "");
    logger.info("Verifying applicant '{}' against external fraud/credit service...", email);
    sleep(1000);
    // Mock rule: anyone on the blocklisted domain fails verification.
    if (email.endsWith("@blocked.com")) {
      logger.info("Applicant '{}' failed background verification", email);
      throw ApplicationFailure.newNonRetryableFailure(
          "Applicant failed background verification: " + email, "VerificationFailed");
    }
    logger.info("Applicant '{}' passed background verification", email);
  }

  @Override
  public void provisionAccount(String applicationId, SubmitScreenInput input) {
    logger.info("Provisioning account for application {} (this is the slow part)...", applicationId);
    sleep(2000);
    logger.info("Application {} provisioned from screen '{}'", applicationId, input.getScreen());
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
