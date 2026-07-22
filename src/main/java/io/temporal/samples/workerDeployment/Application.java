package io.temporal.samples.workerDeployment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the worker deployment sample.
 *
 * <p>This is a single Spring Boot application that contains the workflow and activity code for every
 * business domain. Which domain workers actually start in a given process is decided entirely by the
 * active Spring profile (see {@code src/main/resources/application-*.yml}), NOT by the code. That is
 * the whole point of the sample:
 *
 * <ul>
 *   <li>In production you deploy this same jar once per domain, each with a different profile, so
 *       every domain gets its own independently scaled and independently released worker fleet.
 *   <li>For local development you run the {@code all} profile so a single JVM hosts every domain's
 *       worker.
 * </ul>
 *
 * <p>See README.md for the full architecture guidance behind this layout.
 */
@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
