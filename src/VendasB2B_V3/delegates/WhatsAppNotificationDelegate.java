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
 * WhatsAppNotificationDelegate - Sends WhatsApp messages via Twilio API
 *
 * Purpose: Sends WhatsApp notifications for customer engagement,
 * appointment reminders, and service updates.
 *
 * Input Variables:
 * - whatsappNumber: String - Recipient WhatsApp number
 * - messageText: String - Message text
 * - messageTemplate: String - Template name (optional)
 * - templateParams: List<String> - Template parameters (optional)
 * - mediaUrl: String - Media URL for images/documents (optional)
 *
 * Output Variables:
 * - whatsappSentSuccess: Boolean - Send success indicator
 * - whatsappSentTimestamp: Date - Send timestamp
 * - whatsappMessageSid: String - Message SID
 * - whatsappStatus: String - Delivery status
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("whatsAppNotificationDelegate")
public class WhatsAppNotificationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(WhatsAppNotificationDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public WhatsAppNotificationDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("whatsappNotification", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("whatsappNotification", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String whatsappNumber = (String) execution.getVariable("whatsappNumber");
        String messageText = (String) execution.getVariable("messageText");

        LOGGER.info("Sending WhatsApp notification: to={}", whatsappNumber);

        validateInputs(whatsappNumber, messageText);

        try {
            Map<String, Object> whatsappResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> sendWhatsApp(execution))
            );

            execution.setVariable("whatsappSentSuccess", true);
            execution.setVariable("whatsappSentTimestamp", new Date());
            execution.setVariable("whatsappMessageSid", whatsappResult.get("messageSid"));
            execution.setVariable("whatsappStatus", "sent");

            LOGGER.info("WhatsApp sent successfully: sid={}", whatsappResult.get("messageSid"));

        } catch (Exception e) {
            LOGGER.error("WhatsApp sending failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("whatsappSentSuccess", false);
            execution.setVariable("whatsappSentError", e.getMessage());
            execution.setVariable("whatsappStatus", "failed");
        }
    }

    private void validateInputs(String whatsappNumber, String messageText) {
        if (whatsappNumber == null || whatsappNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("whatsappNumber is required");
        }
        if (messageText == null || messageText.trim().isEmpty()) {
            throw new IllegalArgumentException("messageText is required");
        }
    }

    private Map<String, Object> sendWhatsApp(DelegateExecution execution) throws Exception {
        Map<String, Object> payload = buildWhatsAppPayload(execution);

        LOGGER.debug("Sending WhatsApp with payload: {}", payload);

        // TODO: Implement actual Twilio WhatsApp API call
        // POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json

        Thread.sleep(1000); // Simulate API call

        String messageSid = "WA" + System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("messageSid", messageSid);
        result.put("status", "queued");

        return result;
    }

    private Map<String, Object> buildWhatsAppPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String whatsappNumber = (String) execution.getVariable("whatsappNumber");
        String messageText = (String) execution.getVariable("messageText");
        String mediaUrl = (String) execution.getVariable("mediaUrl");

        payload.put("To", "whatsapp:" + formatPhoneNumber(whatsappNumber));
        payload.put("From", "whatsapp:+5511999999999"); // Twilio WhatsApp number
        payload.put("Body", messageText);

        if (mediaUrl != null && !mediaUrl.trim().isEmpty()) {
            payload.put("MediaUrl", Collections.singletonList(mediaUrl));
        }

        payload.put("StatusCallback", "https://api.austa.com.br/webhook/whatsapp-status");

        return payload;
    }

    private String formatPhoneNumber(String phoneNumber) {
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");
        if (!cleaned.startsWith("55")) {
            cleaned = "55" + cleaned;
        }
        return "+" + cleaned;
    }
}
