package io.temporal.samples.versioning;

import io.temporal.testing.WorkflowReplayer;
import java.io.File;
import java.net.URL;
import org.junit.jupiter.api.Test;

public class OrderWorkflowReplayTest {

    private static final String HISTORY_RESOURCE = "versioning/order_workflow_history.json";

    @Test
    void testReplay() throws Exception {
        URL resource = getClass().getClassLoader().getResource(HISTORY_RESOURCE);
        WorkflowReplayer.replayWorkflowExecution(new File(resource.toURI()), Starter.OrderWorkflowImpl.class);
    }
}
