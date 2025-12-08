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
 * BillingSetupDelegate - Configures billing in ERP/financial systems
 *
 * Purpose: Sets up automated billing, payment schedules, and invoicing
 * for new B2B healthcare contracts.
 *
 * Input Variables:
 * - contratoId: String - Contract ID
 * - clientId: String - Client ID
 * - valorMensalidade: Double - Monthly fee
 * - diaVencimento: Integer - Due day (1-31)
 * - formaPagamento: String - Payment method (boleto, transfer, card)
 * - emailFaturamento: String - Billing email
 * - cicloCobranca: String - Billing cycle (monthly, quarterly, annual)
 *
 * Output Variables:
 * - billingSetupSuccess: Boolean - Setup success indicator
 * - billingSetupTimestamp: Date - Setup timestamp
 * - billingAccountId: String - Billing account ID
 * - nextBillingDate: Date - Next billing date
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("billingSetupDelegate")
public class BillingSetupDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(BillingSetupDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public BillingSetupDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("billingSetup", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("billingSetup", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contratoId = (String) execution.getVariable("contratoId");
        String clientId = (String) execution.getVariable("clientId");
        Double valorMensalidade = (Double) execution.getVariable("valorMensalidade");

        LOGGER.info("Setting up billing: contract={}, client={}, amount={}",
                    contratoId, clientId, valorMensalidade);

        validateInputs(contratoId, clientId, valorMensalidade);

        try {
            Map<String, Object> setupResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> setupBilling(execution))
            );

            execution.setVariable("billingSetupSuccess", true);
            execution.setVariable("billingSetupTimestamp", new Date());
            execution.setVariable("billingAccountId", setupResult.get("billingAccountId"));
            execution.setVariable("nextBillingDate", setupResult.get("nextBillingDate"));

            LOGGER.info("Billing setup completed successfully: account={}",
                       setupResult.get("billingAccountId"));

        } catch (Exception e) {
            LOGGER.error("Billing setup failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("billingSetupSuccess", false);
            execution.setVariable("billingSetupError", e.getMessage());
        }
    }

    private void validateInputs(String contratoId, String clientId, Double valorMensalidade) {
        if (contratoId == null || contratoId.trim().isEmpty()) {
            throw new IllegalArgumentException("contratoId is required");
        }
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (valorMensalidade == null || valorMensalidade <= 0) {
            throw new IllegalArgumentException("valorMensalidade must be greater than 0");
        }
    }

    private Map<String, Object> setupBilling(DelegateExecution execution) throws Exception {
        Map<String, Object> billingConfig = buildBillingConfig(execution);

        LOGGER.debug("Configuring billing with: {}", billingConfig);

        // TODO: Implement actual billing system API call
        // POST /api/v1/billing/accounts

        Thread.sleep(1500); // Simulate API call

        String billingAccountId = "BILL-" + System.currentTimeMillis();
        Date nextBillingDate = calculateNextBillingDate(
            (Integer) execution.getVariable("diaVencimento")
        );

        Map<String, Object> result = new HashMap<>();
        result.put("billingAccountId", billingAccountId);
        result.put("nextBillingDate", nextBillingDate);

        return result;
    }

    private Map<String, Object> buildBillingConfig(DelegateExecution execution) {
        Map<String, Object> config = new HashMap<>();

        config.put("contractId", execution.getVariable("contratoId"));
        config.put("customerId", execution.getVariable("clientId"));
        config.put("monthlyAmount", execution.getVariable("valorMensalidade"));
        config.put("dueDay", execution.getVariable("diaVencimento"));
        config.put("paymentMethod", execution.getVariable("formaPagamento"));
        config.put("billingEmail", execution.getVariable("emailFaturamento"));
        config.put("billingCycle", execution.getVariable("cicloCobranca"));
        config.put("currency", "BRL");
        config.put("autoCharge", true);
        config.put("sendNotifications", true);

        return config;
    }

    private Date calculateNextBillingDate(Integer dueDay) {
        // Calculate next billing date based on due day
        long now = System.currentTimeMillis();
        long daysToAdd = (dueDay != null ? dueDay : 10) * 24 * 60 * 60 * 1000L;
        return new Date(now + daysToAdd);
    }
}
