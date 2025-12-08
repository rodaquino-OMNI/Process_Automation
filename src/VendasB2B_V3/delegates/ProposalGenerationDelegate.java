package com.austa.vendas.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ProposalGenerationDelegate - Generates customized commercial proposals as PDF documents
 *
 * Purpose: Automatically generate professional PDF proposals incorporating MEDDIC insights,
 * ROI calculations, value workshop results, and customized service offerings. Integrates
 * with document generation services and stores proposals in CRM.
 *
 * Input Variables:
 * - nomeCliente: String - Client company name
 * - dadosFinanceiros: Map - Financial data from enrichment
 * - scoreMEDDIC: Double - MEDDIC qualification score
 * - relatorioROI: Map - ROI calculations from calculator
 * - tipoServico: String - Service type being proposed
 * - diagnosticoAtual_VAL: String - Current diagnosis from value workshop
 * - visaoFuturo_VAL: String - Future vision from value workshop
 * - roadmap90dias_VAL: String - 90-day roadmap
 * - investimentoTotal_VAL: Double - Total investment amount
 * - numeroLeitos: Integer - Number of beds (if applicable)
 * - numeroVidas: Integer - Number of covered lives (if applicable)
 * - vendedorResponsavel: String - Sales representative name
 *
 * Output Variables:
 * - propostaURL: String - URL to generated PDF proposal
 * - propostaID: String - Unique proposal identifier
 * - propostaNome: String - Proposal filename
 * - dataGeracao: Date - Generation timestamp
 * - propostaSections: Map - Proposal structure with sections
 * - proposalGenerationSuccess: Boolean - Success indicator
 *
 * Document Structure:
 * 1. Executive Summary (MEDDIC insights)
 * 2. Current Situation Analysis
 * 3. Proposed Solution
 * 4. Financial Investment & ROI
 * 5. Implementation Roadmap
 * 6. Terms & Conditions
 * 7. Next Steps
 *
 * Integration Points:
 * - PDF generation service (e.g., Apache PDFBox, iText, or cloud service)
 * - Document storage (AWS S3, Azure Blob, or internal DMS)
 * - CRM update (stores proposal link in opportunity)
 * - Email service (optional - sends proposal to stakeholders)
 *
 * @author AUSTA V3 Hive Mind - Coder Agent
 * @version 3.0.0
 */
