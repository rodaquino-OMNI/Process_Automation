package com.austa.vendas.delegates;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * CRMTaskCreationDelegate - Creates follow-up tasks in CRM systems
 *
 * Purpose: Automates task creation for sales team follow-ups, ensuring
 * timely engagement and structured sales process execution.
 *
 * Input Variables:
 * - opportunityId: String - Related opportunity ID
 * - taskType: String - Type of task (call, email, meeting, demo, follow_up)
 * - taskSubject: String - Task subject/title
 * - taskDescription: String - Detailed task description
 * - assignedTo: String - User email or ID for task assignment
 * - dueDate: Date - Task due date
 * - priority: String - Priority level (high, medium, low)
 * - reminderMinutes: Integer - Reminder before due date (minutes)
 *
 * Output Variables:
 * - taskCreationSuccess: Boolean - Creation success indicator
 * - taskCreationTimestamp: Date - Creation timestamp
 * - taskId: String - Created task ID in CRM
 * - taskUrl: String - Direct URL to task in CRM
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("crmTaskCreationDelegate")
public class CRMTaskCreationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMTaskCreationDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public CRMTaskCreationDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("crmTaskCreation", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("crmTaskCreation", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String taskType = (String) execution.getVariable("taskType");
        String taskSubject = (String) execution.getVariable("taskSubject");
        String assignedTo = (String) execution.getVariable("assignedTo");

        LOGGER.info("Creating CRM task: type={}, subject={}, assignedTo={}",
                    taskType, taskSubject, assignedTo);

        validateInputs(taskType, taskSubject, assignedTo);

        try {
            Map<String, Object> taskResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> createTask(execution))
            );

            execution.setVariable("taskCreationSuccess", true);
            execution.setVariable("taskCreationTimestamp", new Date());
            execution.setVariable("taskId", taskResult.get("taskId"));
            execution.setVariable("taskUrl", taskResult.get("taskUrl"));

            LOGGER.info("CRM task created successfully: {}", taskResult.get("taskId"));

        } catch (Exception e) {
            LOGGER.error("CRM task creation failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("taskCreationSuccess", false);
            execution.setVariable("taskCreationError", e.getMessage());
        }
    }

    private void validateInputs(String taskType, String taskSubject, String assignedTo) {
        if (taskType == null || taskType.trim().isEmpty()) {
            throw new IllegalArgumentException("taskType is required");
        }
        if (taskSubject == null || taskSubject.trim().isEmpty()) {
            throw new IllegalArgumentException("taskSubject is required");
        }
        if (assignedTo == null || assignedTo.trim().isEmpty()) {
            throw new IllegalArgumentException("assignedTo is required");
        }
    }

    private Map<String, Object> createTask(DelegateExecution execution) throws Exception {
        Map<String, Object> taskPayload = buildTaskPayload(execution);

        LOGGER.debug("Creating CRM task with payload: {}", taskPayload);

        // TODO: Implement actual CRM API call
        // Salesforce: POST /services/data/v58.0/sobjects/Task
        // HubSpot: POST /crm/v3/objects/tasks

        Thread.sleep(1000); // Simulate API call

        String taskId = "TASK-" + System.currentTimeMillis();
        String taskUrl = "https://austa.lightning.force.com/lightning/r/Task/" + taskId + "/view";

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("taskUrl", taskUrl);

        return result;
    }

    private Map<String, Object> buildTaskPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String taskType = (String) execution.getVariable("taskType");
        String taskSubject = (String) execution.getVariable("taskSubject");
        String taskDescription = (String) execution.getVariable("taskDescription");
        String assignedTo = (String) execution.getVariable("assignedTo");
        Date dueDate = (Date) execution.getVariable("dueDate");
        String priority = (String) execution.getVariable("priority");
        Integer reminderMinutes = (Integer) execution.getVariable("reminderMinutes");
        String opportunityId = (String) execution.getVariable("opportunityId");

        payload.put("Subject", taskSubject);
        payload.put("Description", taskDescription != null ? taskDescription : "Automated task from workflow");
        payload.put("ActivityDate", dueDate != null ? dueDate : calculateDefaultDueDate(taskType));
        payload.put("Priority", mapPriority(priority));
        payload.put("Status", "Not Started");
        payload.put("Type", mapTaskType(taskType));
        payload.put("OwnerId", assignedTo);

        if (opportunityId != null) {
            payload.put("WhatId", opportunityId);
        }

        if (reminderMinutes != null && reminderMinutes > 0) {
            payload.put("IsReminderSet", true);
            payload.put("ReminderDateTime", calculateReminderTime(dueDate, reminderMinutes));
        }

        payload.put("AUSTA_Process_ID__c", execution.getProcessInstanceId());
        payload.put("AUSTA_Created_By__c", "Camunda Workflow");

        return payload;
    }

    private String mapTaskType(String taskType) {
        Map<String, String> typeMapping = Map.of(
            "call", "Call",
            "email", "Email",
            "meeting", "Meeting",
            "demo", "Demo",
            "follow_up", "Follow-up"
        );
        return typeMapping.getOrDefault(taskType.toLowerCase(), "Other");
    }

    private String mapPriority(String priority) {
        if (priority == null) return "Normal";
        switch (priority.toLowerCase()) {
            case "high": return "High";
            case "low": return "Low";
            default: return "Normal";
        }
    }

    private Date calculateDefaultDueDate(String taskType) {
        long now = System.currentTimeMillis();
        long daysToAdd = taskType.equals("call") ? 1 : 3;
        return new Date(now + (daysToAdd * 24 * 60 * 60 * 1000));
    }

    private Date calculateReminderTime(Date dueDate, Integer reminderMinutes) {
        if (dueDate == null) return null;
        return new Date(dueDate.getTime() - (reminderMinutes * 60 * 1000));
    }
}
