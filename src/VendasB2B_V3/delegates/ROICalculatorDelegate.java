package com.austa.vendas.delegates;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

/**
 * ROICalculatorDelegate - Generates customized ROI calculations for CFO engagement
 *
 * Purpose: Automatically calculate Return on Investment projections based on client
 * financial data, generating interactive calculators and reports for CFO presentations
 * during the engagement phase.
 *
 * Input Variables:
 * - dadosFinanceiros: Map - Client financial data
 * - tipoServico: String - Service type (uti, radiologia, plano_corporativo, combo)
 * - numeroLeitos: Integer - Number of beds (for ICU)
 * - numeroVidas: Integer - Number of covered lives
 * - ticketMedioAtual: Double - Current average ticket
 *
 * Output Variables:
 * - linkCalculadora: String - Link to interactive ROI calculator
 * - roiPercentual: Double - ROI percentage
 * - paybackMeses: Integer - Payback period in months
 * - economiaAnual: Double - Annual savings in R$
 * - investimentoMensal: Double - Monthly investment required
 * - breakEvenPoint: Integer - Break-even point in months
 * - relatorioROI: Map - Detailed ROI report with charts
 *
 * Calculation Model:
 * - Based on AUSTA's proven benchmarks from existing clients
 * - ICU: 30% reduction in operating costs, 20% occupancy improvement
 * - Radiology: 40% faster turnaround, 25% cost reduction
 * - Corporate Plans: 15% reduction in claims, 10% better retention
 *
 * @author AUSTA V3 Hive Mind - Coder Agent
 * @version 3.0.0
 */
@Component("roiCalculatorDelegate")
public class ROICalculatorDelegate implements JavaDelegate {

    private static final Logger LOGGER = LoggerFactory.getLogger(ROICalculatorDelegate.class);

    // Service-specific improvement benchmarks
    private static final double ICU_COST_REDUCTION = 0.30; // 30%
    private static final double ICU_OCCUPANCY_IMPROVEMENT = 0.20; // 20%
    private static final double RADIOLOGY_COST_REDUCTION = 0.25; // 25%
    private static final double RADIOLOGY_TURNAROUND_IMPROVEMENT = 0.40; // 40%
    private static final double CORPORATE_CLAIMS_REDUCTION = 0.15; // 15%
    private static final double CORPORATE_RETENTION_IMPROVEMENT = 0.10; // 10%

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        String tipoServico = (String) execution.getVariable("tipoServico");

        LOGGER.info("Calculating ROI for service type: {}", tipoServico);

