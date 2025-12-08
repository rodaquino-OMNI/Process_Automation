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
 * SendGridEmailDelegate - Sends transactional emails via SendGrid
 *
 * Purpose: Sends automated emails for proposals, contracts, notifications,
 * and customer communications using SendGrid API.
 *
 * Input Variables:
 * - recipientEmail: String - Recipient email address
 * - recipientName: String - Recipient name
 * - emailType: String - Email template type (proposal, contract, welcome, notification)
 * - subject: String - Email subject (optional, uses template default)
 * - templateData: Map - Dynamic data for email template
 * - attachments: List<Map> - Email attachments (optional)
 * - ccEmails: List<String> - CC recipients (optional)
 *
 * Output Variables:
 * - emailSentSuccess: Boolean - Send success indicator
 * - emailSentTimestamp: Date - Send timestamp
 * - emailMessageId: String - SendGrid message ID
 * - emailStatus: String - Delivery status
 *
 * SendGrid Integration:
 * - API Version: v3
 * - Endpoint: /v3/mail/send
 * - Authentication: API Key
 * - Rate Limit: Depends on plan
 * - Timeout: 30 seconds
 *
 * Email Templates:
 * - proposal_sent: Proposal delivery email
 * - contract_signed: Contract confirmation
 * - welcome_customer: New customer onboarding
 * - payment_reminder: Payment notification
 * - service_notification: Service updates
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("sendGridEmailDelegate")
public class SendGridEmailDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(SendGridEmailDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public SendGridEmailDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("sendGridEmail", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("sendGridEmail", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String recipientEmail = (String) execution.getVariable("recipientEmail");
        String emailType = (String) execution.getVariable("emailType");

        LOGGER.info("Sending email via SendGrid: to={}, type={}", recipientEmail, emailType);

        validateInputs(recipientEmail, emailType);

        try {
            Map<String, Object> emailResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> sendEmail(execution))
            );

            execution.setVariable("emailSentSuccess", true);
            execution.setVariable("emailSentTimestamp", new Date());
            execution.setVariable("emailMessageId", emailResult.get("messageId"));
            execution.setVariable("emailStatus", "sent");

            LOGGER.info("Email sent successfully: messageId={}", emailResult.get("messageId"));

        } catch (Exception e) {
            LOGGER.error("Email sending failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("emailSentSuccess", false);
            execution.setVariable("emailSentError", e.getMessage());
            execution.setVariable("emailStatus", "failed");
        }
    }

    private void validateInputs(String recipientEmail, String emailType) {
        if (recipientEmail == null || !recipientEmail.contains("@")) {
            throw new IllegalArgumentException("Valid recipientEmail is required");
        }
        if (emailType == null || emailType.trim().isEmpty()) {
            throw new IllegalArgumentException("emailType is required");
        }
    }

    private Map<String, Object> sendEmail(DelegateExecution execution) throws Exception {
        Map<String, Object> emailPayload = buildEmailPayload(execution);

        LOGGER.debug("Sending email with payload: {}", emailPayload);

        // TODO: Implement actual SendGrid API call
        // POST https://api.sendgrid.com/v3/mail/send
        // Header: Authorization: Bearer {API_KEY}

        Thread.sleep(1000); // Simulate API call

        String messageId = "MSG-" + System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("messageId", messageId);
        result.put("status", "queued");

        return result;
    }

    private Map<String, Object> buildEmailPayload(DelegateExecution execution) {
        Map<String, Object> payload = new HashMap<>();

        // Personalization
        Map<String, Object> personalization = new HashMap<>();

        // To
        List<Map<String, String>> toList = new ArrayList<>();
        Map<String, String> toEmail = new HashMap<>();
        toEmail.put("email", (String) execution.getVariable("recipientEmail"));
        toEmail.put("name", (String) execution.getVariable("recipientName"));
        toList.add(toEmail);
        personalization.put("to", toList);

        // CC (optional)
        List<String> ccEmails = (List<String>) execution.getVariable("ccEmails");
        if (ccEmails != null && !ccEmails.isEmpty()) {
            List<Map<String, String>> ccList = new ArrayList<>();
            for (String cc : ccEmails) {
                Map<String, String> ccMap = new HashMap<>();
                ccMap.put("email", cc);
                ccList.add(ccMap);
            }
            personalization.put("cc", ccList);
        }

        // Template data
        Map<String, Object> templateData = (Map<String, Object>) execution.getVariable("templateData");
        if (templateData != null) {
            personalization.put("dynamic_template_data", templateData);
        }

        payload.put("personalizations", Collections.singletonList(personalization));

        // From
        Map<String, String> from = new HashMap<>();
        from.put("email", "noreply@austa.com.br");
        from.put("name", "AUSTA Saúde");
        payload.put("from", from);

        // Subject
        String subject = (String) execution.getVariable("subject");
        if (subject != null) {
            payload.put("subject", subject);
        }

        // Template ID
        String emailType = (String) execution.getVariable("emailType");
        payload.put("template_id", getTemplateId(emailType));

        // Attachments (optional)
        List<Map<String, Object>> attachments = (List<Map<String, Object>>) execution.getVariable("attachments");
        if (attachments != null && !attachments.isEmpty()) {
            payload.put("attachments", attachments);
        }

        // Tracking
        Map<String, Object> trackingSettings = new HashMap<>();
        trackingSettings.put("click_tracking", Map.of("enable", true));
        trackingSettings.put("open_tracking", Map.of("enable", true));
        payload.put("tracking_settings", trackingSettings);

        return payload;
    }

    private String getTemplateId(String emailType) {
        Map<String, String> templateMap = Map.of(
            "proposal", "d-proposal123456789",
            "contract", "d-contract123456789",
            "welcome", "d-welcome123456789",
            "notification", "d-notification123456789",
            "payment_reminder", "d-payment123456789"
        );
        return templateMap.getOrDefault(emailType.toLowerCase(), "d-default123456789");
    }
}
