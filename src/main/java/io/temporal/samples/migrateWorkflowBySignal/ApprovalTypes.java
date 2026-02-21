package io.temporal.samples.migrateWorkflowBySignal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApprovalTypes {
    
    public static class GetCandidateApproversRequest {
        private String requestId;
        private String department;
        
        public GetCandidateApproversRequest() {}
        
        public GetCandidateApproversRequest(String requestId, String department) {
            this.requestId = requestId;
            this.department = department;
        }
        
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
    }
    
    public static class CandidateApprovers {
        private List<String> approvers;
        
        public CandidateApprovers() {}
        
        public CandidateApprovers(List<String> approvers) {
            this.approvers = approvers;
        }
        
        public List<String> getApprovers() { return approvers; }
        public void setApprovers(List<String> approvers) { this.approvers = approvers; }
    }
    
    public static class ApproverTask {
        private String taskId;
        private List<String> approvers;
        private String status;
        
        public ApproverTask() {}
        
        public ApproverTask(String taskId, List<String> approvers, String status) {
            this.taskId = taskId;
            this.approvers = approvers;
            this.status = status;
        }
        
        public String getTaskId() { return taskId; }
        public void setTaskId(String taskId) { this.taskId = taskId; }
        public List<String> getApprovers() { return approvers; }
        public void setApprovers(List<String> approvers) { this.approvers = approvers; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
    
    public static class ApprovalAction {
        private boolean approved;
        private String approver;
        private String comments;
        
        public ApprovalAction() {}
        
        public ApprovalAction(boolean approved, String approver, String comments) {
            this.approved = approved;
            this.approver = approver;
            this.comments = comments;
        }
        
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getApprover() { return approver; }
        public void setApprover(String approver) { this.approver = approver; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }
    
    public static class ApprovalResult {
        private boolean approved;
        private String approver;
        private long timestamp;
        
        public ApprovalResult() {}
        
        public ApprovalResult(boolean approved, String approver, long timestamp) {
            this.approved = approved;
            this.approver = approver;
            this.timestamp = timestamp;
        }
        
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getApprover() { return approver; }
        public void setApprover(String approver) { this.approver = approver; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    public static class ExecutionCompletion {
        private String status;
        private long completedAt;
        
        public ExecutionCompletion() {}
        
        public ExecutionCompletion(String status, long completedAt) {
            this.status = status;
            this.completedAt = completedAt;
        }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public long getCompletedAt() { return completedAt; }
        public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }
    }
    
    public static class WorkflowState {
        private CandidateApprovers candidateApprovers;
        private ApproverTask approverTask;
        private List<ApprovalResult> approvalResults;
        private ExecutionCompletion executionCompletion;
        private ApprovalAction approvalAction;
        private int requiredApprovals;
        private Map<String, Integer> versionMap;
        private boolean migrated;

        public WorkflowState() {
            this.versionMap = new HashMap<>();
            this.approvalResults = new java.util.ArrayList<>();
            this.requiredApprovals = 1;
            this.migrated = false;
        }

        public int getVersionFor(String changeId) {
            return versionMap.getOrDefault(changeId, 1);
        }

        public CandidateApprovers getCandidateApprovers() { return candidateApprovers; }
        public void setCandidateApprovers(CandidateApprovers candidateApprovers) { this.candidateApprovers = candidateApprovers; }
        public ApproverTask getApproverTask() { return approverTask; }
        public void setApproverTask(ApproverTask approverTask) { this.approverTask = approverTask; }
        public List<ApprovalResult> getApprovalResults() { return approvalResults; }
        public void setApprovalResults(List<ApprovalResult> approvalResults) { this.approvalResults = approvalResults; }
        public int getRequiredApprovals() { return requiredApprovals; }
        public void setRequiredApprovals(int requiredApprovals) { this.requiredApprovals = requiredApprovals; }
        public ExecutionCompletion getExecutionCompletion() { return executionCompletion; }
        public void setExecutionCompletion(ExecutionCompletion executionCompletion) { this.executionCompletion = executionCompletion; }
        public ApprovalAction getApprovalAction() { return approvalAction; }
        public void setApprovalAction(ApprovalAction approvalAction) { this.approvalAction = approvalAction; }
        public Map<String, Integer> getVersionMap() { return versionMap; }
        public void setVersionMap(Map<String, Integer> versionMap) { this.versionMap = versionMap; }
        public boolean isMigrated() { return migrated; }
        public void setMigrated(boolean migrated) { this.migrated = migrated; }
    }
}