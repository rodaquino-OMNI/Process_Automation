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
 * FinancialDataDelegate - Validates and processes financial data
 *
 * Purpose: Validates customer financial information, credit analysis,
 * and ensures compliance with financial regulations.
 *
 * Input Variables:
 * - cnpj: String - Company tax ID
 * - nomeCliente: String - Company name
 * - valorContrato: Double - Contract value
 * - formaPagamento: String - Payment method
 * - validationType: String - Validation type (credit, compliance, tax)
 *
 * Output Variables:
 * - financialValidationSuccess: Boolean - Validation success
 * - financialValidationTimestamp: Date - Validation timestamp
 * - creditScore: Integer - Credit score (0-1000)
 * - creditApproved: Boolean - Credit approval status
 * - complianceStatus: String - Compliance check result
 * - validationDetails: Map - Detailed validation results
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("financialDataDelegate")
public class FinancialDataDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(FinancialDataDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public FinancialDataDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("financialData", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("financialData", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String cnpj = (String) execution.getVariable("cnpj");
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String validationType = (String) execution.getVariable("validationType");

        LOGGER.info("Starting financial validation: client={}, type={}", nomeCliente, validationType);

        validateInputs(cnpj);

        try {
            Map<String, Object> validationResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> performValidation(execution, validationType))
            );

            execution.setVariable("financialValidationSuccess", true);
            execution.setVariable("financialValidationTimestamp", new Date());
            execution.setVariable("creditScore", validationResult.get("creditScore"));
            execution.setVariable("creditApproved", validationResult.get("creditApproved"));
            execution.setVariable("complianceStatus", validationResult.get("complianceStatus"));
            execution.setVariable("validationDetails", validationResult.get("details"));

            LOGGER.info("Financial validation completed: credit approved = {}",
                       validationResult.get("creditApproved"));

        } catch (Exception e) {
            LOGGER.error("Financial validation failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("financialValidationSuccess", false);
            execution.setVariable("financialValidationError", e.getMessage());
            execution.setVariable("creditApproved", false);
        }
    }

    private void validateInputs(String cnpj) {
        if (cnpj == null || cnpj.trim().isEmpty()) {
            throw new IllegalArgumentException("CNPJ is required for financial validation");
        }
        if (!isValidCNPJ(cnpj)) {
            throw new IllegalArgumentException("Invalid CNPJ format");
        }
    }

    private boolean isValidCNPJ(String cnpj) {
        // Remove non-digits
        String cleanCNPJ = cnpj.replaceAll("[^0-9]", "");
        return cleanCNPJ.length() == 14;
    }

    private Map<String, Object> performValidation(DelegateExecution execution, String validationType)
            throws Exception {

        LOGGER.debug("Performing {} validation", validationType);

        // TODO: Implement actual financial API integrations:
        // - Serasa Experian API for credit check
        // - Receita Federal API for tax compliance
        // - ANS API for healthcare regulatory compliance

        Thread.sleep(1500); // Simulate API calls

        Map<String, Object> result = new HashMap<>();

        switch (validationType != null ? validationType.toLowerCase() : "credit") {
            case "credit":
                performCreditCheck(execution, result);
                break;
            case "compliance":
                performComplianceCheck(execution, result);
                break;
            case "tax":
                performTaxValidation(execution, result);
                break;
            default:
                performCreditCheck(execution, result);
                performComplianceCheck(execution, result);
        }

        return result;
    }

    private void performCreditCheck(DelegateExecution execution, Map<String, Object> result) {
        Double valorContrato = (Double) execution.getVariable("valorContrato");

        // Simulate credit scoring
        int creditScore = (int) (Math.random() * 300 + 700); // 700-1000
        boolean creditApproved = creditScore >= 750 &&
                                (valorContrato == null || valorContrato <= 5000000);

        result.put("creditScore", creditScore);
        result.put("creditApproved", creditApproved);
        result.put("creditRating", calculateCreditRating(creditScore));

        Map<String, Object> details = new HashMap<>();
        details.put("paymentHistory", "Good");
        details.put("debtRatio", 0.35);
        details.put("creditLimit", 10000000.0);
        result.put("details", details);
    }

    private void performComplianceCheck(DelegateExecution execution, Map<String, Object> result) {
        // Simulate compliance checks
        boolean ansCompliant = true;
        boolean taxCompliant = true;
        boolean regulatoryCompliant = true;

        String complianceStatus = (ansCompliant && taxCompliant && regulatoryCompliant)
            ? "COMPLIANT" : "NON_COMPLIANT";

        result.put("complianceStatus", complianceStatus);
        result.put("ansCompliant", ansCompliant);
        result.put("taxCompliant", taxCompliant);
        result.put("regulatoryCompliant", regulatoryCompliant);
    }

    private void performTaxValidation(DelegateExecution execution, Map<String, Object> result) {
        String cnpj = (String) execution.getVariable("cnpj");

        // Simulate tax validation
        boolean taxRegularized = true;
        boolean activeRegistration = true;

        result.put("taxRegularized", taxRegularized);
        result.put("activeRegistration", activeRegistration);
        result.put("taxStatus", "REGULAR");
    }

    private String calculateCreditRating(int score) {
        if (score >= 900) return "AAA";
        if (score >= 850) return "AA";
        if (score >= 800) return "A";
        if (score >= 750) return "BBB";
        if (score >= 700) return "BB";
        return "B";
    }
}
