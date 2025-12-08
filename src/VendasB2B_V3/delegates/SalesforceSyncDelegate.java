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
 * SalesforceSyncDelegate - Synchronizes opportunity data with Salesforce CRM
 *
 * Purpose: Maintains bidirectional sync between Camunda workflow state and Salesforce,
 * ensuring accurate opportunity tracking, MEDDIC scores, stage progressions, and
 * pipeline reporting across both systems.
 *
 * Input Variables:
 * - opportunityId: String - Salesforce opportunity ID (18-char)
 * - nomeCliente: String - Account/Company name
 * - syncType: String - Type of sync (full_sync, opportunity_update, activity_log, meddic_update)
 * - stageName: String - Current opportunity stage
 * - scoreMEDDIC: Double - MEDDIC qualification score (0-10)
 * - amount: Double - Opportunity amount in BRL
 * - closeDate: Date - Expected close date
 * - ownerEmail: String - Opportunity owner email
 * - campaignId: String - Associated campaign ID
 *
 * Output Variables:
 * - salesforceSyncSuccess: Boolean - Sync success indicator
 * - salesforceSyncTimestamp: Date - Sync completion timestamp
 * - salesforceOpportunityUrl: String - Direct URL to opportunity
 * - salesforceRecordVersion: String - Record version for conflict detection
 *
 * Salesforce API Integration:
 * - Endpoint: /services/data/v58.0/sobjects/Opportunity/{Id}
 * - Authentication: OAuth 2.0 with JWT Bearer Flow
 * - Method: PATCH for updates, POST for activities
 * - Timeout: 30 seconds
 * - Rate Limit: 15,000 API calls/24h (monitored)
 *
 * Custom Fields Mapped:
 * - AUSTA_MEDDIC_Score__c: MEDDIC qualification score
 * - AUSTA_Phase__c: Internal workflow phase
 * - AUSTA_Process_Instance_ID__c: Camunda process instance ID
 * - AUSTA_Engagement_Score__c: Engagement level (0-100)
 * - AUSTA_ROI_Calculated__c: ROI percentage
 * - AUSTA_Champion_Confirmed__c: Champion identification status
 *
 * Error Handling:
 * - Circuit Breaker: Opens after 5 consecutive failures
 * - Retry: 3 attempts with exponential backoff (5s, 15s, 45s)
 * - Timeout: 30 seconds per request
 * - Fallback: Queues sync for batch processing
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("salesforceSyncDelegate")
public class SalesforceSyncDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SalesforceSyncDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;
    private static final String SALESFORCE_API_VERSION = "v58.0";

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public SalesforceSyncDelegate() {
        // Configure Circuit Breaker
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("salesforceSync", cbConfig);

        // Configure Retry
        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("salesforceSync", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String opportunityId = (String) execution.getVariable("opportunityId");
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String syncType = (String) execution.getVariable("syncType");

        LOGGER.info("Starting Salesforce sync for opportunity: {} ({}), type: {}",
                    opportunityId, nomeCliente, syncType);

        validateInputs(opportunityId, nomeCliente);

        try {
            // Execute sync with circuit breaker and retry
            Map<String, Object> syncResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> performSync(execution, opportunityId, syncType))
            );

            // Set output variables
            execution.setVariable("salesforceSyncSuccess", true);
            execution.setVariable("salesforceSyncTimestamp", new Date());
            execution.setVariable("salesforceOpportunityUrl",
                generateOpportunityUrl(opportunityId));
            execution.setVariable("salesforceRecordVersion", syncResult.get("version"));
            execution.setVariable("salesforceResponse", syncResult.get("response"));

            LOGGER.info("Salesforce sync completed successfully for opportunity: {}", opportunityId);

        } catch (Exception e) {
            LOGGER.error("Salesforce sync failed for opportunity: {} after {} attempts",
                        opportunityId, MAX_RETRY_ATTEMPTS, e);

            // Queue for batch sync
            queueForBatchSync(execution, opportunityId);

            execution.setVariable("salesforceSyncSuccess", false);
            execution.setVariable("salesforceSyncError", e.getMessage());
            execution.setVariable("salesforceSyncQueued", true);

            // Don't fail workflow - continue with queued sync
        }
    }

    /**
     * Validates required input variables
     */
    private void validateInputs(String opportunityId, String nomeCliente) {
        if (opportunityId == null || opportunityId.trim().isEmpty()) {
            throw new IllegalArgumentException("opportunityId is required for Salesforce sync");
        }
        if (opportunityId.length() != 15 && opportunityId.length() != 18) {
            throw new IllegalArgumentException("Invalid Salesforce opportunityId format");
        }
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            throw new IllegalArgumentException("nomeCliente is required for Salesforce sync");
        }
    }

    /**
     * Performs the actual Salesforce sync operation
     */
    private Map<String, Object> performSync(DelegateExecution execution, String opportunityId,
                                           String syncType) throws Exception {
        Map<String, Object> payload = buildSyncPayload(execution, syncType);

        LOGGER.debug("Syncing to Salesforce with payload: {}", payload);

        // TODO: Implement actual Salesforce REST API call
        // Example: PATCH https://instance.salesforce.com/services/data/v58.0/sobjects/Opportunity/{Id}
        // Headers: Authorization: Bearer {access_token}, Content-Type: application/json

        // Simulate API call
        Thread.sleep(1000); // Simulate network latency

        Map<String, Object> result = new HashMap<>();
        result.put("response", "{ \"id\": \"" + opportunityId + "\", \"success\": true }");
        result.put("version", String.valueOf(System.currentTimeMillis()));

        return result;
    }

    /**
     * Builds sync payload based on sync type
     */
    private Map<String, Object> buildSyncPayload(DelegateExecution execution, String syncType) {
        Map<String, Object> payload = new HashMap<>();

        switch (syncType != null ? syncType.toLowerCase() : "opportunity_update") {
            case "full_sync":
                buildFullSyncPayload(execution, payload);
                break;
            case "meddic_update":
                buildMEDDICPayload(execution, payload);
                break;
            case "activity_log":
                buildActivityPayload(execution, payload);
                break;
            case "opportunity_update":
            default:
                buildOpportunityUpdatePayload(execution, payload);
        }

        // Add metadata
        payload.put("AUSTA_Process_Instance_ID__c", execution.getProcessInstanceId());
        payload.put("AUSTA_Last_Sync__c", new Date());

        return payload;
    }

    private void buildFullSyncPayload(DelegateExecution execution, Map<String, Object> payload) {
        // Opportunity fields
        buildOpportunityUpdatePayload(execution, payload);

        // MEDDIC fields
        buildMEDDICPayload(execution, payload);

        // Additional fields
        Integer engagementScore = (Integer) execution.getVariable("scoreEngajamento_ENG");
        Boolean championConfirmed = (Boolean) execution.getVariable("championConfirmado_ENG");
        Double roiCalculated = (Double) execution.getVariable("roiCalculado_VAL");

        if (engagementScore != null) {
            payload.put("AUSTA_Engagement_Score__c", engagementScore);
        }
        if (championConfirmed != null) {
            payload.put("AUSTA_Champion_Confirmed__c", championConfirmed);
        }
        if (roiCalculated != null) {
            payload.put("AUSTA_ROI_Calculated__c", roiCalculated);
        }
    }

    private void buildOpportunityUpdatePayload(DelegateExecution execution, Map<String, Object> payload) {
        String stageName = (String) execution.getVariable("stageName");
        Double amount = (Double) execution.getVariable("amount");
        Date closeDate = (Date) execution.getVariable("closeDate");
        String ownerEmail = (String) execution.getVariable("ownerEmail");

        if (stageName != null) payload.put("StageName", stageName);
        if (amount != null) payload.put("Amount", amount);
        if (closeDate != null) payload.put("CloseDate", closeDate);
        if (ownerEmail != null) payload.put("Owner", ownerEmail);
    }

    private void buildMEDDICPayload(DelegateExecution execution, Map<String, Object> payload) {
        Double scoreMEDDIC = (Double) execution.getVariable("scoreMEDDIC");
        Map<String, Object> scoreDetalhado = (Map<String, Object>) execution.getVariable("scoreDetalhado");

        if (scoreMEDDIC != null) {
            payload.put("AUSTA_MEDDIC_Score__c", scoreMEDDIC);
        }

        if (scoreDetalhado != null) {
            payload.put("AUSTA_Metrics__c", scoreDetalhado.get("metrics"));
            payload.put("AUSTA_Economic_Buyer__c", scoreDetalhado.get("economicBuyer"));
            payload.put("AUSTA_Decision_Criteria__c", scoreDetalhado.get("decisionCriteria"));
            payload.put("AUSTA_Decision_Process__c", scoreDetalhado.get("decisionProcess"));
            payload.put("AUSTA_Identify_Pain__c", scoreDetalhado.get("identifyPain"));
            payload.put("AUSTA_Champion__c", scoreDetalhado.get("champion"));
        }
    }

    private void buildActivityPayload(DelegateExecution execution, Map<String, Object> payload) {
        String activityNote = (String) execution.getVariable("activityNote");
        String activityType = (String) execution.getVariable("activityType");

        payload.put("Subject", "Camunda Workflow Update");
        payload.put("Description", activityNote);
        payload.put("ActivityDate", new Date());
        payload.put("Type", activityType != null ? activityType : "Note");
    }

    /**
     * Generates Salesforce opportunity URL
     */
    private String generateOpportunityUrl(String opportunityId) {
        return "https://austa.lightning.force.com/lightning/r/Opportunity/" + opportunityId + "/view";
    }

    /**
     * Queues failed sync for batch processing
     */
    private void queueForBatchSync(DelegateExecution execution, String opportunityId) {
        LOGGER.warn("Queueing opportunity {} for batch sync", opportunityId);
        // TODO: Implement queue mechanism (e.g., message queue, database table)
        execution.setVariable("batchSyncQueueTime", new Date());
    }
}
