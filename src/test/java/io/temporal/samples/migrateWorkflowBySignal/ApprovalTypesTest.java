package io.temporal.samples.migrateWorkflowBySignal;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ApprovalTypesTest {

    @Test
    public void testGetCandidateApproversRequest() {
        ApprovalTypes.GetCandidateApproversRequest request = 
            new ApprovalTypes.GetCandidateApproversRequest("req-123", "engineering");
        
        assertEquals("req-123", request.getRequestId());
        assertEquals("engineering", request.getDepartment());
        
        request.setRequestId("req-456");
        request.setDepartment("finance");
        
        assertEquals("req-456", request.getRequestId());
        assertEquals("finance", request.getDepartment());
    }

    @Test
    public void testCandidateApprovers() {
        List<String> approvers = Arrays.asList("user1@company.com", "user2@company.com");
        ApprovalTypes.CandidateApprovers candidateApprovers = 
            new ApprovalTypes.CandidateApprovers(approvers);
        
        assertEquals(approvers, candidateApprovers.getApprovers());
        
        List<String> newApprovers = Arrays.asList("user3@company.com");
        candidateApprovers.setApprovers(newApprovers);
        
        assertEquals(newApprovers, candidateApprovers.getApprovers());
    }

    @Test
    public void testApproverTask() {
        List<String> approvers = Arrays.asList("user1@company.com");
        ApprovalTypes.ApproverTask task = 
            new ApprovalTypes.ApproverTask("task-123", approvers, "PENDING");
        
        assertEquals("task-123", task.getTaskId());
        assertEquals(approvers, task.getApprovers());
        assertEquals("PENDING", task.getStatus());
        
        task.setStatus("COMPLETED");
        assertEquals("COMPLETED", task.getStatus());
    }

    @Test
    public void testApprovalAction() {
        ApprovalTypes.ApprovalAction action = 
            new ApprovalTypes.ApprovalAction(true, "user1@company.com", "Approved");
        
        assertTrue(action.isApproved());
        assertEquals("user1@company.com", action.getApprover());
        assertEquals("Approved", action.getComments());
        
        action.setApproved(false);
        action.setComments("Rejected");
        
        assertFalse(action.isApproved());
        assertEquals("Rejected", action.getComments());
    }

    @Test
    public void testApprovalResult() {
        long timestamp = System.currentTimeMillis();
        ApprovalTypes.ApprovalResult result = 
            new ApprovalTypes.ApprovalResult(true, "user1@company.com", timestamp);
        
        assertTrue(result.isApproved());
        assertEquals("user1@company.com", result.getApprover());
        assertEquals(timestamp, result.getTimestamp());
    }

    @Test
    public void testExecutionCompletion() {
        long completedAt = System.currentTimeMillis();
        ApprovalTypes.ExecutionCompletion completion = 
            new ApprovalTypes.ExecutionCompletion("COMPLETED", completedAt);
        
        assertEquals("COMPLETED", completion.getStatus());
        assertEquals(completedAt, completion.getCompletedAt());
    }

    @Test
    public void testWorkflowState() {
        ApprovalTypes.WorkflowState state = new ApprovalTypes.WorkflowState();
        
        // Test default version behavior
        assertEquals(1, state.getVersionFor("unknown-change"));
        
        // Test setting workflow state components
        ApprovalTypes.CandidateApprovers approvers = 
            new ApprovalTypes.CandidateApprovers(Arrays.asList("user1@company.com"));
        state.setCandidateApprovers(approvers);
        
        assertEquals(approvers, state.getCandidateApprovers());
        
        ApprovalTypes.ApprovalAction action = 
            new ApprovalTypes.ApprovalAction(true, "user1@company.com", "Approved");
        state.setApprovalAction(action);
        
        assertEquals(action, state.getApprovalAction());
    }
}