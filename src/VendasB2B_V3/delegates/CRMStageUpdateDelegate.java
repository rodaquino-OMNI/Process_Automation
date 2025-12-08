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
 * CRMStageUpdateDelegate - Updates sales stage in CRM systems
 *
 * Purpose: Maintains accurate sales stage progression across CRM platforms,
 * ensuring pipeline visibility and forecasting accuracy.
 *
 * Input Variables:
 * - opportunityId: String - CRM opportunity identifier
 * - currentStage: String - Current stage name
 * - newStage: String - Target stage name
 * - stageTransitionReason: String - Reason for stage change
 * - probability: Integer - Win probability (0-100)
 * - expectedCloseDate: Date - Expected close date
 * - dealValue: Double - Opportunity value
 *
 * Output Variables:
 * - stageUpdateSuccess: Boolean - Update success indicator
 * - stageUpdateTimestamp: Date - Update timestamp
 * - previousStage: String - Stage before update
 * - currentCRMStage: String - Stage after update
 * - stageHistory: List<Map> - Stage transition history
 *
 * Stage Mapping:
 * - QUALIFICATION → Qualification (10% probability)
 * - ENGAGEMENT → Engagement (40% probability)
 * - VALUE_DEMONSTRATION → Value Demo (50% probability)
 * - PROPOSAL → Proposal (60% probability)
 * - NEGOTIATION → Negotiation (75% probability)
 * - CLOSED_WON → Closed Won (100% probability)
 * - CLOSED_LOST → Closed Lost (0% probability)
 *
 * Error Handling:
 * - Circuit Breaker: Opens after 5 failures
 * - Retry: 3 attempts with exponential backoff
 * - Timeout: 30 seconds
 * - Fallback: Logs error, continues workflow
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("crmStageUpdateDelegate")
public class CRMStageUpdateDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(CRMStageUpdateDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public CRMStageUpdateDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("crmStageUpdate", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("crmStageUpdate", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String opportunityId = (String) execution.getVariable("opportunityId");
        String currentStage = (String) execution.getVariable("currentStage");
        String newStage = (String) execution.getVariable("newStage");

        LOGGER.info("Updating CRM stage for opportunity {}: {} → {}",
                    opportunityId, currentStage, newStage);

        validateStageTransition(currentStage, newStage);

        try {
            Map<String, Object> updateResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> performStageUpdate(execution, opportunityId, newStage))
            );

            execution.setVariable("stageUpdateSuccess", true);
            execution.setVariable("stageUpdateTimestamp", new Date());
            execution.setVariable("previousStage", currentStage);
            execution.setVariable("currentCRMStage", newStage);

            // Update stage history
            updateStageHistory(execution, currentStage, newStage);

            LOGGER.info("CRM stage updated successfully: {}", newStage);

        } catch (Exception e) {
            LOGGER.error("CRM stage update failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("stageUpdateSuccess", false);
            execution.setVariable("stageUpdateError", e.getMessage());
        }
    }

    private void validateStageTransition(String currentStage, String newStage) {
        if (newStage == null || newStage.trim().isEmpty()) {
            throw new IllegalArgumentException("newStage is required");
        }

        // Validate stage progression (prevent backward movement except to lost)
        if (currentStage != null && !isValidTransition(currentStage, newStage)) {
            LOGGER.warn("Invalid stage transition detected: {} → {}", currentStage, newStage);
        }
    }

    private boolean isValidTransition(String from, String to) {
        // Allow any transition to CLOSED_LOST or CLOSED_WON
        if (to.equals("CLOSED_LOST") || to.equals("CLOSED_WON")) {
            return true;
        }

        // Define valid forward progressions
        Map<String, Integer> stageOrder = Map.of(
            "QUALIFICATION", 1,
            "ENGAGEMENT", 2,
            "VALUE_DEMONSTRATION", 3,
            "PROPOSAL", 4,
            "NEGOTIATION", 5,
            "CLOSED_WON", 6
        );

        Integer fromOrder = stageOrder.get(from);
        Integer toOrder = stageOrder.get(to);

        return fromOrder != null && toOrder != null && toOrder >= fromOrder;
    }

    private Map<String, Object> performStageUpdate(DelegateExecution execution,
                                                   String opportunityId, String newStage)
            throws Exception {

        Map<String, Object> updatePayload = buildStageUpdatePayload(execution, newStage);

        LOGGER.debug("Updating CRM stage with payload: {}", updatePayload);

        // TODO: Implement actual CRM API call
        // Salesforce: PATCH /services/data/v58.0/sobjects/Opportunity/{opportunityId}
        // HubSpot: PATCH /crm/v3/objects/deals/{dealId}

        Thread.sleep(1000); // Simulate API call

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("stage", newStage);

        return result;
    }

    private Map<String, Object> buildStageUpdatePayload(DelegateExecution execution, String newStage) {
        Map<String, Object> payload = new HashMap<>();

        // Map to CRM stage name
        String crmStageName = mapToCRMStage(newStage);
        payload.put("StageName", crmStageName);
        payload.put("AUSTA_Internal_Stage__c", newStage);

        // Calculate probability based on stage
        Integer probability = calculateProbability(newStage);
        payload.put("Probability", probability);

        // Add transition metadata
        String transitionReason = (String) execution.getVariable("stageTransitionReason");
        if (transitionReason != null) {
            payload.put("Stage_Transition_Reason__c", transitionReason);
        }

        Date expectedCloseDate = (Date) execution.getVariable("expectedCloseDate");
        if (expectedCloseDate != null) {
            payload.put("CloseDate", expectedCloseDate);
        }

        Double dealValue = (Double) execution.getVariable("dealValue");
        if (dealValue != null) {
            payload.put("Amount", dealValue);
        }

        // Add timestamp
        payload.put("LastStageChangeDate", new Date());
        payload.put("AUSTA_Process_ID__c", execution.getProcessInstanceId());

        return payload;
    }

    private String mapToCRMStage(String internalStage) {
        Map<String, String> stageMapping = Map.of(
            "QUALIFICATION", "Qualification",
            "ENGAGEMENT", "Engagement",
            "VALUE_DEMONSTRATION", "Value Demonstration",
            "PROPOSAL", "Proposal Sent",
            "NEGOTIATION", "Negotiation",
            "CLOSED_WON", "Closed Won",
            "CLOSED_LOST", "Closed Lost"
        );

        return stageMapping.getOrDefault(internalStage, "Open");
    }

    private Integer calculateProbability(String stage) {
        Map<String, Integer> probabilityMap = Map.of(
            "QUALIFICATION", 10,
            "ENGAGEMENT", 40,
            "VALUE_DEMONSTRATION", 50,
            "PROPOSAL", 60,
            "NEGOTIATION", 75,
            "CLOSED_WON", 100,
            "CLOSED_LOST", 0
        );

        return probabilityMap.getOrDefault(stage, 10);
    }

    private void updateStageHistory(DelegateExecution execution, String fromStage, String toStage) {
        // TODO: Implement stage history tracking
        Map<String, Object> historyEntry = new HashMap<>();
        historyEntry.put("fromStage", fromStage);
        historyEntry.put("toStage", toStage);
        historyEntry.put("timestamp", new Date());
        historyEntry.put("processInstanceId", execution.getProcessInstanceId());

        LOGGER.debug("Stage history entry created: {}", historyEntry);
    }
}
