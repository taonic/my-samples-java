/*
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package io.temporal.samples.updateWithStart;

import io.temporal.api.enums.v1.WorkflowIdConflictPolicy;
import io.temporal.client.*;
import io.temporal.failure.ApplicationFailure;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.*;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sample that demonstrates Update-with-Start, and specifically what happens when the Update is
 * rejected by its validator.
 *
 * <p>Update-with-Start sends an Update request and starts the Workflow in the same call, if the
 * Workflow isn't already running. It is great for latency sensitive, lazy-initialization use cases
 * such as a shopping cart: the client can add an item to the cart without needing to know whether
 * the cart (the Workflow) already exists.
 *
 * <p>The key thing this sample highlights: Update-with-Start is <b>not</b> atomic. If the Update is
 * rejected by its validator, the Workflow is <b>still started</b>. This differs from
 * Signal-with-Start, which is atomic. The client receives a {@link WorkflowUpdateException}, but a
 * new, running Workflow Execution is left behind.
 *
 * <p>Note: Make sure to set {@code frontend.enableUpdateWorkflowExecution=true} in your Temporal
 * config to enable update.
 */
public class Starter {

  static final String TASK_QUEUE = "UpdateWithStartTaskQueue";
  static final String WORKFLOW_ID = "ShoppingCartWorkflow";

  @WorkflowInterface
  public interface ShoppingCartWorkflow {

    /** Runs until {@link #checkout()} is called, then returns the ordered contents of the cart. */
    @WorkflowMethod
    List<String> shop();

    /**
     * Adds an item to the cart and returns the number of items now in the cart. Paired with {@link
     * #validateAddItem(String, int)} which rejects invalid requests before they mutate state.
     */
    @UpdateMethod
    int addItem(String item, int quantity);

    /**
     * Update validator for {@link #addItem(String, int)}. Throwing any exception here rejects the
     * Update: it is never applied, nothing is written to Workflow history, and the caller receives
     * a {@link WorkflowUpdateException}.
     *
     * <p>A validator must return void, take the same arguments as the Update, and must not mutate
     * Workflow state or block.
     */
    @UpdateValidatorMethod(updateName = "addItem")
    void validateAddItem(String item, int quantity);

    /** Query the current contents of the cart. */
    @QueryMethod
    List<String> getCart();

    @SignalMethod
    void checkout();
  }

  public static class ShoppingCartWorkflowImpl implements ShoppingCartWorkflow {

    private final List<String> cart = new ArrayList<>();
    private boolean checkedOut = false;

    @Override
    public List<String> shop() {
      Workflow.await(() -> checkedOut);
      return cart;
    }

    @Override
    public int addItem(String item, int quantity) {
      for (int i = 0; i < quantity; i++) {
        cart.add(item);
      }
      return cart.size();
    }

    @Override
    public void validateAddItem(String item, int quantity) {
      if (item == null || item.isEmpty()) {
        throw ApplicationFailure.newFailure("Item name must not be empty", "InvalidItem");
      }
      if (quantity <= 0) {
        throw ApplicationFailure.newFailure(
            "Quantity must be positive, got " + quantity, "InvalidQuantity");
      }
    }

    @Override
    public List<String> getCart() {
      return cart;
    }

    @Override
    public void checkout() {
      checkedOut = true;
    }
  }

  public static void main(String[] args) throws Exception {
    Logger log = LoggerFactory.getLogger(Starter.class);

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();
    WorkflowClient client = WorkflowClient.newInstance(service);
    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(ShoppingCartWorkflowImpl.class);
    factory.start();

    // A WorkflowIdConflictPolicy is REQUIRED for Update-with-Start. USE_EXISTING means "start the
    // Workflow if it isn't running yet, otherwise attach to the existing one" - the natural fit for
    // the lazy-initialization / shopping-cart pattern.
    WorkflowOptions options =
        WorkflowOptions.newBuilder()
            .setTaskQueue(TASK_QUEUE)
            .setWorkflowId(WORKFLOW_ID)
            .setWorkflowIdConflictPolicy(WorkflowIdConflictPolicy.WORKFLOW_ID_CONFLICT_POLICY_USE_EXISTING)
            .build();

    // ---------------------------------------------------------------------------------------------
    // Scenario 1: the Update is rejected by its validator.
    //
    // We ask to add an item with an invalid (zero) quantity. The validator rejects it, so the
    // Update never applies and the caller gets a WorkflowUpdateException. But note: the Workflow is
    // started anyway. Update-with-Start is NOT atomic.
    // ---------------------------------------------------------------------------------------------
    log.info("Scenario 1: Update-with-Start with an INVALID Update (quantity=0)");

    ShoppingCartWorkflow workflow = client.newWorkflowStub(ShoppingCartWorkflow.class, options);
    WithStartWorkflowOperation<List<String>> startOp =
        new WithStartWorkflowOperation<>(workflow::shop);

    try {
      WorkflowClient.executeUpdateWithStart(
          workflow::addItem,
          "apple",
          0, // invalid: quantity must be positive
          UpdateOptions.<Integer>newBuilder()
              .setWaitForStage(WorkflowUpdateStage.COMPLETED)
              .build(),
          startOp);
      log.error("Expected the Update to be rejected, but it succeeded!");
    } catch (WorkflowUpdateException e) {
      log.info("Update was rejected by the validator as expected: {}", e.getCause().getMessage());
    }

    // Even though the Update was rejected, the Workflow was started. Prove it with a Query: the
    // Workflow is running and its cart is empty (the rejected Update left no trace).
    ShoppingCartWorkflow existing =
        client.newWorkflowStub(ShoppingCartWorkflow.class, WORKFLOW_ID);
    log.info(
        "Despite the rejected Update, the Workflow is running. Current cart contents: {}",
        existing.getCart());

    // ---------------------------------------------------------------------------------------------
    // Scenario 2: a valid Update-with-Start.
    //
    // Because the Workflow from Scenario 1 is already running, USE_EXISTING makes this attach to it
    // instead of starting a new one. This time the validator accepts the Update and it is applied.
    // ---------------------------------------------------------------------------------------------
    log.info("Scenario 2: Update-with-Start with a VALID Update (quantity=3)");

    ShoppingCartWorkflow workflow2 = client.newWorkflowStub(ShoppingCartWorkflow.class, options);
    WithStartWorkflowOperation<List<String>> startOp2 =
        new WithStartWorkflowOperation<>(workflow2::shop);

    int cartSize =
        WorkflowClient.executeUpdateWithStart(
            workflow2::addItem,
            "banana",
            3,
            UpdateOptions.<Integer>newBuilder()
                .setWaitForStage(WorkflowUpdateStage.COMPLETED)
                .build(),
            startOp2);
    log.info("Update accepted and applied. Cart now holds {} item(s).", cartSize);

    // Wrap up: check out and print the final cart.
    workflow2.checkout();
    List<String> finalCart = workflow2.shop();
    log.info("Final cart after checkout: {}", finalCart);

    System.exit(0);
  }
}