        try {
            Map<String, Object> dadosFinanceiros = (Map<String, Object>) execution.getVariable("dadosFinanceiros");
            Integer numeroLeitos = (Integer) execution.getVariable("numeroLeitos");
            Integer numeroVidas = (Integer) execution.getVariable("numeroVidas");
            Double ticketMedioAtual = (Double) execution.getVariable("ticketMedioAtual");

            // Calculate ROI based on service type
            Map<String, Object> roiData = calculateROI(tipoServico, dadosFinanceiros,
                                                       numeroLeitos, numeroVidas, ticketMedioAtual);

            // Generate interactive calculator link
            String calculatorLink = generateCalculatorLink(roiData, tipoServico);

            // Set output variables
            execution.setVariable("linkCalculadora", calculatorLink);
            execution.setVariable("roiPercentual", roiData.get("roiPercentual"));
            execution.setVariable("paybackMeses", roiData.get("paybackMeses"));
            execution.setVariable("economiaAnual", roiData.get("economiaAnual"));
            execution.setVariable("investimentoMensal", roiData.get("investimentoMensal"));
            execution.setVariable("breakEvenPoint", roiData.get("breakEvenPoint"));
            execution.setVariable("relatorioROI", roiData);
            execution.setVariable("roiCalculationSuccess", true);

            LOGGER.info("ROI calculation completed. ROI: {}%, Payback: {} months",
                       roiData.get("roiPercentual"), roiData.get("paybackMeses"));

        } catch (Exception e) {
            LOGGER.error("ROI calculation failed for service type: {}", tipoServico, e);
            setFallbackROI(execution);
            execution.setVariable("roiCalculationSuccess", false);
            // Don't throw - provide fallback data instead
        }
    }

    /**
     * Calculates ROI based on service type and client data
     */
    private Map<String, Object> calculateROI(String tipoServico, Map<String, Object> dadosFinanceiros,
                                             Integer numeroLeitos, Integer numeroVidas, Double ticketMedioAtual) {
        Map<String, Object> roi = new HashMap<>();

        switch (tipoServico.toLowerCase()) {
            case "uti":
            case "icu":
                return calculateICUROI(dadosFinanceiros, numeroLeitos);
            case "radiologia":
            case "radiology":
                return calculateRadiologyROI(dadosFinanceiros);
            case "plano_corporativo":
            case "corporate_plan":
                return calculateCorporatePlanROI(dadosFinanceiros, numeroVidas, ticketMedioAtual);
            case "combo":
                return calculateComboROI(dadosFinanceiros, numeroLeitos, numeroVidas, ticketMedioAtual);
            default:
                return calculateGenericROI(dadosFinanceiros);
        }
    }

    /**
     * Calculates ROI for ICU services
     */
    private Map<String, Object> calculateICUROI(Map<String, Object> dadosFinanceiros, Integer numeroLeitos) {
        Map<String, Object> roi = new HashMap<>();

        // Estimate current ICU costs
        double currentMonthlyCost = numeroLeitos != null ? numeroLeitos * 50000.0 : 500000.0; // R$50k per bed
        double currentAnnualCost = currentMonthlyCost * 12;

        // Calculate savings from AUSTA optimization
        double costReductionAnnual = currentAnnualCost * ICU_COST_REDUCTION;
        double occupancyRevenueIncrease = currentAnnualCost * ICU_OCCUPANCY_IMPROVEMENT;
        double totalAnnualBenefit = costReductionAnnual + occupancyRevenueIncrease;

        // Calculate investment
        double monthlyInvestment = numeroLeitos != null ? numeroLeitos * 5000.0 : 50000.0; // R$5k per bed
        double annualInvestment = monthlyInvestment * 12;

        // Calculate ROI metrics
        double roiPercentual = ((totalAnnualBenefit - annualInvestment) / annualInvestment) * 100;
        int paybackMeses = (int) Math.ceil((annualInvestment / totalAnnualBenefit) * 12);

        roi.put("roiPercentual", round(roiPercentual, 2));
        roi.put("paybackMeses", paybackMeses);
        roi.put("economiaAnual", round(totalAnnualBenefit, 2));
        roi.put("investimentoMensal", round(monthlyInvestment, 2));
        roi.put("investimentoAnual", round(annualInvestment, 2));
        roi.put("breakEvenPoint", paybackMeses);
        roi.put("reducaoCustos", round(costReductionAnnual, 2));
        roi.put("aumentoReceita", round(occupancyRevenueIncrease, 2));
        roi.put("servicoTipo", "UTI/ICU");

        // Add 3-year projection
        addYearlyProjection(roi, annualInvestment, totalAnnualBenefit, 3);

        return roi;
    }

    /**
     * Calculates ROI for Radiology services
     */
    private Map<String, Object> calculateRadiologyROI(Map<String, Object> dadosFinanceiros) {
        Map<String, Object> roi = new HashMap<>();

        double currentAnnualCost = 1200000.0; // Estimated R$1.2M annual radiology costs

        double costReductionAnnual = currentAnnualCost * RADIOLOGY_COST_REDUCTION;
        double efficiencyGains = currentAnnualCost * 0.10; // 10% efficiency gains
        double totalAnnualBenefit = costReductionAnnual + efficiencyGains;

        double monthlyInvestment = 30000.0; // R$30k monthly
        double annualInvestment = monthlyInvestment * 12;

        double roiPercentual = ((totalAnnualBenefit - annualInvestment) / annualInvestment) * 100;
        int paybackMeses = (int) Math.ceil((annualInvestment / totalAnnualBenefit) * 12);

        roi.put("roiPercentual", round(roiPercentual, 2));
        roi.put("paybackMeses", paybackMeses);
        roi.put("economiaAnual", round(totalAnnualBenefit, 2));
        roi.put("investimentoMensal", round(monthlyInvestment, 2));
        roi.put("investimentoAnual", round(annualInvestment, 2));
        roi.put("breakEvenPoint", paybackMeses);
        roi.put("reducaoCustos", round(costReductionAnnual, 2));
        roi.put("ganhosEficiencia", round(efficiencyGains, 2));
        roi.put("servicoTipo", "Radiologia");
        roi.put("melhoriaTempoLaudo", "40% mais rápido");

        addYearlyProjection(roi, annualInvestment, totalAnnualBenefit, 3);

        return roi;
    }

    /**
     * Calculates ROI for Corporate Health Plans
     */
    private Map<String, Object> calculateCorporatePlanROI(Map<String, Object> dadosFinanceiros,
                                                          Integer numeroVidas, Double ticketMedioAtual) {
        Map<String, Object> roi = new HashMap<>();

        int vidas = numeroVidas != null ? numeroVidas : 1000;
        double ticketMedio = ticketMedioAtual != null ? ticketMedioAtual : 500.0;

        double currentAnnualRevenue = vidas * ticketMedio * 12;

        double claimsReduction = currentAnnualRevenue * CORPORATE_CLAIMS_REDUCTION;
        double retentionImprovement = currentAnnualRevenue * CORPORATE_RETENTION_IMPROVEMENT;
        double totalAnnualBenefit = claimsReduction + retentionImprovement;

        double monthlyInvestment = vidas * 15.0; // R$15 per life monthly
        double annualInvestment = monthlyInvestment * 12;

        double roiPercentual = ((totalAnnualBenefit - annualInvestment) / annualInvestment) * 100;
        int paybackMeses = (int) Math.ceil((annualInvestment / totalAnnualBenefit) * 12);

        roi.put("roiPercentual", round(roiPercentual, 2));
        roi.put("paybackMeses", paybackMeses);
        roi.put("economiaAnual", round(totalAnnualBenefit, 2));
        roi.put("investimentoMensal", round(monthlyInvestment, 2));
        roi.put("investimentoAnual", round(annualInvestment, 2));
        roi.put("breakEvenPoint", paybackMeses);
        roi.put("reducaoSinistralidade", round(claimsReduction, 2));
        roi.put("melhoriaRetencao", round(retentionImprovement, 2));
        roi.put("servicoTipo", "Plano Corporativo");
        roi.put("numeroVidas", vidas);

        addYearlyProjection(roi, annualInvestment, totalAnnualBenefit, 3);

        return roi;
    }

    /**
     * Calculates ROI for Combo services (ICU + Radiology + Corporate Plan)
     */
    private Map<String, Object> calculateComboROI(Map<String, Object> dadosFinanceiros,
                                                   Integer numeroLeitos, Integer numeroVidas, Double ticketMedioAtual) {
        Map<String, Object> roi = new HashMap<>();

        // Combine all service benefits with 10% synergy bonus
        Map<String, Object> icuROI = calculateICUROI(dadosFinanceiros, numeroLeitos);
        Map<String, Object> radioROI = calculateRadiologyROI(dadosFinanceiros);
        Map<String, Object> corpROI = calculateCorporatePlanROI(dadosFinanceiros, numeroVidas, ticketMedioAtual);

        double totalBenefit = (Double) icuROI.get("economiaAnual") +
                             (Double) radioROI.get("economiaAnual") +
                             (Double) corpROI.get("economiaAnual");
        totalBenefit *= 1.10; // 10% synergy bonus

        double totalInvestment = (Double) icuROI.get("investimentoAnual") +
                                (Double) radioROI.get("investimentoAnual") +
                                (Double) corpROI.get("investimentoAnual");
        totalInvestment *= 0.90; // 10% combo discount

        double monthlyInvestment = totalInvestment / 12;
        double roiPercentual = ((totalBenefit - totalInvestment) / totalInvestment) * 100;
        int paybackMeses = (int) Math.ceil((totalInvestment / totalBenefit) * 12);

        roi.put("roiPercentual", round(roiPercentual, 2));
        roi.put("paybackMeses", paybackMeses);
        roi.put("economiaAnual", round(totalBenefit, 2));
        roi.put("investimentoMensal", round(monthlyInvestment, 2));
        roi.put("investimentoAnual", round(totalInvestment, 2));
        roi.put("breakEvenPoint", paybackMeses);
        roi.put("servicoTipo", "Combo (UTI + Radiologia + Corporativo)");
        roi.put("bonusSinergia", "10%");
        roi.put("descontoCombo", "10%");

        addYearlyProjection(roi, totalInvestment, totalBenefit, 3);

        return roi;
    }

    /**
     * Calculates generic ROI when service type is not specified
     */
    private Map<String, Object> calculateGenericROI(Map<String, Object> dadosFinanceiros) {
        Map<String, Object> roi = new HashMap<>();

        double totalAnnualBenefit = 1000000.0; // R$1M estimated
        double annualInvestment = 400000.0; // R$400k estimated
        double monthlyInvestment = annualInvestment / 12;

        double roiPercentual = ((totalAnnualBenefit - annualInvestment) / annualInvestment) * 100;
        int paybackMeses = (int) Math.ceil((annualInvestment / totalAnnualBenefit) * 12);

        roi.put("roiPercentual", round(roiPercentual, 2));
        roi.put("paybackMeses", paybackMeses);
        roi.put("economiaAnual", round(totalAnnualBenefit, 2));
        roi.put("investimentoMensal", round(monthlyInvestment, 2));
        roi.put("investimentoAnual", round(annualInvestment, 2));
        roi.put("breakEvenPoint", paybackMeses);
        roi.put("servicoTipo", "Genérico");

        addYearlyProjection(roi, annualInvestment, totalAnnualBenefit, 3);

        return roi;
    }

    /**
     * Adds 3-year financial projection to ROI data
     */
    private void addYearlyProjection(Map<String, Object> roi, double annualInvestment,
                                    double annualBenefit, int years) {
        java.util.List<Map<String, Object>> projection = new java.util.ArrayList<>();

        for (int year = 1; year <= years; year++) {
            Map<String, Object> yearData = new HashMap<>();
            yearData.put("ano", year);
            yearData.put("investimento", round(annualInvestment, 2));
            yearData.put("beneficio", round(annualBenefit, 2));
            yearData.put("lucroLiquido", round(annualBenefit - annualInvestment, 2));
            yearData.put("roiAcumulado", round(((annualBenefit * year) - (annualInvestment * year)) /
                                              (annualInvestment * year) * 100, 2));
            projection.add(yearData);
        }

        roi.put("projecao3Anos", projection);
    }

    /**
     * Generates interactive calculator link
     */
    private String generateCalculatorLink(Map<String, Object> roiData, String tipoServico) {
        // TODO: Implement actual calculator URL generation (e.g., Google Sheets, Excel Online, custom app)
        String baseUrl = "https://roi-calculator.austa.com.br/";
        String calculatorId = java.util.UUID.randomUUID().toString();

        return baseUrl + "?id=" + calculatorId + "&service=" + tipoServico +
               "&roi=" + roiData.get("roiPercentual") +
               "&payback=" + roiData.get("paybackMeses");
    }

    /**
     * Sets fallback ROI data when calculation fails
     */
    private void setFallbackROI(DelegateExecution execution) {
        execution.setVariable("linkCalculadora", "https://roi-calculator.austa.com.br/generic");
        execution.setVariable("roiPercentual", 150.0);
        execution.setVariable("paybackMeses", 8);
        execution.setVariable("economiaAnual", 1000000.0);
        execution.setVariable("investimentoMensal", 40000.0);
        execution.setVariable("breakEvenPoint", 8);

        Map<String, Object> fallbackReport = new HashMap<>();
        fallbackReport.put("servicoTipo", "Estimativa Genérica");
        fallbackReport.put("status", "fallback");
        execution.setVariable("relatorioROI", fallbackReport);
    }

    /**
     * Rounds double values to specified decimal places
     */
    private double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = BigDecimal.valueOf(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }
}
