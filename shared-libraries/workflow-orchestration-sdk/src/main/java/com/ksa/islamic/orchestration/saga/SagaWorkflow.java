package com.ksa.islamic.orchestration.saga;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Base interface for SAGA pattern workflows
 *
 * All SAGA workflows should implement this interface to ensure
 * consistent execution and compensation patterns.
 *
 * @param <I> Input type for the saga
 * @param <O> Output type for the saga
 */
@WorkflowInterface
public interface SagaWorkflow<I, O> {

    /**
     * Execute the SAGA workflow
     *
     * @param input The input for the saga
     * @return The result of the saga execution
     */
    @WorkflowMethod
    O execute(I input);
}