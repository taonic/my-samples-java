package io.temporal.samples.migrateWorkflowBySignal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public class ApprovalActivitiesImplTest {

    private ApprovalActivitiesImpl activities;

    @BeforeEach
    public void setUp() {
        activities = new ApprovalActivitiesImpl();
    }

    @Test
    public void testGetCandidateApprovers() {
        ApprovalTypes.GetCandidateApproversRequest request = 
            new ApprovalTypes.GetCandidateApproversRequest("req-123", "engineering");
        
        ApprovalTypes.CandidateApprovers result = activities.getCandidateApprovers(request);
        
        assertNotNull(result);
        assertEquals(2, result.getApprovers().size());
        assertTrue(result.getApprovers().contains("approver1@company.com"));
        assertTrue(result.getApprovers().contains("approver2@company.com"));
    }

    @Test
    public void testCreateApproverTask() {
        ApprovalTypes.CandidateApprovers candidateApprovers = 
            new ApprovalTypes.CandidateApprovers(Arrays.asList("approver1@company.com"));
        
        ApprovalTypes.ApproverTask result = activities.createApproverTask(candidateApprovers);
        
        assertNotNull(result);
        assertTrue(result.getTaskId().startsWith("task-"));
        assertEquals("PENDING", result.getStatus());
        assertEquals(candidateApprovers.getApprovers(), result.getApprovers());
    }

    @Test
    public void testSubmitApproverTaskApproved() {
        ApprovalTypes.ApprovalAction action = 
            new ApprovalTypes.ApprovalAction(true, "approver1@company.com", "Looks good");
        
        ApprovalTypes.ApprovalResult result = activities.submitApproverTask(action);
        
        assertNotNull(result);
        assertTrue(result.isApproved());
        assertEquals("approver1@company.com", result.getApprover());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    public void testSubmitApproverTaskRejected() {
        ApprovalTypes.ApprovalAction action = 
            new ApprovalTypes.ApprovalAction(false, "approver2@company.com", "Needs changes");
        
        ApprovalTypes.ApprovalResult result = activities.submitApproverTask(action);
        
        assertNotNull(result);
        assertFalse(result.isApproved());
        assertEquals("approver2@company.com", result.getApprover());
        assertTrue(result.getTimestamp() > 0);
    }

    @Test
    public void testCompleteApprovalExecutionData() {
        ApprovalTypes.ApprovalResult approvalResult = 
            new ApprovalTypes.ApprovalResult(true, "approver1@company.com", System.currentTimeMillis());
        
        ApprovalTypes.ExecutionCompletion result = activities.completeApprovalExecutionData(approvalResult);
        
        assertNotNull(result);
        assertEquals("COMPLETED", result.getStatus());
        assertTrue(result.getCompletedAt() > 0);
    }
}