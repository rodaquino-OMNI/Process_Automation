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
import java.util.*;

/**
 * DataWarehouseDelegate - Syncs data to enterprise data warehouse
 *
 * Purpose: Pushes workflow data, metrics, and business events to the
 * data warehouse for analytics, reporting, and business intelligence.
 *
 * Input Variables:
 * - dataType: String - Type of data (opportunity, contract, activity)
 * - syncMode: String - Sync mode (full, incremental)
 * - dataPayload: Map - Data to sync
 *
 * Output Variables:
 * - dwSyncSuccess: Boolean - Sync success indicator
 * - dwSyncTimestamp: Date - Sync timestamp
 * - recordsInserted: Integer - Number of records inserted
 * - recordsUpdated: Integer - Number of records updated
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("dataWarehouseDelegate")
public class DataWarehouseDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataWarehouseDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 45000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public DataWarehouseDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(120))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("dataWarehouse", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(10))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("dataWarehouse", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String dataType = (String) execution.getVariable("dataType");

        LOGGER.info("Syncing to data warehouse: dataType={}", dataType);

        try {
            Map<String, Object> syncResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> syncToDataWarehouse(execution))
            );

            execution.setVariable("dwSyncSuccess", true);
            execution.setVariable("dwSyncTimestamp", new Date());
            execution.setVariable("recordsInserted", syncResult.get("recordsInserted"));
            execution.setVariable("recordsUpdated", syncResult.get("recordsUpdated"));

            LOGGER.info("Data warehouse sync completed: inserted={}, updated={}",
                       syncResult.get("recordsInserted"), syncResult.get("recordsUpdated"));

        } catch (Exception e) {
            LOGGER.error("Data warehouse sync failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("dwSyncSuccess", false);
            execution.setVariable("dwSyncError", e.getMessage());
        }
    }

    private Map<String, Object> syncToDataWarehouse(DelegateExecution execution) throws Exception {
        String dataType = (String) execution.getVariable("dataType");
        Map<String, Object> dataPayload = buildDataPayload(execution, dataType);

        LOGGER.debug("Syncing to data warehouse: {}", dataPayload);

        // TODO: Implement actual Data Warehouse API/ETL call
        // Could be Snowflake, Redshift, BigQuery, or on-prem warehouse
        // POST /api/v1/dw/sync or direct SQL insert

        Thread.sleep(2000); // Simulate ETL operation

        Map<String, Object> result = new HashMap<>();
        result.put("recordsInserted", 1);
        result.put("recordsUpdated", 0);

        return result;
    }

    private Map<String, Object> buildDataPayload(DelegateExecution execution, String dataType) {
        Map<String, Object> payload = new HashMap<>();

        payload.put("dataType", dataType);
        payload.put("processInstanceId", execution.getProcessInstanceId());
        payload.put("businessKey", execution.getBusinessKey());
        payload.put("timestamp", new Date());

        // Add all workflow variables
        Map<String, Object> variables = execution.getVariables();
        payload.put("workflowData", variables);

        return payload;
    }
}
