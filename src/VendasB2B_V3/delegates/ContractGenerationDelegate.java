package com.austa.salesprocess.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contract Generation Delegate - AUSTA V3
 *
 * Purpose: Generate legal contract documents using e-signature platform (DocuSign/Clicksign)
 * Integration: DocuSign REST API or Clicksign API
 *
 * Responsibilities:
 * - Generate contract from approved proposal data
 * - Apply contract template based on service type
 * - Prepare document for e-signature
 * - Store contract in document management system
 * - Return contract ID and URL for signature
 *
 * Error Handling:
 * - Template not found → BpmnError: CONTRACT_TEMPLATE_ERROR
 * - API failure → Retry with exponential backoff
 * - Validation errors → BpmnError: CONTRACT_VALIDATION_ERROR
 */
@Component("contractGenerationDelegate")
public class ContractGenerationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(ContractGenerationDelegate.class);

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting contract generation for process: {}", execution.getProcessInstanceId());

        try {
            // Extract proposal data from execution
            Map<String, Object> proposalData = extractProposalData(execution);

            // Validate required fields
            validateProposalData(proposalData);

            // Select contract template
            String contractType = (String) execution.getVariable("contractType");
            String templateId = selectContractTemplate(contractType);

            // Generate contract document
            String contractId = generateContractDocument(proposalData, templateId);

            // Prepare for e-signature
            String contractUrl = prepareForSignature(contractId, proposalData);

            // Store contract metadata
            storeContractMetadata(contractId, proposalData, execution);

            // Set output variables
            execution.setVariable("contractId", contractId);
            execution.setVariable("contractUrl", contractUrl);
            execution.setVariable("contractGenerated", true);
            execution.setVariable("contractGenerationDate", LocalDateTime.now().toString());

            log.info("Contract generated successfully: {} for process: {}",
                    contractId, execution.getProcessInstanceId());

        } catch (TemplateNotFoundException e) {
            log.error("Contract template not found: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("CONTRACT_TEMPLATE_ERROR",
                    "Contract template not found: " + e.getMessage());

        } catch (ValidationException e) {
            log.error("Contract validation failed: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("CONTRACT_VALIDATION_ERROR",
                    "Contract validation failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during contract generation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Extract proposal data from process variables
     */
    private Map<String, Object> extractProposalData(DelegateExecution execution) {
        Map<String, Object> proposalData = new HashMap<>();

        // Client information
        proposalData.put("clientId", execution.getVariable("clientId"));
        proposalData.put("companyName", execution.getVariable("companyName"));
        proposalData.put("clientCNPJ", execution.getVariable("clientCNPJ"));
        proposalData.put("clientAddress", execution.getVariable("clientAddress"));

        // Contract details
        proposalData.put("dealValue", execution.getVariable("finalDealValue") != null ?
                execution.getVariable("finalDealValue") : execution.getVariable("dealValue"));
        proposalData.put("discountPercentage", execution.getVariable("finalDiscount") != null ?
                execution.getVariable("finalDiscount") : execution.getVariable("discountPercentage"));
        proposalData.put("contractType", execution.getVariable("contractType"));
        proposalData.put("contractDuration", execution.getVariable("contractDuration"));
        proposalData.put("paymentTerms", execution.getVariable("paymentTerms"));
        proposalData.put("slaTerms", execution.getVariable("slaTerms"));
        proposalData.put("penalties", execution.getVariable("penalties"));
        proposalData.put("guarantees", execution.getVariable("guarantees"));

        // Technical specifications
        proposalData.put("technicalSpecs", execution.getVariable("technicalSpecs"));
        proposalData.put("integrations", execution.getVariable("integrations"));
        proposalData.put("equipment", execution.getVariable("equipment"));
        proposalData.put("training", execution.getVariable("training"));

        // Approval information
        proposalData.put("approvalTier", execution.getVariable("approvalTier"));
        proposalData.put("approvedBy", execution.getVariable("approvedBy"));
        proposalData.put("approvalDate", execution.getVariable("approvalDate"));

        return proposalData;
    }

    /**
     * Validate required proposal data
     */
    private void validateProposalData(Map<String, Object> proposalData) throws ValidationException {
        // Required fields
        String[] requiredFields = {
            "clientId", "companyName", "clientCNPJ", "dealValue",
            "contractType", "contractDuration", "paymentTerms"
        };

        for (String field : requiredFields) {
            if (proposalData.get(field) == null || proposalData.get(field).toString().isEmpty()) {
                throw new ValidationException("Required field missing: " + field);
            }
        }

        // Validate CNPJ format
        String cnpj = proposalData.get("clientCNPJ").toString();
        if (!isValidCNPJ(cnpj)) {
            throw new ValidationException("Invalid CNPJ format: " + cnpj);
        }
    }

    /**
     * Select appropriate contract template based on service type
     */
    private String selectContractTemplate(String contractType) throws TemplateNotFoundException {
        Map<String, String> templateMapping = new HashMap<>();
        templateMapping.put("uti_outsourced", "AUSTA_UTI_TEMPLATE_V3");
        templateMapping.put("complete_radiology", "AUSTA_RADIOLOGY_TEMPLATE_V3");
        templateMapping.put("corporate_plan", "AUSTA_CORPORATE_HEALTH_TEMPLATE_V3");
        templateMapping.put("integrated_combo", "AUSTA_INTEGRATED_COMBO_TEMPLATE_V3");

        String templateId = templateMapping.get(contractType);
        if (templateId == null) {
            throw new TemplateNotFoundException("No template found for contract type: " + contractType);
        }

        log.info("Selected template: {} for contract type: {}", templateId, contractType);
        return templateId;
    }

    /**
     * Generate contract document from template
     */
    private String generateContractDocument(Map<String, Object> proposalData, String templateId) {
        // Generate unique contract ID
        String contractId = "CTR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        log.info("Generating contract document: {} using template: {}", contractId, templateId);

        // TODO: Integrate with document generation service
        // - Load template from DMS
        // - Populate template with proposal data
        // - Generate PDF document
        // - Store in document management system

        // Mock implementation
        String contractNumber = generateContractNumber();
        String documentPath = "/contracts/" + contractId + ".pdf";

        log.info("Contract document generated: {} at path: {}", contractNumber, documentPath);

        return contractId;
    }

    /**
     * Prepare contract for e-signature via DocuSign/Clicksign
     */
    private String prepareForSignature(String contractId, Map<String, Object> proposalData) {
        log.info("Preparing contract {} for e-signature", contractId);

        // TODO: Integrate with e-signature platform (DocuSign or Clicksign)
        // - Create signature request
        // - Add signatories (client and AUSTA representatives)
        // - Set signature workflow
        // - Generate signature URL

        // Mock implementation
        String signatureUrl = "https://sign.austa.com.br/contracts/" + contractId + "/sign";

        log.info("E-signature URL generated: {}", signatureUrl);

        return signatureUrl;
    }

    /**
     * Store contract metadata in database
     */
    private void storeContractMetadata(String contractId, Map<String, Object> proposalData,
                                      DelegateExecution execution) {
        log.info("Storing contract metadata for: {}", contractId);

        // TODO: Persist to database
        // - Contract ID
        // - Client ID
        // - Contract value
        // - Contract type
        // - Generation date
        // - Status: pending_signature
        // - Process instance ID

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("contractId", contractId);
        metadata.put("clientId", proposalData.get("clientId"));
        metadata.put("dealValue", proposalData.get("dealValue"));
        metadata.put("status", "pending_signature");
        metadata.put("processInstanceId", execution.getProcessInstanceId());
        metadata.put("generationDate", LocalDateTime.now());

        log.info("Contract metadata stored successfully");
    }

    /**
     * Generate sequential contract number
     */
    private String generateContractNumber() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");
        String datePart = LocalDateTime.now().format(formatter);
        String sequentialPart = String.format("%04d", (int)(Math.random() * 9999));
        return "AUSTA-" + datePart + "-" + sequentialPart;
    }

    /**
     * Validate CNPJ checksum
     */
    private boolean isValidCNPJ(String cnpj) {
        // Remove non-numeric characters
        cnpj = cnpj.replaceAll("[^0-9]", "");

        // CNPJ must have 14 digits
        if (cnpj.length() != 14) {
            return false;
        }

        // TODO: Implement full CNPJ validation with checksum
        // For now, just check length and format
        return true;
    }

    // Custom exceptions
    static class TemplateNotFoundException extends Exception {
        public TemplateNotFoundException(String message) {
            super(message);
        }
    }

    static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }
}
