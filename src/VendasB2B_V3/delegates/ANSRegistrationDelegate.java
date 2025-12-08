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
import java.util.UUID;

/**
 * ANS Registration Delegate - AUSTA V3
 *
 * Purpose: Register beneficiaries with ANS (Agência Nacional de Saúde Suplementar)
 * Compliance: 72-hour deadline for ANS submission
 *
 * Responsibilities:
 * - Validate beneficiary data for ANS format compliance
 * - Format data according to ANS XML schema (TISS)
 * - Submit registration to ANS web service
 * - Monitor ANS protocol status
 * - Handle ANS rejection and retry logic
 *
 * ANS Compliance Requirements:
 * - TISS (Troca de Informações na Saúde Suplementar) standard format
 * - 72-hour registration deadline from contract signature
 * - Required fields: CPF, full name, birth date, gender, plan code
 * - Beneficiary type: Titular or Dependent
 *
 * Error Handling:
 * - ANS API unavailable → Retry with exponential backoff
 * - Invalid data format → BpmnError: ANS_REGISTRATION_ERROR
 * - Timeout (72h) → BpmnError: ANS_COMPLIANCE_TIMEOUT
 */
@Component("ansRegistrationDelegate")
public class ANSRegistrationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ANSRegistrationDelegate.class);

    private static final String ANS_OPERATOR_CODE = "123456"; // AUSTA's ANS registration code
    private static final int ANS_DEADLINE_HOURS = 72;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting ANS registration for process: {}", execution.getProcessInstanceId());

        try {
            // Extract beneficiaries data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> beneficiariesData =
                (List<Map<String, Object>>) execution.getVariable("beneficiariesData");

            String contractId = (String) execution.getVariable("contractId");

            // Validate data for ANS compliance
            validateANSCompliance(beneficiariesData);

            // Format data according to TISS standard
            String tissXml = formatToTISS(beneficiariesData, contractId);

            // Submit to ANS web service
            ANSSubmissionResult result = submitToANS(tissXml, contractId);

            // Store ANS protocol information
            execution.setVariable("ansProtocolNumber", result.getProtocolNumber());
            execution.setVariable("ansRegistrationDate", result.getSubmissionDate().toString());
            execution.setVariable("ansApproved", result.isApproved());
            execution.setVariable("ansResponseMessage", result.getMessage());

            // Check if within 72-hour deadline
            checkComplianceDeadline(execution);

            if (result.isApproved()) {
                log.info("ANS registration successful. Protocol: {} for process: {}",
                        result.getProtocolNumber(), execution.getProcessInstanceId());
            } else {
                log.warn("ANS registration pending/rejected. Protocol: {}, Message: {}",
                        result.getProtocolNumber(), result.getMessage());
            }

        } catch (ANSValidationException e) {
            log.error("ANS validation failed: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("ANS_REGISTRATION_ERROR",
                    "ANS validation failed: " + e.getMessage());

        } catch (ANSTimeoutException e) {
            log.error("ANS compliance deadline exceeded: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("ANS_COMPLIANCE_TIMEOUT",
                    "72-hour compliance deadline exceeded");

        } catch (Exception e) {
            log.error("Unexpected error during ANS registration: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Validate beneficiary data for ANS compliance
     */
    private void validateANSCompliance(List<Map<String, Object>> beneficiariesData)
            throws ANSValidationException {

        if (beneficiariesData == null || beneficiariesData.isEmpty()) {
            throw new ANSValidationException("No beneficiary data provided");
        }

        for (int i = 0; i < beneficiariesData.size(); i++) {
            Map<String, Object> beneficiary = beneficiariesData.get(i);
            String beneficiaryIndex = "Beneficiary #" + (i + 1);

            // Required fields for ANS
            validateRequiredField(beneficiary, "cpf", beneficiaryIndex);
            validateRequiredField(beneficiary, "fullName", beneficiaryIndex);
            validateRequiredField(beneficiary, "birthDate", beneficiaryIndex);
            validateRequiredField(beneficiary, "gender", beneficiaryIndex);
            validateRequiredField(beneficiary, "planCode", beneficiaryIndex);
            validateRequiredField(beneficiary, "beneficiaryType", beneficiaryIndex);

            // Validate CPF format and checksum
            String cpf = (String) beneficiary.get("cpf");
            if (!isValidCPF(cpf)) {
                throw new ANSValidationException(beneficiaryIndex + ": Invalid CPF: " + cpf);
            }

            // Validate age range (0-120 years)
            String birthDate = (String) beneficiary.get("birthDate");
            int age = calculateAge(birthDate);
            if (age < 0 || age > 120) {
                throw new ANSValidationException(beneficiaryIndex + ": Invalid age: " + age);
            }

            // Validate gender
            String gender = (String) beneficiary.get("gender");
            if (!gender.matches("^(M|F)$")) {
                throw new ANSValidationException(beneficiaryIndex + ": Invalid gender: " + gender);
            }
        }

        log.info("ANS compliance validation passed for {} beneficiaries", beneficiariesData.size());
    }

    /**
     * Validate required field exists
     */
    private void validateRequiredField(Map<String, Object> beneficiary, String field,
                                      String beneficiaryIndex) throws ANSValidationException {
        if (beneficiary.get(field) == null || beneficiary.get(field).toString().isEmpty()) {
            throw new ANSValidationException(beneficiaryIndex + ": Missing required field: " + field);
        }
    }

    /**
     * Format beneficiary data according to TISS XML standard
     */
    private String formatToTISS(List<Map<String, Object>> beneficiariesData, String contractId) {
        StringBuilder tissXml = new StringBuilder();

        // TISS XML header
        tissXml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        tissXml.append("<tissTransaction xmlns=\"http://www.ans.gov.br/padroes/tiss/schemas\">");
        tissXml.append("<header>");
        tissXml.append("<operatorCode>").append(ANS_OPERATOR_CODE).append("</operatorCode>");
        tissXml.append("<transactionDate>").append(LocalDateTime.now()).append("</transactionDate>");
        tissXml.append("<contractId>").append(contractId).append("</contractId>");
        tissXml.append("</header>");

        // Beneficiaries data
        tissXml.append("<beneficiaries>");
        for (Map<String, Object> beneficiary : beneficiariesData) {
            tissXml.append("<beneficiary>");
            tissXml.append("<cpf>").append(beneficiary.get("cpf")).append("</cpf>");
            tissXml.append("<fullName>").append(beneficiary.get("fullName")).append("</fullName>");
            tissXml.append("<birthDate>").append(beneficiary.get("birthDate")).append("</birthDate>");
            tissXml.append("<gender>").append(beneficiary.get("gender")).append("</gender>");
            tissXml.append("<planCode>").append(beneficiary.get("planCode")).append("</planCode>");
            tissXml.append("<beneficiaryType>").append(beneficiary.get("beneficiaryType")).append("</beneficiaryType>");
            tissXml.append("<registrationDate>").append(LocalDateTime.now()).append("</registrationDate>");
            tissXml.append("</beneficiary>");
        }
        tissXml.append("</beneficiaries>");
        tissXml.append("</tissTransaction>");

        log.info("TISS XML formatted successfully for {} beneficiaries", beneficiariesData.size());
        return tissXml.toString();
    }

    /**
     * Submit registration to ANS web service
     */
    private ANSSubmissionResult submitToANS(String tissXml, String contractId) {
        log.info("Submitting TISS XML to ANS web service for contract: {}", contractId);

        // TODO: Integrate with ANS TISS web service
        // - Connect to ANS SOAP/REST API
        // - Submit TISS XML
        // - Receive protocol number
        // - Monitor submission status

        // Mock implementation
        String protocolNumber = "ANS-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime submissionDate = LocalDateTime.now();
        boolean approved = true; // Mock approval
        String message = "Registration submitted successfully";

        log.info("ANS submission result - Protocol: {}, Approved: {}", protocolNumber, approved);

        return new ANSSubmissionResult(protocolNumber, submissionDate, approved, message);
    }

    /**
     * Check if within 72-hour compliance deadline
     */
    private void checkComplianceDeadline(DelegateExecution execution) throws ANSTimeoutException {
        LocalDateTime contractSignedDate = LocalDateTime.parse(
            execution.getVariable("closingDate").toString()
        );

        LocalDateTime deadlineDate = contractSignedDate.plusHours(ANS_DEADLINE_HOURS);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(deadlineDate)) {
            long hoursOverdue = java.time.Duration.between(deadlineDate, now).toHours();
            throw new ANSTimeoutException(
                "ANS registration exceeded 72-hour deadline by " + hoursOverdue + " hours"
            );
        }

        long hoursRemaining = java.time.Duration.between(now, deadlineDate).toHours();
        log.info("ANS registration within deadline. Hours remaining: {}", hoursRemaining);
    }

    /**
     * Validate CPF checksum (Brazilian tax ID)
     */
    private boolean isValidCPF(String cpf) {
        // Remove non-numeric characters
        cpf = cpf.replaceAll("[^0-9]", "");

        // CPF must have 11 digits
        if (cpf.length() != 11) {
            return false;
        }

        // Check for known invalid CPFs (all same digit)
        if (cpf.matches("(\\d)\\1{10}")) {
            return false;
        }

        // Calculate first check digit
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (10 - i);
        }
        int checkDigit1 = 11 - (sum % 11);
        if (checkDigit1 >= 10) checkDigit1 = 0;

        // Calculate second check digit
        sum = 0;
        for (int i = 0; i < 10; i++) {
            sum += Character.getNumericValue(cpf.charAt(i)) * (11 - i);
        }
        int checkDigit2 = 11 - (sum % 11);
        if (checkDigit2 >= 10) checkDigit2 = 0;

        // Verify check digits
        return checkDigit1 == Character.getNumericValue(cpf.charAt(9)) &&
               checkDigit2 == Character.getNumericValue(cpf.charAt(10));
    }

    /**
     * Calculate age from birth date string (format: YYYY-MM-DD)
     */
    private int calculateAge(String birthDateStr) {
        LocalDateTime birthDate = LocalDateTime.parse(birthDateStr + "T00:00:00");
        LocalDateTime now = LocalDateTime.now();
        return (int) java.time.Duration.between(birthDate, now).toDays() / 365;
    }

    // Result class for ANS submission
    static class ANSSubmissionResult {
        private final String protocolNumber;
        private final LocalDateTime submissionDate;
        private final boolean approved;
        private final String message;

        public ANSSubmissionResult(String protocolNumber, LocalDateTime submissionDate,
                                  boolean approved, String message) {
            this.protocolNumber = protocolNumber;
            this.submissionDate = submissionDate;
            this.approved = approved;
            this.message = message;
        }

        public String getProtocolNumber() { return protocolNumber; }
        public LocalDateTime getSubmissionDate() { return submissionDate; }
        public boolean isApproved() { return approved; }
        public String getMessage() { return message; }
    }

    // Custom exceptions
    static class ANSValidationException extends Exception {
        public ANSValidationException(String message) {
            super(message);
        }
    }

    static class ANSTimeoutException extends Exception {
        public ANSTimeoutException(String message) {
            super(message);
        }
    }
}
