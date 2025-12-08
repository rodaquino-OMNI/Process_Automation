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
 * PortalActivationDelegate - Activates customer portal access
 *
 * Purpose: Creates customer portal accounts, sets permissions, and
 * sends activation credentials for the AUSTA customer self-service portal.
 *
 * Input Variables:
 * - clientId: String - Client identifier
 * - nomeCliente: String - Company name
 * - adminEmail: String - Portal admin email
 * - adminName: String - Portal admin name
 * - numeroVidas: Integer - Number of beneficiaries
 * - modules: List<String> - Enabled modules (claims, network, reports)
 *
 * Output Variables:
 * - portalActivationSuccess: Boolean - Activation success indicator
 * - portalActivationTimestamp: Date - Activation timestamp
 * - portalUrl: String - Portal URL
 * - portalUserId: String - Portal user ID
 * - activationToken: String - Activation token
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("portalActivationDelegate")
public class PortalActivationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalActivationDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public PortalActivationDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("portalActivation", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("portalActivation", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String clientId = (String) execution.getVariable("clientId");
        String adminEmail = (String) execution.getVariable("adminEmail");

        LOGGER.info("Activating customer portal: client={}, admin={}", clientId, adminEmail);

        validateInputs(clientId, adminEmail);

        try {
            Map<String, Object> activationResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> activatePortal(execution))
            );

            execution.setVariable("portalActivationSuccess", true);
            execution.setVariable("portalActivationTimestamp", new Date());
            execution.setVariable("portalUrl", activationResult.get("portalUrl"));
            execution.setVariable("portalUserId", activationResult.get("userId"));
            execution.setVariable("activationToken", activationResult.get("token"));

            LOGGER.info("Portal activated successfully: userId={}", activationResult.get("userId"));

        } catch (Exception e) {
            LOGGER.error("Portal activation failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("portalActivationSuccess", false);
            execution.setVariable("portalActivationError", e.getMessage());
        }
    }

    private void validateInputs(String clientId, String adminEmail) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required");
        }
        if (adminEmail == null || !adminEmail.contains("@")) {
            throw new IllegalArgumentException("Valid adminEmail is required");
        }
    }

    private Map<String, Object> activatePortal(DelegateExecution execution) throws Exception {
        Map<String, Object> activationPayload = buildActivationPayload(execution);

        LOGGER.debug("Activating portal with: {}", activationPayload);

        // TODO: Implement actual Portal API call
        // POST /api/v1/portal/activate

        Thread.sleep(1500); // Simulate API call

        String userId = "USR-" + System.currentTimeMillis();
        String token = UUID.randomUUID().toString();
        String portalUrl = "https://portal.austa.com.br";

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("token", token);
        result.put("portalUrl", portalUrl);

        return result;
    }

    private Map<String, Object> buildActivationPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String clientId = (String) execution.getVariable("clientId");
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String adminEmail = (String) execution.getVariable("adminEmail");
        String adminName = (String) execution.getVariable("adminName");
        Integer numeroVidas = (Integer) execution.getVariable("numeroVidas");
        List<String> modules = (List<String>) execution.getVariable("modules");

        payload.put("clientId", clientId);
        payload.put("companyName", nomeCliente);
        payload.put("adminEmail", adminEmail);
        payload.put("adminName", adminName);
        payload.put("beneficiaryCount", numeroVidas);
        payload.put("modules", modules != null ? modules :
                   Arrays.asList("claims", "network", "reports", "documents"));
        payload.put("status", "active");
        payload.put("tier", "standard");

        return payload;
    }
}
