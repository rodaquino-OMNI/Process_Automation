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
 * AccountingPostingDelegate - Posts transactions to accounting system
 *
 * Purpose: Creates accounting entries for revenue recognition, accounts
 * receivable, and financial reporting in the general ledger.
 *
 * Input Variables:
 * - contratoId: String - Contract ID
 * - valorContrato: Double - Contract value
 * - dataInicioVigencia: Date - Contract start date
 * - postingType: String - Posting type (revenue, receivable, deferred)
 * - accountingPeriod: String - Accounting period (YYYY-MM)
 *
 * Output Variables:
 * - accountingPostingSuccess: Boolean - Posting success indicator
 * - accountingPostingTimestamp: Date - Posting timestamp
 * - journalEntryId: String - Journal entry ID
 * - ledgerEntries: List<Map> - List of ledger entries created
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("accountingPostingDelegate")
public class AccountingPostingDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccountingPostingDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 30000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public AccountingPostingDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(60))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("accountingPosting", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(5))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("accountingPosting", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String contratoId = (String) execution.getVariable("contratoId");
        Double valorContrato = (Double) execution.getVariable("valorContrato");
        String postingType = (String) execution.getVariable("postingType");

        LOGGER.info("Posting to accounting: contract={}, amount={}, type={}",
                    contratoId, valorContrato, postingType);

        validateInputs(contratoId, valorContrato);

        try {
            Map<String, Object> postingResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> postToAccounting(execution, postingType))
            );

            execution.setVariable("accountingPostingSuccess", true);
            execution.setVariable("accountingPostingTimestamp", new Date());
            execution.setVariable("journalEntryId", postingResult.get("journalEntryId"));
            execution.setVariable("ledgerEntries", postingResult.get("ledgerEntries"));

            LOGGER.info("Accounting posting completed: journal entry={}",
                       postingResult.get("journalEntryId"));

        } catch (Exception e) {
            LOGGER.error("Accounting posting failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("accountingPostingSuccess", false);
            execution.setVariable("accountingPostingError", e.getMessage());
        }
    }

    private void validateInputs(String contratoId, Double valorContrato) {
        if (contratoId == null || contratoId.trim().isEmpty()) {
            throw new IllegalArgumentException("contratoId is required");
        }
        if (valorContrato == null || valorContrato <= 0) {
            throw new IllegalArgumentException("valorContrato must be greater than 0");
        }
    }

    private Map<String, Object> postToAccounting(DelegateExecution execution, String postingType)
            throws Exception {

        List<Map<String, Object>> ledgerEntries = createLedgerEntries(execution, postingType);

        LOGGER.debug("Creating accounting entries: {}", ledgerEntries);

        // TODO: Implement actual accounting system API call
        // POST /api/v1/accounting/journal-entries

        Thread.sleep(1500); // Simulate API call

        String journalEntryId = "JE-" + System.currentTimeMillis();

        Map<String, Object> result = new HashMap<>();
        result.put("journalEntryId", journalEntryId);
        result.put("ledgerEntries", ledgerEntries);

        return result;
    }

    private List<Map<String, Object>> createLedgerEntries(DelegateExecution execution,
                                                          String postingType) {
        List<Map<String, Object>> entries = new ArrayList<>();
        Double valorContrato = (Double) execution.getVariable("valorContrato");

        switch (postingType != null ? postingType.toLowerCase() : "revenue") {
            case "revenue":
                entries.addAll(createRevenueEntries(valorContrato));
                break;
            case "receivable":
                entries.addAll(createReceivableEntries(valorContrato));
                break;
            case "deferred":
                entries.addAll(createDeferredRevenueEntries(valorContrato));
                break;
            default:
                entries.addAll(createRevenueEntries(valorContrato));
        }

        return entries;
    }

    private List<Map<String, Object>> createRevenueEntries(Double amount) {
        List<Map<String, Object>> entries = new ArrayList<>();

        // Debit: Accounts Receivable
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("account", "1.1.01.001"); // Accounts Receivable
        debitEntry.put("accountName", "Contas a Receber");
        debitEntry.put("debit", amount);
        debitEntry.put("credit", 0.0);
        entries.add(debitEntry);

        // Credit: Revenue
        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("account", "4.1.01.001"); // Healthcare Services Revenue
        creditEntry.put("accountName", "Receita de Serviços de Saúde");
        creditEntry.put("debit", 0.0);
        creditEntry.put("credit", amount);
        entries.add(creditEntry);

        return entries;
    }

    private List<Map<String, Object>> createReceivableEntries(Double amount) {
        return createRevenueEntries(amount);
    }

    private List<Map<String, Object>> createDeferredRevenueEntries(Double amount) {
        List<Map<String, Object>> entries = new ArrayList<>();

        // Debit: Cash/Bank
        Map<String, Object> debitEntry = new HashMap<>();
        debitEntry.put("account", "1.1.01.002"); // Cash
        debitEntry.put("accountName", "Caixa e Bancos");
        debitEntry.put("debit", amount);
        debitEntry.put("credit", 0.0);
        entries.add(debitEntry);

        // Credit: Deferred Revenue
        Map<String, Object> creditEntry = new HashMap<>();
        creditEntry.put("account", "2.1.01.003"); // Deferred Revenue
        creditEntry.put("accountName", "Receita Diferida");
        creditEntry.put("debit", 0.0);
        creditEntry.put("credit", amount);
        entries.add(creditEntry);

        return entries;
    }
}
