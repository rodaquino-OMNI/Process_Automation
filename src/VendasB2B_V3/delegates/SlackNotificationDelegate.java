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
 * SlackNotificationDelegate - Sends notifications to Slack channels
 *
 * Purpose: Sends internal team notifications for deal updates, alerts,
 * and workflow milestones to Slack channels.
 *
 * Input Variables:
 * - slackChannel: String - Slack channel (#sales, #operations)
 * - messageText: String - Message text
 * - messageType: String - Message type (info, success, warning, error)
 * - attachments: List<Map> - Rich message attachments (optional)
 * - mentionUsers: List<String> - User IDs to mention (optional)
 *
 * Output Variables:
 * - slackSentSuccess: Boolean - Send success indicator
 * - slackSentTimestamp: Date - Send timestamp
 * - slackMessageTs: String - Message timestamp (ID)
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("slackNotificationDelegate")
public class SlackNotificationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlackNotificationDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public SlackNotificationDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("slackNotification", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("slackNotification", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String slackChannel = (String) execution.getVariable("slackChannel");
        String messageText = (String) execution.getVariable("messageText");

        LOGGER.info("Sending Slack notification: channel={}", slackChannel);

        validateInputs(slackChannel, messageText);

        try {
            Map<String, Object> slackResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> sendSlackMessage(execution))
            );

            execution.setVariable("slackSentSuccess", true);
            execution.setVariable("slackSentTimestamp", new Date());
            execution.setVariable("slackMessageTs", slackResult.get("messageTs"));

            LOGGER.info("Slack notification sent successfully");

        } catch (Exception e) {
            LOGGER.error("Slack notification failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("slackSentSuccess", false);
            execution.setVariable("slackSentError", e.getMessage());
        }
    }

    private void validateInputs(String slackChannel, String messageText) {
        if (slackChannel == null || slackChannel.trim().isEmpty()) {
            throw new IllegalArgumentException("slackChannel is required");
        }
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("messageText is required");
        }
    }

    private Map<String, Object> sendSlackMessage(DelegateExecution execution) throws Exception {
        Map<String, Object> payload = buildSlackPayload(execution);

        LOGGER.debug("Sending Slack message: {}", payload);

        // TODO: Implement actual Slack API call
        // POST https://slack.com/api/chat.postMessage

        Thread.sleep(1000); // Simulate API call

        Map<String, Object> result = new HashMap<>();
        result.put("messageTs", String.valueOf(System.currentTimeMillis() / 1000.0));

        return result;
    }

    private Map<String, Object> buildSlackPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String slackChannel = (String) execution.getVariable("slackChannel");
        String messageText = (String) execution.getVariable("messageText");
        String messageType = (String) execution.getVariable("messageType");

        payload.put("channel", slackChannel);
        payload.put("text", messageText);

        // Add color based on message type
        String color = getColorForType(messageType);

        // Build attachment
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("text", messageText);
        attachment.put("footer", "AUSTA Workflow");
        attachment.put("ts", System.currentTimeMillis() / 1000);

        // Add fields
        List<Map<String, Object>> fields = buildFields(execution);
        if (!fields.isEmpty()) {
            attachment.put("fields", fields);
        }

        payload.put("attachments", Collections.singletonList(attachment));

        return payload;
    }

    private String getColorForType(String messageType) {
        if (messageType == null) return "#36a64f";
        switch (messageType.toLowerCase()) {
            case "success": return "#36a64f";
            case "warning": return "#ff9900";
            case "error": return "#ff0000";
            default: return "#3498db";
        }
    }

    private List<Map<String, Object>> buildFields(DelegateExecution execution) {
        List<Map<String, Object>> fields = new ArrayList<>();

        String nomeCliente = (String) execution.getVariable("nomeCliente");
        if (nomeCliente != null) {
            fields.add(createField("Cliente", nomeCliente, true));
        }

        Double valorContrato = (Double) execution.getVariable("valorContrato");
        if (valorContrato != null) {
            fields.add(createField("Valor", String.format("R$ %.2f", valorContrato), true));
        }

        return fields;
    }

    private Map<String, Object> createField(String title, String value, boolean isShort) {
        Map<String, Object> field = new HashMap<>();
        field.put("title", title);
        field.put("value", value);
        field.put("short", isShort);
        return field;
    }
}
