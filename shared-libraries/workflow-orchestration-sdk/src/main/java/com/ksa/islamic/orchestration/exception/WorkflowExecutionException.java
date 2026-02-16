package com.ksa.islamic.orchestration.exception;

/**
 * Exception for workflow execution failures
 *
 * Thrown when a workflow fails to execute properly,
 * excluding activity-specific failures.
 */
public class WorkflowExecutionException extends RuntimeException {

    private final String workflowId;
    private final String workflowType;
    private final String failureStage;

    public WorkflowExecutionException(String message, String workflowId, String workflowType) {
        super(message);
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.failureStage = null;
    }

    public WorkflowExecutionException(String message, String workflowId, String workflowType, String failureStage) {
        super(message);
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.failureStage = failureStage;
    }

    public WorkflowExecutionException(String message, String workflowId, String workflowType, Throwable cause) {
        super(message, cause);
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.failureStage = null;
    }

    public WorkflowExecutionException(String message, String workflowId, String workflowType,
                                     String failureStage, Throwable cause) {
        super(message, cause);
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.failureStage = failureStage;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public String getFailureStage() {
        return failureStage;
    }

    @Override
    public String toString() {
        return String.format("WorkflowExecutionException{workflowId='%s', workflowType='%s', failureStage='%s', message='%s'}",
                workflowId, workflowType, failureStage, getMessage());
    }
}