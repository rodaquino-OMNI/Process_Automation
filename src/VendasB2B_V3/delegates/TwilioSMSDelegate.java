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
 * TwilioSMSDelegate - Sends SMS notifications via Twilio
 *
 * Purpose: Sends automated SMS notifications for urgent communications,
 * alerts, reminders, and confirmations.
 *
 * Input Variables:
 * - phoneNumber: String - Recipient phone number (E.164 format)
 * - messageBody: String - SMS message text
 * - messageType: String - Message type (alert, reminder, confirmation, otp)
 * - priority: String - Priority level (high, normal, low)
 *
 * Output Variables:
 * - smsSentSuccess: Boolean - Send success indicator
 * - smsSentTimestamp: Date - Send timestamp
 * - smsMessageSid: String - Twilio message SID
 * - smsStatus: String - Delivery status
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("twilioSMSDelegate")
public class TwilioSMSDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TwilioSMSDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public TwilioSMSDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("twilioSMS", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("twilioSMS", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String phoneNumber = (String) execution.getVariable("phoneNumber");
        String messageBody = (String) execution.getVariable("messageBody");

        LOGGER.info("Sending SMS via Twilio: to={}", phoneNumber);

        validateInputs(phoneNumber, messageBody);

        try {
            Map<String, Object> smsResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> sendSMS(execution))
            );

            execution.setVariable("smsSentSuccess", true);
            execution.setVariable("smsSentTimestamp", new Date());
            execution.setVariable("smsMessageSid", smsResult.get("messageSid"));
            execution.setVariable("smsStatus", "sent");

            LOGGER.info("SMS sent successfully: sid={}", smsResult.get("messageSid"));

        } catch (Exception e) {
            LOGGER.error("SMS sending failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("smsSentSuccess", false);
            execution.setVariable("smsSentError", e.getMessage());
            execution.setVariable("smsStatus", "failed");
        }
    }

    private void validateInputs(String phoneNumber, String messageBody) {
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("phoneNumber is required");
        }
        if (messageBody == null || messageBody.trim().isEmpty()) {
            throw new IllegalArgumentException("messageBody is required");
        }
        if (messageBody.length() > 1600) {
            throw new IllegalArgumentException("messageBody exceeds maximum length (1600 chars)");
        }
    }

    private Map<String, Object> sendSMS(DelegateExecution execution) throws Exception {
        Map<String, Object> smsPayload = buildSMSPayload(execution);

        LOGGER.debug("Sending SMS with payload: {}", smsPayload);

        // TODO: Implement actual Twilio API call
        // POST https://api.twilio.com/2010-04-01/Accounts/{AccountSid}/Messages.json

        Thread.sleep(1000); // Simulate API call

        String messageSid = "SM" + System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("messageSid", messageSid);
        result.put("status", "queued");

        return result;
    }

    private Map<String, Object> buildSMSPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        String phoneNumber = (String) execution.getVariable("phoneNumber");
        String messageBody = (String) execution.getVariable("messageBody");

        payload.put("To", formatPhoneNumber(phoneNumber));
        payload.put("From", "+5511999999999"); // Twilio phone number
        payload.put("Body", messageBody);
        payload.put("StatusCallback", "https://api.austa.com.br/webhook/sms-status");

        return payload;
    }

    private String formatPhoneNumber(String phoneNumber) {
        // Remove non-digits
        String cleaned = phoneNumber.replaceAll("[^0-9]", "");

        // Add +55 if not present (Brazil)
        if (!cleaned.startsWith("55")) {
            cleaned = "55" + cleaned;
        }

        return "+" + cleaned;
    }
}
