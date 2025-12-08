# MÁQUINA DE EXPANSÃO E VENDAS B2B - AUSTA

## Processo Integrado End-to-End

**Versão:** 1.0  
**Data:** Dezembro 2025  
**Vertical:** Operadora de Saúde AUSTA  
**Processos Integrados:** OPE-04, OPE-05, OPE-09, OPE-11

---

# PARTE 1: RESUMO ANALÍTICO

## 1.1 Síntese da Análise dos Processos

### OPE-04 — Vendas Consultivas B2B e Gestão de Grandes Contas

**O que o processo já cobre bem:**
- Abordagem estruturada em 6 etapas críticas: qualificação de leads → diagnóstico → proposta → negociação → acompanhamento → renovação
- KPIs bem definidos (# Vidas Sob Cuidado, Taxa de Conversão >100 vidas)
- Medidas de direção claras (contatos/semana, propostas no prazo, visitas trimestrais)
- Papéis e responsabilidades bem distribuídos (Gerente de Vendas, Coordenador, Gerente de Sucesso do Cliente)
- Riscos mapeados com controles sugeridos (pipeline insuficiente, proposta não competitiva, demora na resposta)

**Lacunas e oportunidades identificadas:**
- Ausência de automação formal do pipeline de vendas no CRM
- Falta de integração explícita com marketing automation para nurturing de leads
- Handoff entre vendas e implantação não formalizado como workflow automatizado
- Critérios de priorização de oportunidades não sistematizados (valor, risco, potencial de crescimento)
- Alçadas de aprovação de condições especiais podem ser melhor orquestradas via workflow

---

### OPE-05 — Desenvolvimento e Lançamento Ágil de Produtos

**O que o processo já cobre bem:**
- Pipeline completo de desenvolvimento: pesquisa → concepção → viabilidade → protótipos → precificação → registro ANS → lançamento
- KPI estratégico claro (% Receita com Novos Produtos)
- KPI específico de time-to-market bem definido
- Medidas de direção orientadas a cadência de desenvolvimento e validação com clientes
- Gestão de riscos incluindo produto sem demanda e atrasos no registro

**Lacunas e oportunidades identificadas:**
- Falta de integração formal com feedback de vendas e implantação (ciclo de melhoria contínua)
- Ausência de processo de ajuste de produto baseado em insights de mercados em expansão
- Não há trigger explícito para demanda de novos produtos originada de oportunidades de vendas ou expansão geográfica
- Priorização de produtos para novas praças não está formalizada

---

### OPE-09 — Implantação e Onboarding de Novos Clientes

**O que o processo já cobre bem:**
- Orquestração completa entre venda e go-live em 6 etapas: planejamento → cadastro → configuração → treinamento → comunicação → monitoramento
- KPI estratégico de NPS dos Grandes Clientes
- KPI específico de tempo de implantação
- Medidas de direção focadas em prazo, qualidade de cadastros e satisfação
- Riscos bem mapeados (dados incompletos, configuração incorreta, baixo engajamento)

**Lacunas e oportunidades identificadas:**
- Handoff de vendas para implantação pode ser automatizado como evento BPMN
- Validação de dados cadastrais pode ser automatizada com integrações de sistema
- Integração com sistemas do cliente (RH, folha de pagamento) não está formalizada como tarefa de serviço
- Coleta de NPS pode acionar automaticamente ações de melhoria e feedback para vendas

---

### OPE-11 — Expansão Geográfica e Desenvolvimento de Mercados

**O que o processo já cobre bem:**
- Metodologia estruturada em 4 etapas: avaliação de mercado → credenciamento → registro ANS → ramp-up
- KPI estratégico alinhado (# Vidas Sob Cuidado)
- KPI específico de vidas em novas regiões
- Medidas de direção para credenciamento e cumprimento de cronograma
- Riscos mapeados (mercado mal avaliado, rede insuficiente, atraso no registro)

**Lacunas e oportunidades identificadas:**
- Falta de integração formal entre expansão geográfica e desenvolvimento de produtos (novos produtos para novas praças)
- Ausência de workflow para ativação de geração de demanda em novas regiões
- Critérios de priorização de mercados não automatizados
- Monitoramento de ramp-up pode gerar feedback automatizado para ajuste de estratégia

---

## 1.2 Principais Integrações da Máquina Integrada

### Pontos de Integração Críticos

| De | Para | Tipo de Integração | Descrição |
|---|---|---|---|
| OPE-11 (Expansão) | OPE-05 (Produtos) | Signal Event | Demanda de novos produtos para novas regiões |
| OPE-11 (Expansão) | OPE-04 (Vendas) | Workflow Sequencial | Habilitação comercial após registro ANS |
| OPE-04 (Vendas) | OPE-09 (Implantação) | Message Event | Contrato assinado dispara planejamento de implantação |
| OPE-09 (Implantação) | OPE-04 (Gestão Contas) | Signal Event | Implantação concluída inicia gestão de conta |
| OPE-04 (Gestão Contas) | OPE-09 (Implantação) | Signal Event | Expansão de contrato dispara nova implantação |
| OPE-09 (Implantação) | OPE-05 (Produtos) | Feedback Loop | Insights de implantação alimentam melhorias de produto |

### Responsabilidades Convergentes

A máquina integrada de Expansão, Vendas e Onboarding B2B deve convergir as seguintes responsabilidades:

1. **Diretor de Expansão**: Visão estratégica de mercados, priorização de praças, aprovação de business cases
2. **Gerente de Vendas**: Liderança do funil de vendas, gestão de pipeline, fechamento de contratos
3. **Gerente de Sucesso do Cliente**: Orquestração de implantação, gestão de relacionamento, NPS
4. **Coordenador de Marketing**: Geração de demanda, nurturing de leads, comunicação de lançamentos
5. **Especialista de Cadastros e Regras**: Configuração de sistemas, validação de dados, integrações técnicas

---

## 1.3 Automações Necessárias

### Automações de Orquestração

| Automação | Ferramenta | Trigger | Resultado |
|---|---|---|---|
| Lead Scoring Automático | Marketing Automation | Novo lead | Lead qualificado com score BANT |
| Workflow de Aprovação de Preços | Camunda BPM | Condição especial detectada | Aprovação hierárquica automatizada |
| Notificação de Proposta | CRM + Email | Prazo de proposta | Alerta automático para vendedor |
| Validação Cadastral | Tasy ERP | Upload de dados | Validação de CPF, elegibilidade |
| Configuração de Coberturas | Tasy ERP | Contrato assinado | Regras de autorização configuradas |
| Coleta de NPS | Survey Tool | 30 dias pós go-live | Pesquisa automática enviada |

### Integrações de Sistema

| Sistema Origem | Sistema Destino | Dados Trocados |
|---|---|---|
| CRM (Salesforce/HubSpot) | Camunda BPM | Oportunidades, propostas, contratos |
| Marketing Automation | CRM | Leads qualificados (MQL/SAL) |
| Tasy ERP | Portal B2B | Beneficiários, coberturas, autorizações |
| Sistema RH Cliente | Tasy ERP | Dados de funcionários, movimentações |
| Camunda BPM | BI (Power BI) | KPIs, métricas de processo |

---

# PARTE 2: DESCRIÇÃO TEXTUAL DO PROCESSO ALVO

## 2.1 Visão Geral do Processo End-to-End

A **Máquina de Expansão e Vendas B2B** da AUSTA é um processo integrado que orquestra desde a identificação de novos mercados até a operação contínua de clientes corporativos, passando por geração de demanda, vendas consultivas, implantação e gestão de grandes contas.

### Fluxo Principal

```
[Expansão Geográfica] → [Geração de Demanda] → [Vendas Consultivas] → 
[Implantação e Onboarding] → [Gestão de Grandes Contas] → [Renovação/Expansão]
```

O processo opera em ciclo contínuo, onde:
- Novos mercados habilitados alimentam a geração de demanda
- Leads qualificados entram no funil de vendas
- Contratos fechados disparam implantação
- Clientes implantados entram em gestão contínua
- Oportunidades de expansão reiniciam o ciclo de implantação

---

## 2.2 Subprocessos Principais

### SUBPROCESSO 1: Expansão Geográfica (OPE-11)

**Objetivo:** Avaliar, preparar e habilitar novas regiões para operação comercial.

**Representação Visual no BPMN:**
- Subprocesso colapsável `SubProcess_ExpansaoGeografica`
- Tarefas de usuário para avaliação de mercado e registro ANS
- Tarefa de serviço para credenciamento automatizado
- Gateway exclusivo para decisão de viabilidade
- Evento de sinal para comunicar habilitação de nova região

**Etapas:**
1. **Avaliar Mercados-Alvo** (User Task) - Diretor de Expansão analisa potencial, concorrência e viabilidade
2. **Gateway de Viabilidade** - Decisão baseada em score de viabilidade (≥7 = viável)
3. **Credenciar Prestadores-Chave** (Service Task) - Automação do pipeline de credenciamento
4. **Registrar Produtos na ANS** (User Task) - Especialista ANS submete registro regulatório
5. **Nova Região Habilitada** (Signal Event) - Dispara geração de demanda na nova praça

**KPI Medido:** # Vidas adicionadas em novas regiões nos últimos 90 dias

---

### SUBPROCESSO 2: Geração de Demanda e Marketing

**Objetivo:** Gerar, qualificar e nutrir leads para o funil de vendas B2B.

**Representação Visual no BPMN:**
- Subprocesso `SubProcess_GeracaoDemanda`
- Tarefas de serviço integradas com Marketing Automation
- Data Store Reference para CRM

**Etapas:**
1. **Executar Campanhas de Marketing** (Service Task) - Automação de campanhas digitais e eventos
2. **Gerar Leads Qualificados (MQL)** (Service Task) - Lead scoring automático por perfil e comportamento
3. **Nutrir Leads (Lead Nurturing)** (Service Task) - Nutrição com conteúdo até estarem prontos para vendas

**Automação:** Integração com Marketing Automation (HubSpot, RD Station) e CRM (Salesforce)

---

### SUBPROCESSO 3: Vendas Consultivas B2B (OPE-04)

**Objetivo:** Conduzir vendas consultivas estruturadas para grandes contas corporativas.

**Representação Visual no BPMN:**
- Subprocesso expandido `SubProcess_VendasConsultivas`
- Tarefas de usuário para cada etapa do funil
- Gateway exclusivo para qualificação de leads (BANT ≥60, >100 vidas)
- Call Activity para integração com precificação
- Eventos de mensagem para feedback de proposta
- Gateway para aprovação de condições especiais

**Etapas:**
1. **Qualificar Leads (SAL)** (User Task) - Coordenador de Vendas aplica critérios BANT
2. **Gateway Lead Qualificado** - Decisão: >100 vidas E score BANT ≥60
3. **Realizar Discovery Consultivo** (User Task) - Gerente de Vendas conduz reunião de entendimento
4. **Elaborar Diagnóstico de Necessidades** (User Task) - Análise profunda do contexto do prospect
5. **Analisar Risco e Precificar** (Call Activity) - Integração com área de precificação
6. **Desenvolver Proposta Customizada** (User Task) - Elaboração de proposta técnica e comercial (SLA 48h)
7. **Enviar Proposta ao Cliente** (Send Task) - Envio formal com registro no CRM
8. **Aguardar Feedback** (Catch Event) - Evento de mensagem para retorno do cliente
9. **Gateway Status da Proposta** - Rotas: Aceita / Em Negociação / Recusada
10. **Negociar Condições** (User Task) - Renegociação dentro de alçadas
11. **Gateway Aprovação Especial** - Condições fora do padrão → Diretoria
12. **Solicitar Aprovação da Diretoria** (User Task) - Workflow de aprovação hierárquica

**KPIs Medidos:**
- Taxa de conversão de propostas para contratos (empresas >100 vidas)
- % de propostas enviadas dentro do prazo comprometido
- # de contatos qualificados por vendedor por semana

---

### SUBPROCESSO 4: Implantação e Onboarding (OPE-09)

**Objetivo:** Orquestrar todo o processo entre a venda e o início da operação, garantindo experiência positiva.

**Representação Visual no BPMN:**
- Subprocesso expandido `SubProcess_Implantacao`
- Gateway paralelo para execução simultânea de atividades
- Tarefas de serviço para automações de sistema
- Timer event para aguardar data de go-live
- Boundary error event para tratamento de erros de validação

**Etapas:**
1. **Contrato Assinado** (Message Event) - Dispara início da implantação
2. **Planejar Implantação e Cronograma** (User Task) - Gerente de Sucesso do Cliente define plano
3. **Gateway Paralelo** - Inicia 3 trilhas simultâneas:
   - **Trilha 1 - Cadastro:** Cadastrar Beneficiários → Validar Dados → Corrigir Inconsistências (loop)
   - **Trilha 2 - Configuração:** Configurar Regras → Configurar Coberturas → Integrar Sistemas Cliente
   - **Trilha 3 - Treinamento:** Treinar RH Cliente → Comunicar e Engajar Beneficiários
4. **Gateway de Sincronização** - Aguarda conclusão das 3 trilhas
5. **Gateway Go-Live Readiness** - Checklist de prontidão (cadastros, sistemas, treinamentos, comunicação)
6. **Timer de Go-Live** (Timer Event) - Aguarda data programada
7. **Executar Go-Live** (Service Task) - Ativação automática do contrato
8. **Monitoramento Pós-Implantação** (User Task) - Acompanhamento das primeiras semanas
9. **Coletar NPS da Implantação** (Service Task) - Pesquisa automática aos 30 dias
10. **Implantação Concluída** (Signal Event) - Dispara gestão de grandes contas

**KPIs Medidos:**
- Tempo médio de implantação completa (dias)
- % de implantações concluídas no prazo acordado
- % de cadastros realizados sem erros na primeira tentativa
- NPS da experiência de implantação (primeiros 30 dias)

---

### SUBPROCESSO 5: Gestão Estratégica de Grandes Contas (OPE-04 Pós-Venda)

**Objetivo:** Maximizar retenção, satisfação e expansão de clientes corporativos.

**Representação Visual no BPMN:**
- Subprocesso `SubProcess_GestaoGrandesContas`
- Tarefas de usuário para acompanhamento e identificação de oportunidades
- Gateway para decisão de expansão de contrato

**Etapas:**
1. **Acompanhamento Estratégico de Conta** (User Task) - Business reviews, visitas trimestrais
2. **Identificar Oportunidades Cross-Sell/Up-Sell** (User Task) - Análise de filiais, subsidiárias, novos produtos
3. **Gerir Renovação de Contrato** (User Task) - Análise de sinistralidade, reajuste, renegociação
4. **Gateway Oportunidade de Expansão** - Se expansão detectada → Signal para nova implantação

**KPI Medido:** % de visitas em clientes com mais de 100 vidas por trimestre

---

### SUBPROCESSO 6: Desenvolvimento e Lançamento de Produtos (OPE-05)

**Objetivo:** Desenvolver e lançar produtos de forma ágil em resposta às necessidades do mercado.

**Representação Visual no BPMN:**
- Subprocesso disparado por evento (triggered by event)
- Signal Start Event para demanda de novo produto
- Tarefas de usuário e serviço para cada etapa do desenvolvimento

**Etapas:**
1. **Demanda de Novo Produto Identificada** (Signal Event) - Origem: vendas, expansão ou mercado
2. **Pesquisa de Mercado** (User Task) - Análise de tendências e oportunidades
3. **Conceber e Desenhar Produto** (User Task) - Design de features e benefícios
4. **Analisar Viabilidade** (User Task) - Business case técnico e financeiro
5. **Gateway Produto Viável** - Decisão de continuar ou descartar
6. **Desenvolver Protótipos e Testar** (User Task) - Validação com early adopters
7. **Precificar Produto** (User Task) - Modelagem atuarial
8. **Registrar Produto na ANS** (User Task) - Cumprimento regulatório
9. **Lançar Produto no Mercado** (Service Task) - Comercialização e acompanhamento

**KPI Medido:** Time-to-market de novos produtos (dias)

---

## 2.3 Contribuição para Geração de Receita e Expansão

### Aceleração de Receita

| Etapa | Contribuição para Receita |
|---|---|
| Expansão Geográfica | Habilita novos mercados, multiplicando o TAM (Total Addressable Market) |
| Geração de Demanda | Preenche o funil de vendas com leads qualificados |
| Vendas Consultivas | Converte oportunidades em contratos, gerando receita contratada |
| Implantação | Ativa vidas rapidamente, antecipando início de faturamento |
| Gestão de Contas | Maximiza LTV via renovação, cross-sell e up-sell |
| Desenvolvimento de Produtos | Cria novas fontes de receita via inovação |

### Redução de Churn e Time-to-Revenue

- **Implantação estruturada**: Reduz churn inicial por problemas operacionais
- **NPS de implantação**: Identifica e corrige problemas precocemente
- **Go-live programado**: Permite previsibilidade de faturamento
- **Gestão proativa de contas**: Antecipa riscos de cancelamento

---

## 2.4 Mapeamento Visual no Diagrama BPMN

Todos os elementos descritos estão representados visualmente no arquivo BPMN:

| Elemento do Processo | Representação no BPMN | ID no XML |
|---|---|---|
| Pools de participantes | Pool (Mercado, Cliente, AUSTA, Sistemas) | Pool_* |
| Áreas funcionais | Lanes (8 raias de responsabilidade) | Lane_* |
| Etapas de OPE-04 | User Tasks, Service Tasks, Gateways | Task_*, Gateway_* |
| Etapas de OPE-05 | Subprocesso disparado por evento | SubProcess_DesenvolvimentoProdutos |
| Etapas de OPE-09 | Subprocesso com parallelismo | SubProcess_Implantacao |
| Etapas de OPE-11 | Subprocesso sequencial | SubProcess_ExpansaoGeografica |
| Integrações de sistema | Message Flows, Service Tasks | MsgFlow_*, Task_*Service |
| Documentos e dados | Data Objects, Data Stores | DataObject_*, DataStore_* |
| Pontos de medição de KPI | Text Annotations | Annotation_KPI_* |
| Decisões de negócio | Exclusive/Parallel Gateways | Gateway_* |
| Eventos de comunicação | Message, Signal, Timer Events | Event_* |

---

# PARTE 3: ESPECIFICAÇÕES TÉCNICAS DO ARQUIVO BPMN

## 3.1 Estrutura do Arquivo

O arquivo BPMN está estruturado conforme o padrão BPMN 2.0 e compatível com Camunda 7:

```
bpmn:definitions
├── bpmn:collaboration (Colaboração entre pools)
│   ├── bpmn:participant (4 pools)
│   └── bpmn:messageFlow (9 fluxos de mensagem)
├── bpmn:process (Processo principal)
│   ├── bpmn:laneSet (8 lanes)
│   ├── bpmn:startEvent (3 eventos de início)
│   ├── bpmn:subProcess (6 subprocessos)
│   ├── bpmn:task (30+ tarefas)
│   ├── bpmn:gateway (15+ gateways)
│   ├── bpmn:dataStoreReference (1 CRM)
│   ├── bpmn:dataObjectReference (4 objetos de dados)
│   └── bpmn:textAnnotation (6 anotações de KPI)
├── bpmn:message (4 definições)
├── bpmn:signal (5 definições)
├── bpmn:error (1 definição)
└── bpmndi:BPMNDiagram (Diagrama visual)
```

## 3.2 Nomenclatura Padronizada

| Prefixo | Tipo de Elemento | Exemplo |
|---|---|---|
| Event_ | Eventos | Event_LeadRecebido, Event_ContratoAssinado |
| Task_ | Tarefas | Task_QualificarLeadsSAL, Task_PlanejarImplantacao |
| Gateway_ | Gateways | Gateway_LeadQualificado, Gateway_GoLiveReadiness |
| SubProcess_ | Subprocessos | SubProcess_VendasConsultivas |
| CallActivity_ | Call Activities | CallActivity_AnalisarRisco |
| DataObject_ | Objetos de Dados | DataObject_Proposta, DataObject_BusinessCase |
| DataStore_ | Data Stores | DataStore_CRM |
| Flow_ | Sequence Flows | Flow_LeadParaQualificacao |
| MsgFlow_ | Message Flows | MsgFlow_EnvioPropostaCliente |
| Pool_ | Pools | Pool_ClienteCorporativo |
| Lane_ | Lanes | Lane_VendasConsultivas |
| Annotation_ | Anotações | Annotation_KPI_VidasSobCuidado |
| Signal_ | Sinais | Signal_ContratoFechado |
| Message_ | Mensagens | Message_LeadInbound |
| Error_ | Erros | Error_ValidacaoCadastral |

## 3.3 Extensões Camunda

O arquivo utiliza extensões Camunda para:

- **camunda:assignee**: Atribuição de tarefas a cargos específicos
- **camunda:candidateGroups**: Grupos de candidatos para tarefas
- **camunda:delegateExpression**: Expressões para tarefas de serviço
- **camunda:formData**: Formulários para tarefas de usuário

## 3.4 Validação e Importação

Para importar no Camunda Modeler ou Camunda Platform 7:

1. Abrir o Camunda Modeler
2. File → Open → Selecionar arquivo BPMN
3. Validar diagrama (ctrl+shift+v)
4. Fazer deploy para Camunda Engine

O arquivo está pronto para execução em ambiente Camunda 7, necessitando apenas:
- Implementação dos delegates (Java/Spring) para Service Tasks
- Configuração de conectores para integrações externas
- Definição de formulários para User Tasks (se não usar os genéricos)

---

# CONCLUSÃO

Este processo BPMN representa uma **máquina integrada de Expansão e Vendas B2B** que:

1. **Integra os 4 processos estratégicos** (OPE-04, OPE-05, OPE-09, OPE-11) em um fluxo coerente
2. **Automatiza handoffs** entre vendas, implantação e gestão de contas via eventos BPMN
3. **Implementa todos os KPIs e medidas de direção** do Manual de Processos AUSTA
4. **Orquestra automações** com CRM, Marketing Automation, Tasy ERP e ferramentas de gestão
5. **Trata exceções** como perda de oportunidade, erros de validação e atrasos de implantação
6. **Cria ciclos de feedback** entre resultados comerciais e desenvolvimento de produtos
7. **É tecnicamente implementável** no Camunda 7 com todas as extensões necessárias

O processo maximiza a **geração de receita**, **acelera o time-to-revenue**, **reduz churn inicial** e **escala a operação** de forma estruturada e mensurável.
