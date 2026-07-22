package io.temporal.samples.formValidation.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stateless validation rules for the application form, shared by the Nexus validation handler
 * (per-screen, before submit) and the Workflow's final check (at submit). Keeping the rules in one
 * place means the synchronous per-screen validation and the authoritative submit-time validation
 * can never drift apart.
 *
 * <p>Nothing here touches Temporal — it is ordinary domain logic.
 */
public final class FormRules {

  public static final String SCREEN_APPLICANT = "applicant";
  public static final String SCREEN_ADDRESS = "address";
  public static final String SCREEN_FINANCIALS = "financials";

  private FormRules() {}

  /** Validate a single screen's fields. Returns the list of errors (empty means valid). */
  public static List<String> validateScreen(String screen, Map<String, String> fields) {
    List<String> errors = new ArrayList<>();
    if (screen == null) {
      errors.add("Missing screen name");
      return errors;
    }
    switch (screen) {
      case SCREEN_APPLICANT:
        requireNonBlank(fields, "fullName", errors);
        requireEmail(fields, "email", errors);
        break;
      case SCREEN_ADDRESS:
        requireNonBlank(fields, "street", errors);
        requireNonBlank(fields, "postcode", errors);
        break;
      case SCREEN_FINANCIALS:
        requirePositiveInt(fields, "annualIncome", errors);
        requirePositiveInt(fields, "loanAmount", errors);
        break;
      default:
        errors.add("Unknown screen: " + screen);
    }
    return errors;
  }

  private static void requireNonBlank(Map<String, String> fields, String key, List<String> errors) {
    String v = fields == null ? null : fields.get(key);
    if (v == null || v.trim().isEmpty()) {
      errors.add(key + " is required");
    }
  }

  private static void requireEmail(Map<String, String> fields, String key, List<String> errors) {
    String v = fields == null ? null : fields.get(key);
    if (v == null || !v.contains("@") || !v.contains(".")) {
      errors.add(key + " must be a valid email address");
    }
  }

  private static void requirePositiveInt(
      Map<String, String> fields, String key, List<String> errors) {
    String v = fields == null ? null : fields.get(key);
    try {
      if (v == null || Integer.parseInt(v.trim()) <= 0) {
        errors.add(key + " must be a positive number");
      }
    } catch (NumberFormatException e) {
      errors.add(key + " must be a number");
    }
  }
}
