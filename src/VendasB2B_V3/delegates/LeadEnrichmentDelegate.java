package com.austa.vendas.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * LeadEnrichmentDelegate - Enriches lead data from external CRM sources
 *
 * Purpose: Automatically fetch and enrich lead information from CRM (Salesforce/HubSpot),
 * company databases, and external data sources to provide comprehensive context
 * for sales qualification.
 *
 * Input Variables:
 * - nomeCliente: String - Client company name
 * - cnpj: String - Brazilian company tax ID
 * - tipoPesquisa: String - Type of research (completa, basica, express)
 *
 * Output Variables:
 * - dadosFinanceiros: Map - Financial data (revenue, employees, growth rate)
 * - estruturaDecisao: Map - Decision-making structure
 * - desafiosIdentificados: List<String> - Identified business challenges
 * - concorrentes: List<String> - Current competitors/suppliers
 * - iniciativasEstrategicas: List<String> - Strategic initiatives
 * - fitScore: Double - Backward compatibility fit score for deals <$500K
 *
 * Error Handling:
 * - Retry: 3 attempts with 5-minute intervals (R3/PT5M)
 * - Fallback: Returns partial data with reduced confidence score
 * - Timeout: 2 minutes maximum execution time
 *
 * @author AUSTA V3 Hive Mind - Coder Agent
 * @version 3.0.0
 */
