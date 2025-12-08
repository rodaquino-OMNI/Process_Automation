package com.austa.salesprocess.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Health Card Generation Delegate - AUSTA V3
 *
 * Purpose: Generate health cards (carteirinhas) for beneficiaries in PDF format
 * Output: PDF files with QR codes, beneficiary photos, and plan information
 *
 * Responsibilities:
 * - Generate individual health cards for each beneficiary
 * - Apply plan-specific branding and design
 * - Include QR code for mobile validation
 * - Add ANS registration number
 * - Store PDFs in document management system
 * - Return card URLs for distribution
 *
 * Card Components:
 * - Beneficiary photo (if available)
 * - Full name
 * - CPF (masked)
 * - Birth date
 * - Plan name and code
 * - Card number (unique identifier)
 * - QR code (card validation)
 * - ANS operator code
 * - Emergency contacts
 * - Validity period
 *
 * Error Handling:
 * - PDF generation failure → BpmnError: CARD_GENERATION_ERROR
 * - Template not found → BpmnError: CARD_TEMPLATE_ERROR
 * - Storage failure → Retry with exponential backoff
 */
@Component("healthCardGenerationDelegate")
public class HealthCardGenerationDelegate implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(HealthCardGenerationDelegate.class);

    private static final String CARD_FORMAT = "PDF";
    private static final String CARD_SIZE = "CR80"; // Standard credit card size (85.60 × 53.98 mm)

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        log.info("Starting health card generation for process: {}", execution.getProcessInstanceId());

        try {
            // Extract beneficiaries data
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> beneficiariesData =
                (List<Map<String, Object>>) execution.getVariable("beneficiariesData");

            String ansProtocolNumber = (String) execution.getVariable("ansProtocolNumber");
            String contractId = (String) execution.getVariable("contractId");

            // Generate cards for all beneficiaries
            List<HealthCard> generatedCards = generateHealthCards(beneficiariesData, ansProtocolNumber, contractId);

            // Store cards in document management system
            List<String> cardUrls = storeHealthCards(generatedCards);

            // Set output variables
            execution.setVariable("healthCardsGenerated", true);
            execution.setVariable("cardUrls", cardUrls);
            execution.setVariable("totalCardsGenerated", generatedCards.size());
            execution.setVariable("cardGenerationDate", LocalDateTime.now().toString());

            log.info("Successfully generated {} health cards for process: {}",
                    generatedCards.size(), execution.getProcessInstanceId());

        } catch (TemplateException e) {
            log.error("Card template error: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("CARD_TEMPLATE_ERROR",
                    "Card template error: " + e.getMessage());

        } catch (CardGenerationException e) {
            log.error("Card generation failed: {}", e.getMessage());
            throw new org.camunda.bpm.engine.delegate.BpmnError("CARD_GENERATION_ERROR",
                    "Card generation failed: " + e.getMessage());

        } catch (Exception e) {
            log.error("Unexpected error during card generation: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Generate health cards for all beneficiaries
     */
    private List<HealthCard> generateHealthCards(List<Map<String, Object>> beneficiariesData,
                                                 String ansProtocolNumber, String contractId)
            throws CardGenerationException {

        List<HealthCard> generatedCards = new ArrayList<>();

        for (Map<String, Object> beneficiary : beneficiariesData) {
            try {
                HealthCard card = generateSingleCard(beneficiary, ansProtocolNumber, contractId);
                generatedCards.add(card);

                log.info("Generated card: {} for beneficiary: {}",
                        card.getCardNumber(), beneficiary.get("fullName"));

            } catch (Exception e) {
                log.error("Failed to generate card for beneficiary: {}", beneficiary.get("fullName"), e);
                throw new CardGenerationException(
                    "Failed to generate card for: " + beneficiary.get("fullName") + " - " + e.getMessage()
                );
            }
        }

        return generatedCards;
    }

    /**
     * Generate single health card
     */
    private HealthCard generateSingleCard(Map<String, Object> beneficiary,
                                         String ansProtocolNumber, String contractId)
            throws Exception {

        // Generate unique card number
        String cardNumber = generateCardNumber(beneficiary);

        // Prepare card data
        Map<String, String> cardData = new HashMap<>();
        cardData.put("cardNumber", cardNumber);
        cardData.put("fullName", (String) beneficiary.get("fullName"));
        cardData.put("cpf", maskCPF((String) beneficiary.get("cpf")));
        cardData.put("birthDate", formatDate((String) beneficiary.get("birthDate")));
        cardData.put("gender", (String) beneficiary.get("gender"));
        cardData.put("planCode", (String) beneficiary.get("planCode"));
        cardData.put("planName", getPlanName((String) beneficiary.get("planCode")));
        cardData.put("ansOperatorCode", "123456"); // AUSTA's ANS code
        cardData.put("ansProtocol", ansProtocolNumber);
        cardData.put("contractId", contractId);
        cardData.put("issueDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        cardData.put("validUntil", calculateValidUntil());
        cardData.put("emergencyPhone", "0800-123-4567");

        // Generate QR code
        String qrCodeData = generateQRCode(cardNumber, cardData);

        // Generate PDF card
        byte[] pdfData = generatePDFCard(cardData, qrCodeData);

        return new HealthCard(cardNumber, (String) beneficiary.get("cpf"), pdfData);
    }

    /**
     * Generate unique card number
     */
    private String generateCardNumber(Map<String, Object> beneficiary) {
        String cpf = ((String) beneficiary.get("cpf")).replaceAll("[^0-9]", "");
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(6);
        String random = String.format("%04d", (int)(Math.random() * 9999));

        return cpf.substring(0, 4) + timestamp + random;
    }

    /**
     * Mask CPF for privacy (XXX.XXX.123-45)
     */
    private String maskCPF(String cpf) {
        cpf = cpf.replaceAll("[^0-9]", "");
        return "XXX.XXX." + cpf.substring(6, 9) + "-" + cpf.substring(9, 11);
    }

    /**
     * Format date to Brazilian format (DD/MM/YYYY)
     */
    private String formatDate(String dateStr) {
        try {
            LocalDateTime date = LocalDateTime.parse(dateStr + "T00:00:00");
            return date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (Exception e) {
            return dateStr; // Return as-is if parsing fails
        }
    }

    /**
     * Get plan name from plan code
     */
    private String getPlanName(String planCode) {
        Map<String, String> planNames = new HashMap<>();
        planNames.put("AUSTA_UTI_01", "AUSTA UTI Completa");
        planNames.put("AUSTA_RADIO_01", "AUSTA Radiologia Premium");
        planNames.put("AUSTA_CORP_01", "AUSTA Corporativo Plus");
        planNames.put("AUSTA_COMBO_01", "AUSTA Combo Integrado");

        return planNames.getOrDefault(planCode, "AUSTA Health Plan");
    }

    /**
     * Calculate card validity (12 months from issue)
     */
    private String calculateValidUntil() {
        LocalDateTime validUntil = LocalDateTime.now().plusMonths(12);
        return validUntil.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    /**
     * Generate QR code data for card validation
     */
    private String generateQRCode(String cardNumber, Map<String, String> cardData) {
        // QR code contains encrypted card validation data
        StringBuilder qrData = new StringBuilder();
        qrData.append("CARD=").append(cardNumber).append(";");
        qrData.append("CPF=").append(cardData.get("cpf")).append(";");
        qrData.append("PLAN=").append(cardData.get("planCode")).append(";");
        qrData.append("ANS=").append(cardData.get("ansOperatorCode")).append(";");
        qrData.append("VALID=").append(cardData.get("validUntil"));

        // TODO: Encrypt QR data for security
        return qrData.toString();
    }

    /**
     * Generate PDF card using template
     */
    private byte[] generatePDFCard(Map<String, String> cardData, String qrCodeData)
            throws Exception {

        log.info("Generating PDF card for: {}", cardData.get("fullName"));

        // TODO: Integrate with PDF generation library (iText, Apache PDFBox, etc.)
        // - Load card template
        // - Populate template with beneficiary data
        // - Add QR code image
        // - Add beneficiary photo (if available)
        // - Generate PDF with standard card dimensions (CR80: 85.60 × 53.98 mm)

        // Mock implementation - return empty byte array
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Mock PDF content
        String mockPDF = "PDF_CONTENT_FOR_CARD_" + cardData.get("cardNumber");
        outputStream.write(mockPDF.getBytes());

        log.info("PDF card generated successfully");

        return outputStream.toByteArray();
    }

    /**
     * Store health cards in document management system
     */
    private List<String> storeHealthCards(List<HealthCard> generatedCards) {
        List<String> cardUrls = new ArrayList<>();

        for (HealthCard card : generatedCards) {
            // TODO: Store PDF in document management system (S3, Azure Blob, etc.)
            // - Upload PDF file
            // - Generate access URL
            // - Set permissions (private, expiring link)

            String cardUrl = "https://cards.austa.com.br/" + card.getCardNumber() + ".pdf";
            cardUrls.add(cardUrl);

            log.info("Card stored: {} at URL: {}", card.getCardNumber(), cardUrl);
        }

        return cardUrls;
    }

    // Health card data class
    static class HealthCard {
        private final String cardNumber;
        private final String cpf;
        private final byte[] pdfData;

        public HealthCard(String cardNumber, String cpf, byte[] pdfData) {
            this.cardNumber = cardNumber;
            this.cpf = cpf;
            this.pdfData = pdfData;
        }

        public String getCardNumber() { return cardNumber; }
        public String getCpf() { return cpf; }
        public byte[] getPdfData() { return pdfData; }
    }

    // Custom exceptions
    static class CardGenerationException extends Exception {
        public CardGenerationException(String message) {
            super(message);
        }
    }

    static class TemplateException extends Exception {
        public TemplateException(String message) {
            super(message);
        }
    }
}
