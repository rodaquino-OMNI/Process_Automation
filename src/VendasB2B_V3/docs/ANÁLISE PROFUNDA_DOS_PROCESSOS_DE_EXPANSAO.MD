ANÁLISE PROFUNDA DOS PROCESSOS
1.1 MAPEAMENTO DOS FLUXOS PRINCIPAIS
OPE-04 — Vendas Consultivas B2B e Gestão de Grandes Contas
Etapas Críticas do Ciclo:
Qualificação de leads e prospecção de clientes potenciais
Análise de necessidades e diagnóstico empresarial
Desenvolvimento de propostas customizadas
Negociação e fechamento de contratos
Acompanhamento estratégico de contas-chave
Renovação e expansão de contratos
KPIs Definidos:
Estratégico: # VIDAS SOB CUIDADO
Específico: Taxa de conversão de propostas para contratos (empresas >100 vidas)
Medidas de Direção:
contatos qualificados por vendedor por semana
% propostas enviadas dentro do prazo comprometido (SLA 48h)
% visitas em clientes >100 vidas por trimestre
OPE-05 — Desenvolvimento e Lançamento Ágil de Produtos
Etapas Críticas:
Pesquisa de mercado e identificação de oportunidades
Concepção e design de produtos
Análise de viabilidade técnica e financeira
Desenvolvimento de protótipos e testes
Precificação e registro regulatório (ANS)
Lançamento e monitoramento de performance
KPIs Definidos:
Estratégico: % RECEITA COM NOVOS PRODUTOS
Específico: Time-to-market de novos produtos (dias)
Medidas de Direção:
etapas do desenvolvimento concluídas por mês
% cronograma de desenvolvimento cumprido
protótipos testados com clientes potenciais por trimestre
OPE-09 — Implantação e Onboarding de Novos Clientes
Etapas Críticas:
Planejamento da transição e cronograma de implantação
Cadastramento de beneficiários e enriquecimento de dados de saúde
Configuração de regras, coberturas e sistemas
Treinamento e capacitação de RH do cliente
Comunicação e engajamento dos beneficiários
Monitoramento inicial e ajustes pós-implantação
KPIs Definidos:
Estratégico: # NPS DOS GRANDES CLIENTES
Específico: Tempo médio de implantação completa (dias)
Medidas de Direção:
% implantações concluídas no prazo acordado
% cadastros sem erros na primeira tentativa
NPS da experiência de implantação (primeiros 30 dias)
OPE-11 — Expansão Geográfica e Desenvolvimento de Mercados
Etapas Críticas:
Avaliação de mercados-alvo
Credenciamento de prestadores-chave
Registro e lançamento de produtos em novas regiões (ANS)
Monitoramento de ramp-up de operação
KPIs Definidos:
Estratégico: # VIDAS SOB CUIDADO
Específico: # Vidas adicionadas em novas regiões nos últimos 90 dias
Medidas de Direção:
credenciamentos de prestadores-chave em novas regiões por trimestre
% cumprimento do cronograma de lançamento
Taxa de conversão de vendas em novas regiões
1.2 PONTOS DE INTEGRAÇÃO ENTRE OS PROCESSOS
De	Para	Tipo de Integração	Descrição
OPE-11 (Expansão)	OPE-05 (Produtos)	Signal Event	Demanda de novos produtos para novas regiões
OPE-11 (Expansão)	OPE-04 (Vendas)	Workflow Sequencial	Habilitação comercial após registro ANS
OPE-04 (Vendas)	OPE-09 (Implantação)	Message Event	Contrato assinado dispara planejamento de implantação
OPE-09 (Implantação)	OPE-04 (Gestão Contas)	Signal Event	Implantação concluída inicia gestão de conta
OPE-04 (Gestão Contas)	OPE-09 (Implantação)	Signal Event	Expansão de contrato dispara nova implantação
OPE-09 (Implantação)	OPE-05 (Produtos)	Feedback Loop	Insights de implantação alimentam melhorias de produto
1.3 LACUNAS E OPORTUNIDADES IDENTIFICADAS NO BPMN EXISTENTE
O que o BPMN existente já cobre bem:
Estrutura básica de pools e lanes
Fluxo principal de expansão → geração de demanda → vendas → implantação → gestão
Subprocessos para cada área funcional
Integração via sinais e mensagens
KPIs como text annotations
Lacunas identificadas que precisam ser expandidas:
Gestão de Pipeline e Funil de Vendas mais detalhada:
Falta subprocesso para ABM (Account-Based Marketing)
Ausência de scoring de oportunidades automatizado
Critérios de priorização de contas não explícitos no diagrama
Inteligência de Mercado e Análise de Dados:
Falta subprocesso de análise preditiva de mercados
Ausência de data store para Business Intelligence
Não há feedback loop estruturado de dados de mercado
Aprovações e Alçadas mais granulares:
Workflow de aprovação de condições especiais simplificado
Falta escalation event para propostas prioritárias
Ausência de comitê de pricing formal
Integração com Sistemas Legados mais explícita:
Falta detalhamento de integração com Tasy ERP
Ausência de service task para API de beneficiários
Integração com sistemas do cliente não detalhada
Monitoramento e Métricas em tempo real:
Falta subprocesso de dashboard e alertas automáticos
Ausência de timer events para SLAs de etapas
Medição de KPIs não automatizada
Fluxos de Exceção mais completos:
Tratamento de perda de oportunidade em estágio avançado
Atraso crítico em implantação
Mudança de escopo pelo cliente
Falha em integrações de sistemas
Ciclos de Feedback e Melhoria Contínua:
Loop de aprendizado com perdas de propostas
Feedback de implantação para ajuste de produtos
Insights de campo para expansão geográfica
1.4 ELEMENTOS OBRIGATÓRIOS PARA O NOVO BPMN
Participantes (Pools):
Mercado e Prospects (black box)
Cliente Corporativo (black box)
AUSTA - Máquina de Expansão e Vendas B2B (main)
Sistemas Integrados e Plataformas (black box)
Lanes (Raias de Responsabilidade):
Expansão Geográfica (OPE-11)
Marketing e Geração de Demanda
Inteligência de Mercado e Analytics
Vendas Consultivas B2B (OPE-04)
Gestão de Grandes Contas
Produto e Pricing (OPE-05)
Precificação e Análise de Risco
Implantação e Onboarding (OPE-09)
Backoffice e Operações
Customer Success e Retenção
Subprocessos Expandidos:
Análise e Priorização de Mercados
Geração de Demanda e Marketing
Qualificação e Priorização de Leads (ABM)
Discovery e Diagnóstico Consultivo
Elaboração e Aprovação de Propostas
Negociação e Fechamento
Planejamento de Implantação
Execução da Implantação (paralelo)
Go-Live e Ativação
Monitoramento Pós-Implantação
Gestão de Grandes Contas
Expansão e Renovação de Contratos
Desenvolvimento Ágil de Produtos
Eventos Obrigatórios:
Start Events: Lead inbound, Solicitação de proposta, Ciclo de expansão, Demanda de produto
Intermediate Events: Timer para SLAs, Message para feedback, Signal para integrações
Boundary Events: Error para validações, Timer para escalation
End Events: Contrato fechado, Oportunidade perdida, Implantação concluída, Produto lançado
Gateways Críticos:
Lead qualificado? (BANT ≥60, >100 vidas)
Mercado viável? (Score ≥7)
Produto viável?
Aprovação especial necessária?
Go-Live ready?
Oportunidade de expansão?
Dados válidos?
Data Objects e Data Stores:
CRM (Salesforce/HubSpot)
Tasy ERP
Marketing Automation
Business Intelligence
Proposta Comercial
Business Case
Modelo de Precificação
Cronograma de Implantação
Contrato
