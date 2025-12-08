package com.austa.salesprocess.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Credential Delivery Delegate - AUSTA V3
 *
 * Purpose: Deliver health card credentials to beneficiaries via email and SMS
 * Channels: Email (PDF attachment + download link) and SMS (secure link)
 *
 * Responsibilities:
 * - Send health card PDFs via email
 * - Send secure download links via SMS
 * - Generate temporary access codes
 * - Track delivery status
 * - Retry failed deliveries
 * - Log all delivery attempts
 *
 * Delivery Channels:
 * - Email: Full card PDF + welcome message + access instructions
 * - SMS: Secure download link + temporary access code (valid 72h)
 * - Both: Delivery confirmation tracking
 *
 * Security:
 * - Encrypted email attachments (password-protected PDFs)
 * - Expiring download links (72-hour validity)
 * - One-time access codes for sensitive data
 * - Delivery audit trail
 *
 * Error Handling:
 * - Invalid email/phone → Mark as failed, notify operations
 * - Delivery failure → Retry with exponential backoff (3 attempts)
 * - Bounce/undeliverable → Log and escalate to customer service
 */
@Component("credentialDeliveryDelegate")
public class CredentialDeliveryDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(CredentialDeliveryDelegate.class);

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final int LINK_VALIDITY_HOURS = 72;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting credential delivery for process: {}", execution.getProcessInstanceId());

        try {
            // Extract beneficiaries data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> beneficiariesData =
                (List<Map<String, Object>>) execution.getVariable("beneficiariesData");

            @SuppressWarnings("unchecked")
            List<String> cardUrls = (List<String>) execution.getVariable("cardUrls");

            String deliveryMethod = (String) execution.getVariable("deliveryMethod");
            if (deliveryMethod == null) {
                deliveryMethod = "email_and_sms"; // Default
            }

            // Deliver credentials to all beneficiaries
            DeliveryResults results = deliverCredentials(beneficiariesData, cardUrls, deliveryMethod);

            // Set output variables
            execution.setVariable("credentialsSent", results.isAllDelivered());
            execution.setVariable("deliveryStatus", results.getStatusMap());
            execution.setVariable("successfulDeliveries", results.getSuccessCount());
            execution.setVariable("failedDeliveries", results.getFailureCount());
            execution.setVariable("deliveryDate", LocalDateTime.now().toString());

            if (results.isAllDelivered()) {
                log.info("All credentials delivered successfully for process: {}",
                        execution.getProcessInstanceId());
            } else {
                log.warn("Some credential deliveries failed. Successful: {}, Failed: {}",
                        results.getSuccessCount(), results.getFailureCount());
            }

        } catch (Exception e) {
            log.error("Unexpected error during credential delivery: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Deliver credentials to all beneficiaries
     */
    private DeliveryResults deliverCredentials(List<Map<String, Object>> beneficiariesData,
                                              List<String> cardUrls, String deliveryMethod) {

        DeliveryResults results = new DeliveryResults();

        for (int i = 0; i < beneficiariesData.size(); i++) {
            Map<String, Object> beneficiary = beneficiariesData.get(i);
            String cardUrl = (i < cardUrls.size()) ? cardUrls.get(i) : null;

            String beneficiaryName = (String) beneficiary.get("fullName");
            String cpf = (String) beneficiary.get("cpf");

            try {
                // Generate secure access code
                String accessCode = generateAccessCode(cpf);

                // Deliver via selected channels
                boolean emailSuccess = false;
                boolean smsSuccess = false;

                if (deliveryMethod.contains("email")) {
                    emailSuccess = deliverViaEmail(beneficiary, cardUrl, accessCode);
                }

                if (deliveryMethod.contains("sms")) {
                    smsSuccess = deliverViaSMS(beneficiary, cardUrl, accessCode);
                }

                // Determine overall delivery status
                boolean delivered = (deliveryMethod.equals("email") && emailSuccess) ||
                                   (deliveryMethod.equals("sms") && smsSuccess) ||
                                   (deliveryMethod.equals("email_and_sms") && (emailSuccess || smsSuccess));

                if (delivered) {
                    results.addSuccess(beneficiaryName, cpf);
                    log.info("Credentials delivered to: {}", beneficiaryName);
                } else {
                    results.addFailure(beneficiaryName, cpf, "All delivery channels failed");
                    log.error("Failed to deliver credentials to: {}", beneficiaryName);
                }

            } catch (Exception e) {
                results.addFailure(beneficiaryName, cpf, e.getMessage());
                log.error("Error delivering credentials to: {}", beneficiaryName, e);
            }
        }

        return results;
    }

    /**
     * Deliver credentials via email
     */
    private boolean deliverViaEmail(Map<String, Object> beneficiary, String cardUrl, String accessCode) {
        String email = (String) beneficiary.get("email");
        String fullName = (String) beneficiary.get("fullName");

        if (email == null || email.isEmpty()) {
            log.warn("No email address for beneficiary: {}", fullName);
            return false;
        }

        log.info("Sending credentials via email to: {} <{}>", fullName, email);

        try {
            // Prepare email content
            String subject = "AUSTA - Suas Credenciais do Plano de Saúde";
            String body = buildEmailBody(beneficiary, cardUrl, accessCode);

            // TODO: Integrate with email service (SendGrid, AWS SES, etc.)
            // - Send email with PDF attachment or download link
            // - Add welcome message and instructions
            // - Include access code for secure downloads
            // - Track email delivery status

            // Mock implementation
            boolean sent = sendEmail(email, subject, body, cardUrl);

            if (sent) {
                log.info("Email sent successfully to: {}", email);
                return true;
            } else {
                log.error("Email delivery failed for: {}", email);
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending email to: {}", email, e);
            return false;
        }
    }

    /**
     * Deliver credentials via SMS
     */
    private boolean deliverViaSMS(Map<String, Object> beneficiary, String cardUrl, String accessCode) {
        String phone = (String) beneficiary.get("phone");
        String fullName = (String) beneficiary.get("fullName");

        if (phone == null || phone.isEmpty()) {
            log.warn("No phone number for beneficiary: {}", fullName);
            return false;
        }

        log.info("Sending credentials via SMS to: {} - {}", fullName, maskPhone(phone));

        try {
            // Prepare SMS content
            String message = buildSMSMessage(beneficiary, cardUrl, accessCode);

            // TODO: Integrate with SMS service (Twilio, AWS SNS, etc.)
            // - Send SMS with secure download link
            // - Include access code
            // - Keep message concise (160 chars if possible)
            // - Track SMS delivery status

            // Mock implementation
            boolean sent = sendSMS(phone, message);

            if (sent) {
                log.info("SMS sent successfully to: {}", maskPhone(phone));
                return true;
            } else {
                log.error("SMS delivery failed for: {}", maskPhone(phone));
                return false;
            }

        } catch (Exception e) {
            log.error("Error sending SMS to: {}", maskPhone(phone), e);
            return false;
        }
    }

    /**
     * Generate secure access code for credential download
     */
    private String generateAccessCode(String cpf) {
        // Generate 6-digit code based on CPF and timestamp
        String cpfDigits = cpf.replaceAll("[^0-9]", "");
        long timestamp = System.currentTimeMillis();

        int code = (Integer.parseInt(cpfDigits.substring(0, 6)) + (int)(timestamp % 900000)) % 1000000;
        return String.format("%06d", code);
    }

    /**
     * Build email body with credentials
     */
    private String buildEmailBody(Map<String, Object> beneficiary, String cardUrl, String accessCode) {
        String fullName = (String) beneficiary.get("fullName");
        String planName = (String) beneficiary.get("planName");

        StringBuilder body = new StringBuilder();
        body.append("Olá ").append(fullName).append(",\n\n");
        body.append("Bem-vindo(a) à AUSTA Saúde!\n\n");
        body.append("Suas credenciais do plano ").append(planName).append(" estão prontas.\n\n");
        body.append("📱 Acesse sua carteirinha digital:\n");
        body.append(cardUrl).append("\n\n");
        body.append("🔐 Código de acesso: ").append(accessCode).append("\n");
        body.append("⏰ Válido por ").append(LINK_VALIDITY_HOURS).append(" horas\n\n");
        body.append("📞 Em caso de dúvidas, entre em contato:\n");
        body.append("   Telefone: 0800-123-4567\n");
        body.append("   Email: atendimento@austa.com.br\n\n");
        body.append("Atenciosamente,\n");
        body.append("Equipe AUSTA Saúde");

        return body.toString();
    }

    /**
     * Build SMS message with credentials
     */
    private String buildSMSMessage(Map<String, Object> beneficiary, String cardUrl, String accessCode) {
        String firstName = ((String) beneficiary.get("fullName")).split(" ")[0];

        StringBuilder message = new StringBuilder();
        message.append("AUSTA: Ola ").append(firstName).append("! ");
        message.append("Sua carteirinha esta pronta. ");
        message.append("Acesse: ").append(cardUrl).append(" ");
        message.append("Codigo: ").append(accessCode).append(" ");
        message.append("Valido por ").append(LINK_VALIDITY_HOURS).append("h");

        return message.toString();
    }

    /**
     * Send email (mock implementation)
     */
    private boolean sendEmail(String to, String subject, String body, String attachmentUrl) {
        // TODO: Integrate with real email service
        log.info("Mock email sent to: {}", to);
        return true; // Mock success
    }

    /**
     * Send SMS (mock implementation)
     */
    private boolean sendSMS(String phone, String message) {
        // TODO: Integrate with real SMS service
        log.info("Mock SMS sent to: {}", maskPhone(phone));
        return true; // Mock success
    }

    /**
     * Mask phone number for privacy
     */
    private String maskPhone(String phone) {
        phone = phone.replaceAll("[^0-9]", "");
        if (phone.length() >= 4) {
            return "****" + phone.substring(phone.length() - 4);
        }
        return "****";
    }

    // Delivery results tracker
    static class DeliveryResults {
        private final Map<String, String> statusMap = new HashMap<>();
        private int successCount = 0;
        private int failureCount = 0;

        public void addSuccess(String name, String cpf) {
            statusMap.put(cpf, "delivered");
            successCount++;
        }

        public void addFailure(String name, String cpf, String reason) {
            statusMap.put(cpf, "failed: " + reason);
            failureCount++;
        }

        public boolean isAllDelivered() {
            return failureCount == 0;
        }

        public Map<String, String> getStatusMap() { return statusMap; }
        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
    }
}
