package com.ksa.islamic.orchestration.activity.collections;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

/**
 * Daily cron workflow that monitors repayment schedules.
 *
 * Runs at 6:00 AM KSA time (UTC+3) every day.
 * Cron schedule: "0 3 * * *"  (3am UTC = 6am KSA)
 *
 * Each execution:
 *  1. Find SCHEDULED installments where due_date <= today → mark DUE
 *  2. Find DUE installments where due_date + grace_days < today → mark OVERDUE
 *  3. For newly OVERDUE loans → start DunningWorkflow
 *  4. Send PRE_DUE reminders for installments due in 3 days
 */
@WorkflowInterface
public interface ScheduleMonitorWorkflow {

    @WorkflowMethod
    ScheduleMonitorResult execute(ScheduleMonitorRequest request);

    record ScheduleMonitorRequest(
            String tenantId,        // null = all tenants
            String runDate          // ISO date string, e.g. "2026-05-01"
    ) {}

    record ScheduleMonitorResult(
            String runDate,
            int installmentsMarkedDue,
            int installmentsMarkedOverdue,
            int dunningWorkflowsStarted,
            int remindersSent
    ) {}
}
