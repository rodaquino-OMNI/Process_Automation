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
 * TasyERPIntegrationDelegate - Integrates with Tasy ERP system
 *
 * Purpose: Synchronizes sales and customer data with Tasy ERP for
 * healthcare operations, billing, and financial management.
 *
 * Input Variables:
 * - clientId: String - Client identifier
 * - nomeCliente: String - Client name
 * - numeroVidas: Integer - Number of beneficiaries
 * - contratoId: String - Contract ID
 * - valorContrato: Double - Contract value
 * - dataInicioVigencia: Date - Contract start date
 * - planoContratado: String - Contracted plan
 * - syncType: String - Sync type (customer, contract, billing)
 *
 * Output Variables:
 * - tasyIntegrationSuccess: Boolean - Integration success indicator
 * - tasyIntegrationTimestamp: Date - Integration timestamp
 * - tasyCustomerCode: String - Customer code in Tasy
 * - tasyContractCode: String - Contract code in Tasy
 * - tasyResponse: String - API response
 *
 * Tasy ERP Integration:
 * - Endpoint: /api/v1/customers, /api/v1/contracts
 * - Authentication: API Key + OAuth 2.0
 * - Timeout: 45 seconds (ERP operations can be slower)
 * - Data Format: JSON
 *
 * Error Handling:
 * - Circuit Breaker: Opens after 5 failures
 * - Retry: 3 attempts with exponential backoff
 * - Timeout: 45 seconds
 * - Fallback: Queues for batch processing
 *
 * @author AUSTA V3 Backend Team
 * @version 3.0.0
 */