@Component("leadEnrichmentDelegate")
public class LeadEnrichmentDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(LeadEnrichmentDelegate.class);
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long TIMEOUT_SECONDS = 120;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String cnpj = (String) execution.getVariable("cnpj");
        String tipoPesquisa = (String) execution.getVariable("tipoPesquisa");

        LOGGER.info("Starting lead enrichment for client: {} (CNPJ: {}), type: {}",
                    nomeCliente, cnpj, tipoPesquisa);

        int attempt = getAttemptNumber(execution);

        try {
            // Execute enrichment with timeout protection
            Map<String, Object> enrichedData = executeWithTimeout(() -> {
                return enrichLeadData(nomeCliente, cnpj, tipoPesquisa);
            }, TIMEOUT_SECONDS);

            // Set output variables
            execution.setVariable("dadosFinanceiros", enrichedData.get("financialData"));
            execution.setVariable("estruturaDecisao", enrichedData.get("decisionStructure"));
            execution.setVariable("desafiosIdentificados", enrichedData.get("challenges"));
            execution.setVariable("concorrentes", enrichedData.get("competitors"));
            execution.setVariable("iniciativasEstrategicas", enrichedData.get("strategicInitiatives"));
            execution.setVariable("fitScore", enrichedData.get("fitScore"));
            execution.setVariable("leadEnrichmentSuccess", true);
            execution.setVariable("enrichmentConfidence", enrichedData.get("confidence"));

            LOGGER.info("Lead enrichment completed successfully for client: {}", nomeCliente);

        } catch (Exception e) {
            LOGGER.error("Lead enrichment failed for client: {} (Attempt {}/{})",
                        nomeCliente, attempt, MAX_RETRY_ATTEMPTS, e);

            if (attempt < MAX_RETRY_ATTEMPTS) {
                // Will retry via Camunda retry mechanism
                execution.setVariable("leadEnrichmentAttempt", attempt + 1);
                throw new RuntimeException("Lead enrichment failed, retrying...", e);
            } else {
                // Max retries reached, provide fallback data
                LOGGER.warn("Max retry attempts reached. Providing fallback data for client: {}", nomeCliente);
                setFallbackData(execution, nomeCliente);
            }
        }
    }

    /**
     * Enriches lead data from multiple sources
     */
    private Map<String, Object> enrichLeadData(String nomeCliente, String cnpj, String tipoPesquisa) {
        Map<String, Object> result = new HashMap<>();

        // Simulate CRM API call
        Map<String, Object> crmData = fetchFromCRM(nomeCliente, cnpj);

        // Simulate external database enrichment
        Map<String, Object> externalData = fetchFromExternalSources(cnpj, tipoPesquisa);

        // Combine and process data
        result.put("financialData", mergeFinancialData(crmData, externalData));
        result.put("decisionStructure", extractDecisionStructure(crmData));
        result.put("challenges", identifyBusinessChallenges(crmData, externalData));
        result.put("competitors", extractCompetitors(externalData));
        result.put("strategicInitiatives", extractStrategicInitiatives(crmData, externalData));
        result.put("fitScore", calculateFitScore(crmData, externalData));
        result.put("confidence", calculateConfidenceScore(crmData, externalData));

        return result;
    }

    /**
     * Fetches data from CRM system (Salesforce/HubSpot)
     */
    private Map<String, Object> fetchFromCRM(String nomeCliente, String cnpj) {
        // TODO: Implement actual CRM API integration
        // For now, return mock data
        Map<String, Object> crmData = new HashMap<>();
        crmData.put("companyName", nomeCliente);
        crmData.put("cnpj", cnpj);
        crmData.put("revenue", 5000000.0); // R$5M annual revenue
        crmData.put("employees", 250);
        crmData.put("industry", "Healthcare");
        crmData.put("growthRate", 0.15); // 15% growth
        return crmData;
    }

    /**
     * Fetches data from external sources (company databases, news, reports)
     */
    private Map<String, Object> fetchFromExternalSources(String cnpj, String tipoPesquisa) {
        // TODO: Implement external API integrations
        Map<String, Object> externalData = new HashMap<>();
        externalData.put("marketPosition", "Mid-sized regional player");
        externalData.put("recentNews", "Expansion announced Q3 2024");
        return externalData;
    }

    private Map<String, Object> mergeFinancialData(Map<String, Object> crmData, Map<String, Object> externalData) {
        Map<String, Object> financial = new HashMap<>();
        financial.put("faturamentoAnual", crmData.get("revenue"));
        financial.put("numeroFuncionarios", crmData.get("employees"));
        financial.put("taxaCrescimento", crmData.get("growthRate"));
        financial.put("posicaoMercado", externalData.get("marketPosition"));
        return financial;
    }

    private Map<String, Object> extractDecisionStructure(Map<String, Object> crmData) {
        Map<String, Object> structure = new HashMap<>();
        structure.put("ceo", "João Silva");
        structure.put("cfo", "Maria Santos");
        structure.put("diretorClinico", "Dr. Pedro Costa");
        structure.put("comiteDecisao", true);
        return structure;
    }

    private java.util.List<String> identifyBusinessChallenges(Map<String, Object> crmData, Map<String, Object> externalData) {
        java.util.List<String> challenges = new java.util.ArrayList<>();
        challenges.add("Alto custo operacional em UTI");
        challenges.add("Baixa utilização de leitos");
        challenges.add("Sinistralidade crescente");
        return challenges;
    }

    private java.util.List<String> extractCompetitors(Map<String, Object> externalData) {
        java.util.List<String> competitors = new java.util.ArrayList<>();
        competitors.add("Competitor A");
        competitors.add("Competitor B");
        return competitors;
    }

    private java.util.List<String> extractStrategicInitiatives(Map<String, Object> crmData, Map<String, Object> externalData) {
        java.util.List<String> initiatives = new java.util.ArrayList<>();
        initiatives.add("Expansão regional planejada");
        initiatives.add("Modernização de sistemas");
        return initiatives;
    }

    /**
     * Calculates fit score for backward compatibility with V1 (<$500K deals)
     */
    private Double calculateFitScore(Map<String, Object> crmData, Map<String, Object> externalData) {
        double revenue = (Double) crmData.getOrDefault("revenue", 0.0);
        int employees = (Integer) crmData.getOrDefault("employees", 0);

        // Simple fit scoring algorithm
        double score = 0.0;
        if (revenue >= 1000000) score += 3.0;
        else if (revenue >= 500000) score += 2.0;
        else score += 1.0;

        if (employees >= 100) score += 3.0;
        else if (employees >= 50) score += 2.0;
        else score += 1.0;

        // Normalize to 0-10 scale
        return Math.min(10.0, score);
    }

    private Double calculateConfidenceScore(Map<String, Object> crmData, Map<String, Object> externalData) {
        // Calculate confidence based on data completeness
        int fields = crmData.size() + externalData.size();
        return Math.min(1.0, fields / 10.0);
    }

    /**
     * Sets fallback data when enrichment fails
     */
    private void setFallbackData(DelegateExecution execution, String nomeCliente) {
        Map<String, Object> fallbackFinancial = new HashMap<>();
        fallbackFinancial.put("faturamentoAnual", 0.0);
        fallbackFinancial.put("source", "fallback");

        execution.setVariable("dadosFinanceiros", fallbackFinancial);
        execution.setVariable("estruturaDecisao", new HashMap<>());
        execution.setVariable("desafiosIdentificados", new java.util.ArrayList<>());
        execution.setVariable("concorrentes", new java.util.ArrayList<>());
        execution.setVariable("iniciativasEstrategicas", new java.util.ArrayList<>());
        execution.setVariable("fitScore", 0.0);
        execution.setVariable("leadEnrichmentSuccess", false);
        execution.setVariable("enrichmentConfidence", 0.0);
    }

    private int getAttemptNumber(DelegateExecution execution) {
        Integer attempt = (Integer) execution.getVariable("leadEnrichmentAttempt");
        return attempt != null ? attempt : 1;
    }

    /**
     * Executes a callable with timeout protection
     */
    private <T> T executeWithTimeout(java.util.concurrent.Callable<T> callable, long timeoutSeconds) throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        java.util.concurrent.Future<T> future = executor.submit(callable);

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new RuntimeException("Lead enrichment timeout after " + timeoutSeconds + " seconds", e);
        } finally {
            executor.shutdown();
        }
    }
}