@Component("proposalGenerationDelegate")
public class ProposalGenerationDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProposalGenerationDelegate.class);
    private static final String PROPOSAL_BASE_URL = "https://proposals.austa.com.br/";
    private static final int MAX_RETRY_ATTEMPTS = 2;

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String nomeCliente = (String) execution.getVariable("nomeCliente");
        String tipoServico = (String) execution.getVariable("tipoServico");

        LOGGER.info("Starting proposal generation for client: {}, service: {}", nomeCliente, tipoServico);

        int attempt = getAttemptNumber(execution);

        try {
            // Gather all required data
            ProposalData proposalData = gatherProposalData(execution);

            // Validate data completeness
            validateProposalData(proposalData);

            // Generate proposal document
            String proposalId = generateProposalId(nomeCliente, tipoServico);
            String proposalFilename = generateProposalFilename(nomeCliente, proposalId);

            // Create PDF document
            byte[] pdfContent = generatePDFDocument(proposalData, proposalId);

            // Upload to storage
            String proposalURL = uploadProposalToStorage(pdfContent, proposalFilename);

            // Create proposal structure map
            Map<String, Object> proposalSections = createProposalStructure(proposalData);

            // Set output variables
            execution.setVariable("propostaURL", proposalURL);
            execution.setVariable("propostaID", proposalId);
            execution.setVariable("propostaNome", proposalFilename);
            execution.setVariable("dataGeracao", new Date());
            execution.setVariable("propostaSections", proposalSections);
            execution.setVariable("proposalGenerationSuccess", true);
            execution.setVariable("propostaValidade", calculateExpirationDate(30)); // 30 days validity

            LOGGER.info("Proposal generated successfully. ID: {}, URL: {}", proposalId, proposalURL);

        } catch (Exception e) {
            LOGGER.error("Proposal generation failed for client: {} (Attempt {}/{})",
                        nomeCliente, attempt, MAX_RETRY_ATTEMPTS, e);

            if (attempt < MAX_RETRY_ATTEMPTS) {
                execution.setVariable("proposalGenerationAttempt", attempt + 1);
                throw new RuntimeException("Proposal generation failed, retrying...", e);
            } else {
                LOGGER.warn("Max retry attempts reached. Setting fallback proposal data.");
                setFallbackProposal(execution, nomeCliente);
            }
        }
    }

    /**
     * Gathers all data needed for proposal generation
     */
    private ProposalData gatherProposalData(DelegateExecution execution) {
        ProposalData data = new ProposalData();

        // Client data
        data.nomeCliente = (String) execution.getVariable("nomeCliente");
        data.cnpj = (String) execution.getVariable("cnpj");
        data.dadosFinanceiros = (Map<String, Object>) execution.getVariable("dadosFinanceiros");

        // Qualification data
        data.scoreMEDDIC = (Double) execution.getVariable("scoreMEDDIC");
        data.dorPrincipal = (String) execution.getVariable("dorPrincipal");
        data.impactoFinanceiroDor = (Double) execution.getVariable("impactoFinanceiroDor");
        data.championIdentificado = (Boolean) execution.getVariable("championIdentificado");
        data.nomeChampion = (String) execution.getVariable("nomeChampion");

        // ROI data
        data.relatorioROI = (Map<String, Object>) execution.getVariable("relatorioROI");

        // Value workshop data
        data.diagnosticoAtual = (String) execution.getVariable("diagnosticoAtual_VAL");
        data.visaoFuturo = (String) execution.getVariable("visaoFuturo_VAL");
        data.roadmap90dias = (String) execution.getVariable("roadmap90dias_VAL");
        data.quickWins = (String) execution.getVariable("quickWins_VAL");

        // Investment data
        data.investimentoTotal = (Double) execution.getVariable("investimentoTotal_VAL");
        data.roiCalculado = (Double) execution.getVariable("roiCalculado_VAL");
        data.paybackMeses = (Integer) execution.getVariable("paybackMeses");

        // Service configuration
        data.tipoServico = (String) execution.getVariable("tipoServico");
        data.numeroLeitos = (Integer) execution.getVariable("numeroLeitos");
        data.numeroVidas = (Integer) execution.getVariable("numeroVidas");

        // Sales representative
        data.vendedorResponsavel = (String) execution.getVariable("vendedorResponsavel");
        data.vendedorEmail = (String) execution.getVariable("vendedorEmail");
        data.vendedorTelefone = (String) execution.getVariable("vendedorTelefone");

        return data;
    }

    /**
     * Validates that required proposal data is present
     */
    private void validateProposalData(ProposalData data) throws Exception {
        if (data.nomeCliente == null || data.nomeCliente.trim().isEmpty()) {
            throw new Exception("Client name is required for proposal generation");
        }
        if (data.tipoServico == null || data.tipoServico.trim().isEmpty()) {
            throw new Exception("Service type is required for proposal generation");
        }
        if (data.investimentoTotal == null || data.investimentoTotal <= 0) {
            throw new Exception("Investment amount is required for proposal generation");
        }
    }

    /**
     * Generates PDF document with proposal content
     */
    private byte[] generatePDFDocument(ProposalData data, String proposalId) {
        // TODO: Implement actual PDF generation using Apache PDFBox, iText, or cloud service
        // This is a placeholder that would integrate with your PDF generation library

        LOGGER.info("Generating PDF document for proposal: {}", proposalId);

        // Simulated PDF generation
        StringBuilder pdfContent = new StringBuilder();
        pdfContent.append("PROPOSTA COMERCIAL AUSTA\n\n");
        pdfContent.append("Cliente: ").append(data.nomeCliente).append("\n");
        pdfContent.append("Tipo de Serviço: ").append(data.tipoServico).append("\n");
        pdfContent.append("Investimento: R$ ").append(data.investimentoTotal).append("\n");
        pdfContent.append("ROI: ").append(data.roiCalculado).append("%\n");
        pdfContent.append("Payback: ").append(data.paybackMeses).append(" meses\n");

        // In production, this would return actual PDF bytes
        return pdfContent.toString().getBytes();
    }

    /**
     * Uploads proposal to storage service
     */
    private String uploadProposalToStorage(byte[] pdfContent, String filename) {
        // TODO: Implement actual cloud storage upload (AWS S3, Azure Blob, etc.)
        LOGGER.info("Uploading proposal to storage: {}", filename);

        // Simulated upload - return URL
        return PROPOSAL_BASE_URL + filename;
    }

    /**
     * Creates proposal structure with sections
     */
    private Map<String, Object> createProposalStructure(ProposalData data) {
        Map<String, Object> structure = new HashMap<>();

        // Section 1: Executive Summary
        Map<String, Object> executiveSummary = new HashMap<>();
        executiveSummary.put("title", "Sumário Executivo");
        executiveSummary.put("content", buildExecutiveSummary(data));
        structure.put("section1_executiveSummary", executiveSummary);

        // Section 2: Current Situation Analysis
        Map<String, Object> currentSituation = new HashMap<>();
        currentSituation.put("title", "Análise da Situação Atual");
        currentSituation.put("content", data.diagnosticoAtual);
        currentSituation.put("pain", data.dorPrincipal);
        currentSituation.put("financialImpact", data.impactoFinanceiroDor);
        structure.put("section2_currentSituation", currentSituation);

        // Section 3: Proposed Solution
        Map<String, Object> solution = new HashMap<>();
        solution.put("title", "Solução Proposta");
        solution.put("content", data.visaoFuturo);
        solution.put("serviceType", data.tipoServico);
        solution.put("quickWins", data.quickWins);
        structure.put("section3_solution", solution);

        // Section 4: Financial Investment & ROI
        Map<String, Object> financial = new HashMap<>();
        financial.put("title", "Investimento e Retorno");
        financial.put("investment", data.investimentoTotal);
        financial.put("roi", data.roiCalculado);
        financial.put("payback", data.paybackMeses);
        financial.put("roiReport", data.relatorioROI);
        structure.put("section4_financial", financial);

        // Section 5: Implementation Roadmap
        Map<String, Object> roadmap = new HashMap<>();
        roadmap.put("title", "Roadmap de Implementação");
        roadmap.put("content", data.roadmap90dias);
        structure.put("section5_roadmap", roadmap);

        // Section 6: Terms & Conditions
        Map<String, Object> terms = new HashMap<>();
        terms.put("title", "Termos e Condições");
        terms.put("content", buildTermsAndConditions(data));
        structure.put("section6_terms", terms);

        // Section 7: Next Steps
        Map<String, Object> nextSteps = new HashMap<>();
        nextSteps.put("title", "Próximos Passos");
        nextSteps.put("content", buildNextSteps(data));
        nextSteps.put("salesRep", data.vendedorResponsavel);
        nextSteps.put("contact", data.vendedorEmail);
        structure.put("section7_nextSteps", nextSteps);

        return structure;
    }

    private String buildExecutiveSummary(ProposalData data) {
        StringBuilder summary = new StringBuilder();
        summary.append("A AUSTA apresenta uma solução completa de ").append(data.tipoServico);
        summary.append(" que irá transformar a operação de ").append(data.nomeCliente).append(".\n\n");

        if (data.championIdentificado != null && data.championIdentificado) {
            summary.append("Em parceria com ").append(data.nomeChampion).append(", ");
        }

        summary.append("identificamos oportunidades de melhoria que podem gerar ");
        summary.append("um ROI de ").append(data.roiCalculado).append("% ");
        summary.append("com payback em apenas ").append(data.paybackMeses).append(" meses.\n\n");

        summary.append("Score de Qualificação MEDDIC: ").append(data.scoreMEDDIC).append("/10");

        return summary.toString();
    }

    private String buildTermsAndConditions(ProposalData data) {
        StringBuilder terms = new StringBuilder();
        terms.append("1. Investimento mensal de R$ ").append(data.investimentoTotal).append("\n");
        terms.append("2. Contrato mínimo de 12 meses\n");
        terms.append("3. Garantia de resultados conforme métricas acordadas\n");
        terms.append("4. SLA de suporte 24/7 para serviços críticos\n");
        terms.append("5. Revisões trimestrais de performance\n");
        terms.append("6. Proposta válida por 30 dias\n");
        return terms.toString();
    }

    private String buildNextSteps(ProposalData data) {
        StringBuilder steps = new StringBuilder();
        steps.append("1. Análise e aprovação da proposta pela diretoria\n");
        steps.append("2. Reunião de alinhamento com stakeholders\n");
        steps.append("3. Assinatura do contrato\n");
        steps.append("4. Kickoff do projeto de implementação\n");
        steps.append("5. Início da fase de onboarding\n\n");
        steps.append("Contato: ").append(data.vendedorResponsavel).append(" - ");
        steps.append(data.vendedorEmail);
        return steps.toString();
    }

    private String generateProposalId(String nomeCliente, String tipoServico) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String dateStr = sdf.format(new Date());

        String clienteCode = nomeCliente.replaceAll("[^A-Za-z0-9]", "").substring(0,
                                Math.min(5, nomeCliente.length())).toUpperCase();
        String serviceCode = tipoServico.substring(0, Math.min(3, tipoServico.length())).toUpperCase();

        return "PROP-" + clienteCode + "-" + serviceCode + "-" + dateStr + "-" +
               String.format("%04d", (int)(Math.random() * 10000));
    }

    private String generateProposalFilename(String nomeCliente, String proposalId) {
        String sanitizedName = nomeCliente.replaceAll("[^A-Za-z0-9]", "_");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return "Proposta_AUSTA_" + sanitizedName + "_" + proposalId + "_" +
               sdf.format(new Date()) + ".pdf";
    }

    private Date calculateExpirationDate(int days) {
        long millisInDay = 24 * 60 * 60 * 1000L;
        return new Date(System.currentTimeMillis() + (days * millisInDay));
    }

    private void setFallbackProposal(DelegateExecution execution, String nomeCliente) {
        String fallbackId = "PROP-FALLBACK-" + System.currentTimeMillis();
        String fallbackFilename = "Proposta_" + nomeCliente + "_Fallback.pdf";

        execution.setVariable("propostaURL", PROPOSAL_BASE_URL + fallbackFilename);
        execution.setVariable("propostaID", fallbackId);
        execution.setVariable("propostaNome", fallbackFilename);
        execution.setVariable("dataGeracao", new Date());
        execution.setVariable("propostaSections", new HashMap<>());
        execution.setVariable("proposalGenerationSuccess", false);
        execution.setVariable("proposalGenerationError", "Fallback proposal created due to generation failure");
    }

    private int getAttemptNumber(DelegateExecution execution) {
        Integer attempt = (Integer) execution.getVariable("proposalGenerationAttempt");
        return attempt != null ? attempt : 1;
    }

    /**
     * Inner class to hold proposal data
     */
    private static class ProposalData {
        String nomeCliente;
        String cnpj;
        Map<String, Object> dadosFinanceiros;
        Double scoreMEDDIC;
        String dorPrincipal;
        Double impactoFinanceiroDor;
        Boolean championIdentificado;
        String nomeChampion;
        Map<String, Object> relatorioROI;
        String diagnosticoAtual;
        String visaoFuturo;
        String roadmap90dias;
        String quickWins;
        Double investimentoTotal;
        Double roiCalculado;
        Integer paybackMeses;
        String tipoServico;
        Integer numeroLeitos;
        Integer numeroVidas;
        String vendedorResponsavel;
        String vendedorEmail;
        String vendedorTelefone;
    }
}