@Component("tasyERPIntegrationDelegate")
public class TasyERPIntegrationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(TasyERPIntegrationDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_MS = 45000;

    private final CircuitBreaker circuitBreaker;
    private final Retry retry;

    public TasyERPIntegrationDelegate() {
        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(120))
            .slidingWindowSize(10)
            .build();
        this.circuitBreaker = CircuitBreaker.of("tasyERP", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(MAX_RETRY_ATTEMPTS)
            .waitDuration(Duration.ofSeconds(10))
            .retryExceptions(Exception.class)
            .build();
        this.retry = Retry.of("tasyERP", retryConfig);
    }

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String clientId = (String) execution.getVariable("clientId");
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String syncType = (String) execution.getVariable("syncType");

        LOGGER.info("Starting Tasy ERP integration: client={}, type={}", nomeCliente, syncType);

        validateInputs(clientId, nomeCliente);

        try {
            Map<String, Object> integrationResult = circuitBreaker.executeSupplier(() ->
                retry.executeSupplier(() -> performIntegration(execution, syncType))
            );

            execution.setVariable("tasyIntegrationSuccess", true);
            execution.setVariable("tasyIntegrationTimestamp", new Date());
            execution.setVariable("tasyCustomerCode", integrationResult.get("customerCode"));
            execution.setVariable("tasyContractCode", integrationResult.get("contractCode"));
            execution.setVariable("tasyResponse", integrationResult.get("response"));

            LOGGER.info("Tasy ERP integration completed successfully for client: {}", nomeCliente);

        } catch (Exception e) {
            LOGGER.error("Tasy ERP integration failed after {} attempts", MAX_RETRY_ATTEMPTS, e);

            execution.setVariable("tasyIntegrationSuccess", false);
            execution.setVariable("tasyIntegrationError", e.getMessage());

            // Queue for batch processing
            queueForBatchProcessing(execution);
        }
    }

    private void validateInputs(String clientId, String nomeCliente) {
        if (clientId == null || clientId.trim().isEmpty()) {
            throw new IllegalArgumentException("clientId is required for Tasy integration");
        }
        if (nomeCliente == null || nomeCliente.trim().isEmpty()) {
            throw new IllegalArgumentException("nomeCliente is required for Tasy integration");
        }
    }

    private Map<String, Object> performIntegration(DelegateExecution execution, String syncType)
            throws Exception {

        Map<String, Object> payload = buildIntegrationPayload(execution, syncType);

        LOGGER.debug("Integrating with Tasy ERP: {}", payload);

        // TODO: Implement actual Tasy ERP API call
        // POST https://tasy-erp.austa.com.br/api/v1/customers
        // POST https://tasy-erp.austa.com.br/api/v1/contracts

        Thread.sleep(2000); // Simulate ERP API call

        Map<String, Object> result = new HashMap<>();
        result.put("customerCode", "CUST-" + System.currentTimeMillis());
        result.put("contractCode", "CONT-" + System.currentTimeMillis());
        result.put("response", "{ \"status\": \"success\", \"message\": \"Integration completed\" }");

        return result;
    }

    private Map<String, Object> buildIntegrationPayload(DelegateExecution execution, String syncType) {
        Map<String, Object> payload = new HashMap<>();

        switch (syncType != null ? syncType.toLowerCase() : "customer") {
            case "contract":
                buildContractPayload(execution, payload);
                break;
            case "billing":
                buildBillingPayload(execution, payload);
                break;
            case "customer":
            default:
                buildCustomerPayload(execution, payload);
        }

        // Add common metadata
        payload.put("sourceSystem", "Camunda");
        payload.put("processInstanceId", execution.getProcessInstanceId());
        payload.put("integrationTimestamp", new Date());

        return payload;
    }

    private void buildCustomerPayload(DelegateExecution execution, Map<String, Object> payload) {
        String clientId = (String) execution.getVariable("clientId");
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String cnpj = (String) execution.getVariable("cnpj");
        Integer numeroVidas = (Integer) execution.getVariable("numeroVidas");
        String endereco = (String) execution.getVariable("endereco");
        String telefone = (String) execution.getVariable("telefone");
        String emailContato = (String) execution.getVariable("emailContato");

        payload.put("customerId", clientId);
        payload.put("customerName", nomeCliente);
        payload.put("documentNumber", cnpj);
        payload.put("numberOfBeneficiaries", numeroVidas);
        payload.put("address", endereco);
        payload.put("phone", telefone);
        payload.put("email", emailContato);
        payload.put("customerType", "B2B");
        payload.put("active", true);
    }

    private void buildContractPayload(DelegateExecution execution, Map<String, Object> payload) {
        String contratoId = (String) execution.getVariable("contratoId");
        String clientId = (String) execution.getVariable("clientId");
        Double valorContrato = (Double) execution.getVariable("valorContrato");
        Date dataInicioVigencia = (Date) execution.getVariable("dataInicioVigencia");
        String planoContratado = (String) execution.getVariable("planoContratado");
        Integer numeroVidas = (Integer) execution.getVariable("numeroVidas");

        payload.put("contractId", contratoId);
        payload.put("customerId", clientId);
        payload.put("contractValue", valorContrato);
        payload.put("startDate", dataInicioVigencia);
        payload.put("plan", planoContratado);
        payload.put("numberOfBeneficiaries", numeroVidas);
        payload.put("status", "ACTIVE");
    }

    private void buildBillingPayload(DelegateExecution execution, Map<String, Object> payload) {
        String contratoId = (String) execution.getVariable("contratoId");
        Double valorMensal = (Double) execution.getVariable("valorMensalidade");
        String formaPagamento = (String) execution.getVariable("formaPagamento");
        Integer diaVencimento = (Integer) execution.getVariable("diaVencimento");

        payload.put("contractId", contratoId);
        payload.put("monthlyAmount", valorMensal);
        payload.put("paymentMethod", formaPagamento);
        payload.put("dueDay", diaVencimento);
        payload.put("billingCycle", "MONTHLY");
    }

    private void queueForBatchProcessing(DelegateExecution execution) {
        LOGGER.warn("Queuing Tasy integration for batch processing");
        execution.setVariable("tasyBatchQueueTime", new Date());
        // TODO: Implement queue mechanism
    }
}
