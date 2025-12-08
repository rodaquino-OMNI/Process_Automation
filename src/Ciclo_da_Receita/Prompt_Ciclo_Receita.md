# PROMPT TÉCNICO: Geração de Processos BPMN - Ciclo de Receita Hospital do Futuro

## CONTEXTO E OBJETIVO

Você é um especialista em modelagem BPMN 2.0 e automação de processos hospitalares. Sua tarefa é criar **11 arquivos BPMN** completos e funcionais para o Ciclo de Receita do Hospital do Futuro:

1. **1 Processo Orquestrador Principal** (`ORCH_Ciclo_Receita_Hospital_Futuro.bpmn`)
2. **10 Subprocessos** correspondentes às etapas críticas (`SUB_01_*.bpmn` até `SUB_10_*.bpmn`)

Todos os arquivos devem ser **100% compatíveis com Camunda Platform 7** e conter **todos os elementos visuais (BPMNDiagram/BPMNShape/BPMNEdge)** para renderização correta no Camunda Modeler.

---

## ESPECIFICAÇÕES TÉCNICAS OBRIGATÓRIAS

### Estrutura XML Base

```xml
<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions 
  xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:di="http://www.omg.org/spec/DD/20100524/DI"
  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  id="Definitions_[ID_UNICO]"
  targetNamespace="http://bpmn.io/schema/bpmn"
  exporter="Camunda Modeler"
  exporterVersion="5.20.0">
```

### Requisitos Camunda 7

| Atributo | Requisito |
|----------|-----------|
| `isExecutable` | `true` em todos os processos |
| `camunda:historyTimeToLive` | Obrigatório (usar `P365D` ou `730`) |
| `camunda:asyncBefore` | `true` em Service Tasks para resiliência |
| `camunda:jobPriority` | Definir para tasks críticas |
| Expressões | Usar `${variavel}` (não `#{variavel}`) |
| Conectores | Usar `camunda:connector` com `camunda:connectorId` |

### Padrões de Nomenclatura

| Elemento | Padrão | Exemplo |
|----------|--------|---------|
| Process ID | `Process_[Codigo]_[Nome]` | `Process_ORCH_Revenue_Cycle` |
| Participant ID | `Participant_[Nome]` | `Participant_Hospital` |
| Lane ID | `Lane_[Numero]_[Nome]` | `Lane_01_First_Contact` |
| Task ID | `Task_[Acao]_[Objeto]` | `Task_Validate_Eligibility` |
| Gateway ID | `Gateway_[Decisao]` | `Gateway_Authorization_Status` |
| Event ID | `Event_[Tipo]_[Nome]` | `Event_Start_Patient_Contact` |
| Sequence Flow ID | `Flow_[Origem]_to_[Destino]` | `Flow_Task1_to_Gateway1` |
| Message Flow ID | `MsgFlow_[Origem]_[Destino]` | `MsgFlow_Hospital_Insurance` |

### Dimensões Visuais Padrão (em pixels)

| Elemento | Largura | Altura |
|----------|---------|--------|
| Task (qualquer tipo) | 100 | 80 |
| Gateway | 50 | 50 |
| Start/End Event | 36 | 36 |
| Intermediate Event | 36 | 36 |
| Boundary Event | 36 | 36 |
| Subprocess (collapsed) | 100 | 80 |
| Subprocess (expanded) | 350-600 | 200-400 |
| Lane | largura do pool | 150-250 |
| Pool/Participant | 2000-6000 | soma das lanes |
| Text Annotation | 100-200 | 30-50 |

### Espaçamento Padrão

- Entre elementos horizontais: **150-200px**
- Entre elementos verticais: **100-150px**
- Margem inicial do pool: **x=160, y=80**
- Início do primeiro elemento na lane: **x=200**

---

## PROCESSO ORQUESTRADOR PRINCIPAL

### Arquivo: `ORCH_Ciclo_Receita_Hospital_Futuro.bpmn`

### Estrutura de Colaboração

```
<bpmn:collaboration id="Collaboration_Revenue_Cycle_Orchestrator">
  <!-- Participantes Internos -->
  <bpmn:participant id="Participant_Hospital" name="Hospital - Ciclo de Receita" processRef="Process_ORCH_Revenue_Cycle" />
  
  <!-- Participantes Externos (Black Box) -->
  <bpmn:participant id="Participant_Patient" name="Paciente / Responsável" />
  <bpmn:participant id="Participant_Insurance" name="Operadora de Saúde" />
  <bpmn:participant id="Participant_TASY" name="Sistema TASY ERP" />
  <bpmn:participant id="Participant_Government" name="Órgãos Reguladores (ANS/RF)" />
  <bpmn:participant id="Participant_Bank" name="Instituições Financeiras" />
  
  <!-- Message Flows entre participantes -->
  <bpmn:messageFlow id="MsgFlow_Patient_Request" sourceRef="Participant_Patient" targetRef="Event_Start_Patient_Contact" />
  <!-- ... demais message flows ... -->
</bpmn:collaboration>
```

### Lanes do Processo Principal (10 lanes)

| Lane ID | Nome | Cor Sugerida | Subprocesso Chamado |
|---------|------|--------------|---------------------|
| `Lane_01_First_Contact` | 1. Primeiro Contato / Agendamento | #E8F6F3 | `SUB_01_First_Contact` |
| `Lane_02_Pre_Authorization` | 2. Pré-Autorização / Elegibilidade | #EBF5FB | `SUB_02_Pre_Authorization` |
| `Lane_03_Admission` | 3. Admissão / Registro | #F4ECF7 | `SUB_03_Admission` |
| `Lane_04_Clinical_Production` | 4. Produção Assistencial | #FEF9E7 | `SUB_04_Clinical_Production` |
| `Lane_05_Coding_Audit` | 5. Codificação / Auditoria | #FDEDEC | `SUB_05_Coding_Audit` |
| `Lane_06_Billing_Submission` | 6. Fechamento / Envio | #F0F3F4 | `SUB_06_Billing_Submission` |
| `Lane_07_Denials_Management` | 7. Gestão de Glosas | #FDF2E9 | `SUB_07_Denials_Management` |
| `Lane_08_Revenue_Collection` | 8. Recebimento / Conciliação | #E8F8F5 | `SUB_08_Revenue_Collection` |
| `Lane_09_Analytics` | 9. Analytics / BI | #EAF2F8 | `SUB_09_Analytics` |
| `Lane_10_Maximization` | 10. Maximização de Receita | #F5EEF8 | `SUB_10_Maximization` |

### Elementos do Orquestrador por Lane

Cada lane deve conter:

```xml
<!-- Padrão por Lane -->
<bpmn:callActivity id="CallActivity_SUB_XX" name="[Nome do Subprocesso]" calledElement="Process_SUB_XX_[Nome]">
  <bpmn:extensionElements>
    <camunda:in businessKey="#{execution.processBusinessKey}" />
    <camunda:in variables="all" />
    <camunda:out variables="all" />
  </bpmn:extensionElements>
  <bpmn:incoming>Flow_entrada</bpmn:incoming>
  <bpmn:outgoing>Flow_saida</bpmn:outgoing>
</bpmn:callActivity>
```

### Fluxo do Orquestrador

```
[Start Event: Contato Paciente]
    ↓
[Call Activity: SUB_01_First_Contact]
    ↓
[Gateway: Tem Convênio?]
    ├─ Sim → [Call Activity: SUB_02_Pre_Authorization]
    └─ Não → [Task: Registro Particular] → continua
    ↓
[Call Activity: SUB_03_Admission]
    ↓
[Call Activity: SUB_04_Clinical_Production]
    ↓
[Call Activity: SUB_05_Coding_Audit]
    ↓
[Call Activity: SUB_06_Billing_Submission]
    ↓
[Gateway: Houve Glosa?]
    ├─ Sim → [Call Activity: SUB_07_Denials_Management] → volta para análise
    └─ Não → continua
    ↓
[Call Activity: SUB_08_Revenue_Collection]
    ↓
[Parallel Gateway: Split]
    ├─ [Call Activity: SUB_09_Analytics] (assíncrono)
    └─ [Call Activity: SUB_10_Maximization] (assíncrono)
    ↓
[Parallel Gateway: Join]
    ↓
[End Event: Ciclo Completo]
```

### Eventos de Borda (Boundary Events) Obrigatórios

- Timer Event em `SUB_02_Pre_Authorization`: Timeout de autorização (48h)
- Error Event em `SUB_06_Billing_Submission`: Falha de transmissão
- Signal Event em `SUB_04_Clinical_Production`: Alta do paciente
- Escalation Event em `SUB_07_Denials_Management`: Prazo ANS crítico

---

## ESPECIFICAÇÃO DOS 10 SUBPROCESSOS

### SUB_01: Primeiro Contato e Agendamento

**Arquivo:** `SUB_01_First_Contact.bpmn`

**Process ID:** `Process_SUB_01_First_Contact`

**Lanes:**
1. `Lane_Digital_Channels` - Canais Digitais (WhatsApp/Portal/App)
2. `Lane_Call_Center` - Central de Atendimento
3. `Lane_TASY_Scheduling` - Sistema TASY Agendamento

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Contact` | Start Event (Message) | Solicitação Recebida | Trigger: webhook WhatsApp/Portal |
| `Task_Identify_Channel` | Service Task | Identificar Canal de Origem | RPA: classificação automática |
| `Task_Capture_Data` | User Task | Capturar Dados do Paciente | Form: campos básicos |
| `Gateway_Patient_Exists` | Exclusive Gateway | Paciente Cadastrado? | Condição: `${patientExists}` |
| `Task_Create_Patient` | Service Task | Criar Cadastro | API: TASY Patient Create |
| `Task_Identify_Service` | Service Task | Identificar Serviço | IA: NLP classificação |
| `Task_Check_Availability` | Service Task | Verificar Disponibilidade | API: TASY Schedule Query |
| `Gateway_Slot_Available` | Exclusive Gateway | Horário Disponível? | Condição: `${slotAvailable}` |
| `Task_Add_Waiting_List` | Service Task | Adicionar Lista de Espera | API: TASY Waiting List |
| `Task_Book_Appointment` | Service Task | Confirmar Agendamento | API: TASY Schedule Book |
| `Task_Send_Confirmation` | Service Task | Enviar Confirmação | RPA: WhatsApp/SMS/Email |
| `Event_Timer_Reminder` | Timer Boundary Event | Lembrete 24h | Timer: `R/PT24H` |
| `Task_Send_Reminder` | Service Task | Enviar Lembrete | RPA: multicanal |
| `Event_End_Scheduled` | End Event | Agendamento Concluído | - |

**Variáveis de Processo:**
```json
{
  "patientId": "String",
  "patientName": "String",
  "patientCPF": "String",
  "contactChannel": "String (WHATSAPP|PORTAL|PHONE|APP)",
  "serviceType": "String",
  "appointmentDateTime": "Date",
  "slotId": "String",
  "insuranceId": "String",
  "appointmentId": "String"
}
```

---

### SUB_02: Pré-Autorização e Elegibilidade

**Arquivo:** `SUB_02_Pre_Authorization.bpmn`

**Process ID:** `Process_SUB_02_Pre_Authorization`

**Lanes:**
1. `Lane_Eligibility` - Verificação de Elegibilidade
2. `Lane_Authorization` - Autorização
3. `Lane_RPA_Portals` - RPA Portais Operadoras
4. `Lane_Appeals` - Recursos e Negativas

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Auth` | Start Event | Início Autorização | - |
| `Task_TASY_Eligibility` | Service Task | Verificar Elegibilidade TASY | API: TASY Eligibility Check |
| `Task_RPA_Portal_Check` | Service Task | Consultar Portal Operadora | RPA IBM: Bot Elegibilidade |
| `Gateway_Eligible` | Exclusive Gateway | Elegível? | Condição: `${isEligible}` |
| `Task_Calculate_Copay` | Service Task | Calcular Coparticipação | Motor de Regras |
| `Task_Generate_TISS` | Service Task | Gerar Guia TISS | API: TASY TISS Generator |
| `Task_Submit_Auth` | Service Task | Enviar Solicitação | RPA: submit portal/webservice |
| `Event_Timer_Auth_Timeout` | Timer Boundary Event | Timeout 48h | Timer: `PT48H` |
| `Gateway_Auth_Status` | Exclusive Gateway | Status Autorização | Condição: `${authStatus}` |
| `Task_LLM_Appeal` | Service Task | Gerar Recurso (LLM) | API: LLM Analysis |
| `Task_Submit_Appeal` | Service Task | Enviar Recurso | RPA: submit recurso |
| `Task_Update_Status` | Service Task | Atualizar Status TASY | API: TASY Auth Update |
| `Event_End_Authorized` | End Event | Autorizado | - |
| `Event_End_Denied` | End Event (Error) | Negado Definitivo | - |

**Variáveis de Processo:**
```json
{
  "insuranceId": "String",
  "insuranceName": "String",
  "planCode": "String",
  "procedureCodes": "List<String>",
  "isEligible": "Boolean",
  "copayAmount": "Double",
  "tissGuideNumber": "String",
  "authorizationNumber": "String",
  "authStatus": "String (PENDING|APPROVED|DENIED|PARTIAL)",
  "denialReason": "String",
  "appealText": "String",
  "appealNumber": "String"
}
```

**Conectores Camunda:**

```xml
<bpmn:serviceTask id="Task_RPA_Portal_Check" name="Consultar Portal Operadora">
  <bpmn:extensionElements>
    <camunda:connector>
      <camunda:inputOutput>
        <camunda:inputParameter name="insuranceId">${insuranceId}</camunda:inputParameter>
        <camunda:inputParameter name="patientCPF">${patientCPF}</camunda:inputParameter>
        <camunda:inputParameter name="procedureCodes">${procedureCodes}</camunda:inputParameter>
      </camunda:inputOutput>
      <camunda:connectorId>ibm-rpa-eligibility-bot</camunda:connectorId>
    </camunda:connector>
  </bpmn:extensionElements>
</bpmn:serviceTask>
```

---

### SUB_03: Admissão e Registro

**Arquivo:** `SUB_03_Admission.bpmn`

**Process ID:** `Process_SUB_03_Admission`

**Lanes:**
1. `Lane_Self_Service` - Autoatendimento (Totem/App)
2. `Lane_Reception` - Recepção
3. `Lane_TASY_ADT` - TASY ADT (Admissão)

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Arrival` | Start Event | Chegada do Paciente | - |
| `Gateway_Checkin_Type` | Exclusive Gateway | Tipo de Check-in | Condição: `${checkinType}` |
| `Task_Self_Checkin` | User Task | Check-in Autoatendimento | Form: Totem/App |
| `Task_Manual_Checkin` | User Task | Check-in Recepção | Form: TASY ADT |
| `Task_Biometric_Auth` | Service Task | Autenticação Biométrica | API: Biometria |
| `Task_Document_OCR` | Service Task | Digitalizar Documentos | RPA: OCR documentos |
| `Task_CPF_Validation` | Service Task | Validar CPF (RF) | API: Receita Federal |
| `Task_Credit_Check` | Service Task | Consulta Crédito | API: Serasa/SPC |
| `Task_Cost_Estimate` | Service Task | Gerar Estimativa | Motor de Regras |
| `Task_Digital_Consent` | User Task | Aceite Digital | Form: termos e consentimento |
| `Task_TASY_Admission` | Service Task | Registrar Admissão TASY | API: TASY ADT Create |
| `Task_Generate_Bracelet` | Service Task | Gerar Pulseira QR/RFID | API: impressora + RFID |
| `Task_Assign_Room` | Service Task | Designar Leito/Sala | API: TASY Bed Management |
| `Task_Notify_Team` | Service Task | Notificar Equipe | API: Push notification |
| `Event_End_Admitted` | End Event | Paciente Admitido | - |

**Subprocesso Embarcado: Admissão de Emergência**

```xml
<bpmn:subProcess id="SubProcess_Emergency" name="Admissão Emergência" triggeredByEvent="true">
  <bpmn:startEvent id="Event_Emergency_Start" name="Emergência">
    <bpmn:signalEventDefinition signalRef="Signal_Emergency" />
  </bpmn:startEvent>
  <!-- Fluxo simplificado de emergência -->
  <bpmn:task id="Task_Fast_Track" name="Fast Track Admissão" />
  <bpmn:endEvent id="Event_Emergency_End" />
</bpmn:subProcess>
```

---

### SUB_04: Produção Assistencial e Documentação Clínica

**Arquivo:** `SUB_04_Clinical_Production.bpmn`

**Process ID:** `Process_SUB_04_Clinical_Production`

**Lanes:**
1. `Lane_Medical_Team` - Equipe Médica
2. `Lane_Nursing` - Enfermagem
3. `Lane_Pharmacy` - Farmácia
4. `Lane_IoT_RFID` - Captura IoT/RFID
5. `Lane_Integration` - Integração (LIS/PACS)

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Care` | Start Event | Início Atendimento | - |
| `Task_Medical_Eval` | User Task | Avaliação Médica | Form: PEP |
| `Task_Register_CID` | Service Task | Registrar CID | API: TASY + sugestão IA |
| `Task_Create_Orders` | User Task | Criar Ordens de Serviço | Form: TASY Orders |
| `Task_Prescribe` | User Task | Prescrição | Form: TASY Prescription |
| `Task_RFID_Capture` | Service Task | Captura RFID Materiais | IoT: RFID readers |
| `Task_Weight_Sensor` | Service Task | Sensor de Peso Dispensário | IoT: Weight sensors |
| `Task_Medication_Admin` | User Task | Administrar Medicação | Form: código de barras |
| `Task_Nursing_Evolution` | User Task | Evolução Enfermagem | Form: PEP Nursing |
| `Task_Medical_Evolution` | User Task | Evolução Médica | Form: PEP Medical |
| `Task_LIS_Integration` | Service Task | Integrar Resultados Lab | API: LIS → TASY |
| `Task_PACS_Integration` | Service Task | Integrar Laudos Imagem | API: PACS → TASY |
| `Event_Signal_Discharge` | Intermediate Signal Event | Sinal de Alta | Signal: `Signal_Discharge` |
| `Task_Discharge_Summary` | User Task | Sumário de Alta | Form: PEP Discharge |
| `Event_End_Care` | End Event | Atendimento Concluído | - |

**Event Subprocess para Auditoria Concorrente:**

```xml
<bpmn:subProcess id="SubProcess_Concurrent_Audit" triggeredByEvent="true">
  <bpmn:startEvent id="Event_Audit_Timer">
    <bpmn:timerEventDefinition>
      <bpmn:timeCycle>R/PT1H</bpmn:timeCycle> <!-- A cada 1 hora -->
    </bpmn:timerEventDefinition>
  </bpmn:startEvent>
  <bpmn:serviceTask id="Task_Run_Audit_Rules" name="Executar Regras de Auditoria" />
  <bpmn:exclusiveGateway id="Gateway_Audit_Issues" name="Inconsistências?" />
  <bpmn:serviceTask id="Task_Create_Alert" name="Criar Alerta" />
  <bpmn:endEvent id="Event_Audit_End" />
</bpmn:subProcess>
```

---

### SUB_05: Codificação e Auditoria Interna

**Arquivo:** `SUB_05_Coding_Audit.bpmn`

**Process ID:** `Process_SUB_05_Coding_Audit`

**Lanes:**
1. `Lane_AI_Coding` - Codificação Automática (IA)
2. `Lane_Human_Coding` - Codificadores
3. `Lane_Audit` - Auditoria Interna
4. `Lane_Quality` - Qualidade

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Coding` | Start Event | Conta Pronta para Codificação | - |
| `Task_AI_TUSS_Suggestion` | Service Task | Sugestão TUSS por IA | API: LLM Coding |
| `Task_AI_DRG_Coding` | Service Task | Codificação DRG | API: DRG Engine |
| `Task_Validate_CID_Proc` | Service Task | Validar CID x Procedimento | Motor de Regras |
| `Gateway_AI_Confidence` | Exclusive Gateway | Confiança IA > 95%? | Condição: `${aiConfidence > 0.95}` |
| `Task_Human_Review` | User Task | Revisão Humana | Form: Coding Review |
| `Task_Completeness_Check` | Service Task | Verificar Completude | Motor de Regras |
| `Gateway_Complete` | Exclusive Gateway | Documentação Completa? | Condição: `${isComplete}` |
| `Task_Request_Docs` | Service Task | Solicitar Documentos | API: Notification |
| `Task_Internal_Audit` | Service Task | Auditoria Interna | Motor de Regras |
| `Gateway_Audit_Pass` | Exclusive Gateway | Aprovado Auditoria? | Condição: `${auditPassed}` |
| `Task_Apply_Corrections` | Service Task | Aplicar Correções | API: TASY Update |
| `Task_Quality_Score` | Service Task | Calcular Score Qualidade | Motor de Regras |
| `Event_End_Coded` | End Event | Codificação Concluída | - |

**Variáveis de Processo:**
```json
{
  "accountId": "String",
  "suggestedTUSSCodes": "List<Object>",
  "suggestedDRG": "String",
  "aiConfidence": "Double",
  "cidCodes": "List<String>",
  "isComplete": "Boolean",
  "missingDocs": "List<String>",
  "auditPassed": "Boolean",
  "auditFindings": "List<Object>",
  "qualityScore": "Double"
}
```

---

### SUB_06: Fechamento e Envio de Contas

**Arquivo:** `SUB_06_Billing_Submission.bpmn`

**Process ID:** `Process_SUB_06_Billing_Submission`

**Lanes:**
1. `Lane_Billing` - Faturamento
2. `Lane_TISS_Engine` - Motor TISS
3. `Lane_Transmission` - Transmissão
4. `Lane_Monitoring` - Monitoramento

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Billing` | Start Event | Início Fechamento | - |
| `Task_Consolidate_Charges` | Service Task | Consolidar Lançamentos | API: TASY Billing |
| `Task_Apply_Contract_Rules` | Service Task | Aplicar Regras Contratuais | Motor de Regras |
| `Task_Calculate_Values` | Service Task | Calcular Valores | Motor de Regras |
| `Task_Group_By_Guide` | Service Task | Agrupar por Guia/Lote | API: TASY |
| `Task_Pre_Validation` | Service Task | Validação Pré-Envio | Motor de Regras TISS |
| `Gateway_Valid` | Exclusive Gateway | XML Válido? | Condição: `${isValid}` |
| `Task_Fix_Errors` | User Task | Corrigir Erros | Form: Error Fix |
| `Task_Generate_TISS_Batch` | Service Task | Gerar Lote TISS | API: TISS Generator |
| `Task_Submit_Webservice` | Service Task | Enviar Webservice | API: Operadora WS |
| `Task_Submit_Portal` | Service Task | Enviar Portal | RPA: Portal Upload |
| `Gateway_Submission_Type` | Exclusive Gateway | Tipo de Envio | Condição: `${submissionType}` |
| `Task_Capture_Protocol` | Service Task | Capturar Protocolo | API/RPA: Response Parser |
| `Event_Error_Transmission` | Boundary Error Event | Erro Transmissão | Error: `Error_Transmission` |
| `Task_Retry_Submission` | Service Task | Retry Automático | Retry: 3x com backoff |
| `Task_Update_Status` | Service Task | Atualizar Status | API: TASY Billing Status |
| `Event_End_Submitted` | End Event | Conta Enviada | - |

---

### SUB_07: Gestão de Glosas e Recursos

**Arquivo:** `SUB_07_Denials_Management.bpmn`

**Process ID:** `Process_SUB_07_Denials_Management`

**Lanes:**
1. `Lane_Capture` - Captura de Glosas
2. `Lane_Analysis` - Análise
3. `Lane_LLM_Appeals` - Recursos (LLM)
4. `Lane_Negotiation` - Negociação

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Denial` | Start Event (Message) | Glosa Recebida | Trigger: portal/arquivo |
| `Task_RPA_Capture_Denials` | Service Task | Capturar Glosas | RPA: Portal scraping |
| `Task_Classify_Denial` | Service Task | Classificar Glosa | IA: classificação |
| `Gateway_Denial_Type` | Exclusive Gateway | Tipo de Glosa | Condição: `${denialType}` |
| `Task_Auto_Correct` | Service Task | Correção Automática | Motor de Regras |
| `Task_LLM_Analysis` | Service Task | Análise LLM | API: LLM Analysis |
| `Task_Search_Evidence` | Service Task | Buscar Evidências | API: TASY Docs |
| `Task_Generate_Appeal` | Service Task | Gerar Recurso | API: LLM Appeal Gen |
| `Task_Human_Review_Appeal` | User Task | Revisar Recurso | Form: Appeal Review |
| `Task_Submit_Appeal` | Service Task | Enviar Recurso | RPA: Portal Submit |
| `Event_Timer_ANS_Deadline` | Timer Boundary Event | Prazo ANS | Timer: calculado |
| `Task_Escalate` | Service Task | Escalar | API: Escalation |
| `Task_Track_Response` | Service Task | Acompanhar Resposta | RPA: Status Check |
| `Gateway_Appeal_Result` | Exclusive Gateway | Resultado Recurso | Condição: `${appealResult}` |
| `Task_Register_Recovery` | Service Task | Registrar Recuperação | API: TASY Financial |
| `Task_Register_Loss` | Service Task | Registrar Perda | API: TASY Financial |
| `Event_End_Resolved` | End Event | Glosa Resolvida | - |

---

### SUB_08: Recebimento e Conciliação Financeira

**Arquivo:** `SUB_08_Revenue_Collection.bpmn`

**Process ID:** `Process_SUB_08_Revenue_Collection`

**Lanes:**
1. `Lane_Bank_Integration` - Integração Bancária
2. `Lane_Reconciliation` - Conciliação
3. `Lane_AR_Management` - Contas a Receber
4. `Lane_Collection` - Cobrança

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Payment` | Start Event (Timer) | Ciclo de Conciliação | Timer: `0 6 * * *` (6h diário) |
| `Task_Process_CNAB` | Service Task | Processar CNAB | RPA: CNAB Parser |
| `Task_Process_PIX` | Service Task | Processar PIX | API: PIX Integration |
| `Task_Auto_Matching` | Service Task | Matching Automático | Motor de Regras |
| `Gateway_Match_Found` | Exclusive Gateway | Match Encontrado? | Condição: `${matchFound}` |
| `Task_Manual_Matching` | User Task | Matching Manual | Form: Reconciliation |
| `Task_Allocate_Payment` | Service Task | Alocar Pagamento | API: TASY Financial |
| `Task_Analyze_Difference` | Service Task | Analisar Diferença | Motor de Regras |
| `Gateway_Difference_Type` | Exclusive Gateway | Tipo de Diferença | Condição: `${differenceType}` |
| `Task_Create_Provision` | Service Task | Criar Provisão | API: TASY Accounting |
| `Task_Aging_Analysis` | Service Task | Análise de Aging | Motor de Regras |
| `Task_Collection_Workflow` | Service Task | Régua de Cobrança | RPA: multicanal |
| `Task_Negativation` | Service Task | Negativar SPC/Serasa | API: Credit Bureau |
| `Task_Legal_Referral` | Service Task | Encaminhar Jurídico | API: Legal System |
| `Task_Write_Off` | Service Task | Provisão para Perda | API: TASY Accounting |
| `Event_End_Collected` | End Event | Recebimento Concluído | - |

---

### SUB_09: Analytics e Business Intelligence

**Arquivo:** `SUB_09_Analytics.bpmn`

**Process ID:** `Process_SUB_09_Analytics`

**Lanes:**
1. `Lane_Data_Collection` - Coleta de Dados
2. `Lane_Processing` - Processamento
3. `Lane_KPI_Engine` - Motor de KPIs
4. `Lane_Reporting` - Relatórios e Alertas

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Analytics` | Start Event (Timer) | Ciclo Analytics | Timer: `*/5 * * * *` (5min) |
| `Task_Collect_TASY` | Service Task | Coletar Dados TASY | API: TASY Data Export |
| `Task_Collect_RPA_Logs` | Service Task | Coletar Logs RPA | API: RPA Platform |
| `Task_Collect_External` | Service Task | Coletar Dados Externos | API: External feeds |
| `Task_Data_Quality` | Service Task | Validar Qualidade | Motor de Regras |
| `Task_Stream_Processing` | Service Task | Processamento Stream | Kafka/Spark |
| `Task_Batch_Processing` | Service Task | Processamento Batch | Spark/ETL |
| `Task_Data_Lake_Update` | Service Task | Atualizar Data Lake | API: Data Lake |
| `Task_Calculate_KPIs` | Service Task | Calcular KPIs | Motor de KPIs |
| `Task_ML_Anomaly` | Service Task | Detecção de Anomalias | ML: Isolation Forest |
| `Task_ML_Prediction` | Service Task | Previsões | ML: Time Series |
| `Gateway_Anomaly_Detected` | Exclusive Gateway | Anomalia Detectada? | Condição: `${anomalyDetected}` |
| `Task_Create_Alert` | Service Task | Criar Alerta | API: Notification |
| `Task_Update_Dashboard` | Service Task | Atualizar Dashboard | API: Power BI |
| `Task_Generate_Reports` | Service Task | Gerar Relatórios | RPA: Report Gen |
| `Event_End_Analytics` | End Event | Ciclo Concluído | - |

---

### SUB_10: Maximização de Receita e Melhoria Contínua

**Arquivo:** `SUB_10_Maximization.bpmn`

**Process ID:** `Process_SUB_10_Maximization`

**Lanes:**
1. `Lane_Opportunity_Analysis` - Análise de Oportunidades
2. `Lane_VBHC` - Desenvolvimento VBHC
3. `Lane_Process_Mining` - Process Mining
4. `Lane_Continuous_Improvement` - Melhoria Contínua

**Elementos Obrigatórios:**

| ID | Tipo | Nome | Automação |
|----|------|------|-----------|
| `Event_Start_Maximization` | Start Event (Timer) | Ciclo Maximização | Timer: semanal |
| `Task_Identify_Upsell` | Service Task | Identificar Upsell | ML: Recommendation |
| `Task_Analyze_Undercoding` | Service Task | Analisar Subcodificação | ML: Pattern Analysis |
| `Task_Detect_Missed_Charges` | Service Task | Detectar Não Cobrados | Motor de Regras |
| `Task_Benchmark_Analysis` | Service Task | Análise Benchmark | API: External Data |
| `Task_Cost_Analysis` | Service Task | Análise de Custos | API: TASY Cost |
| `Task_Pricing_Simulation` | Service Task | Simulação Precificação | Motor de Regras |
| `Task_Bundle_Creation` | Service Task | Criar Pacotes | Motor de Regras |
| `Task_Margin_Monitoring` | Service Task | Monitorar Margem | Motor de KPIs |
| `Task_Process_Mining` | Service Task | Process Mining | Celonis/ProM |
| `Task_Identify_Bottlenecks` | Service Task | Identificar Gargalos | ML: Process Mining |
| `Task_Generate_Improvements` | Service Task | Gerar Melhorias | LLM: Suggestions |
| `Task_Prioritize_Actions` | Service Task | Priorizar Ações | Motor de Regras |
| `Task_Create_Action_Plan` | User Task | Criar Plano de Ação | Form: Action Plan |
| `Task_Track_Implementation` | Service Task | Acompanhar Implementação | API: Project Mgmt |
| `Event_End_Maximization` | End Event | Ciclo Concluído | - |

---

## REQUISITOS VISUAIS (BPMNDiagram)

### Estrutura Obrigatória do Diagrama

Cada arquivo BPMN deve conter a seção `<bpmndi:BPMNDiagram>` completa:

```xml
<bpmndi:BPMNDiagram id="BPMNDiagram_[ProcessId]">
  <bpmndi:BPMNPlane id="BPMNPlane_[ProcessId]" bpmnElement="[Collaboration_ou_Process_Id]">
    
    <!-- Shape para cada Participant/Pool -->
    <bpmndi:BPMNShape id="[ParticipantId]_di" bpmnElement="[ParticipantId]" isHorizontal="true">
      <dc:Bounds x="160" y="80" width="2000" height="800" />
      <bpmndi:BPMNLabel />
    </bpmndi:BPMNShape>
    
    <!-- Shape para cada Lane -->
    <bpmndi:BPMNShape id="[LaneId]_di" bpmnElement="[LaneId]" isHorizontal="true">
      <dc:Bounds x="190" y="80" width="1970" height="200" />
      <bpmndi:BPMNLabel />
    </bpmndi:BPMNShape>
    
    <!-- Shape para cada Task/Gateway/Event -->
    <bpmndi:BPMNShape id="[ElementId]_di" bpmnElement="[ElementId]">
      <dc:Bounds x="300" y="140" width="100" height="80" />
      <bpmndi:BPMNLabel>
        <dc:Bounds x="305" y="190" width="90" height="40" />
      </bpmndi:BPMNLabel>
    </bpmndi:BPMNShape>
    
    <!-- Edge para cada Sequence Flow -->
    <bpmndi:BPMNEdge id="[FlowId]_di" bpmnElement="[FlowId]">
      <di:waypoint x="400" y="180" />
      <di:waypoint x="500" y="180" />
      <bpmndi:BPMNLabel>
        <dc:Bounds x="440" y="162" width="40" height="14" />
      </bpmndi:BPMNLabel>
    </bpmndi:BPMNEdge>
    
    <!-- Edge para Message Flow (entre pools) -->
    <bpmndi:BPMNEdge id="[MsgFlowId]_di" bpmnElement="[MsgFlowId]">
      <di:waypoint x="400" y="80" />
      <di:waypoint x="400" y="40" />
    </bpmndi:BPMNEdge>
    
  </bpmndi:BPMNPlane>
</bpmndi:BPMNDiagram>
```

### Regras de Posicionamento

1. **Pools externos (Black Box)**: Posicionar acima ou abaixo do pool principal
   - Acima: `y = 0` a `y = 60`
   - Abaixo: `y = altura_pool_principal + 100`

2. **Fluxo horizontal**: Elementos devem fluir da esquerda para direita
   - Incremento X entre elementos: **150-200px**
   - Início: `x = 200` (após margem do pool)

3. **Alinhamento vertical por Lane**:
   - Centro da lane = `y_lane + (altura_lane / 2)`
   - Tasks centralizadas verticalmente na lane

4. **Gateways**: 
   - Centralizar horizontalmente entre elementos adjacentes
   - Labels posicionados acima ou abaixo (não sobrepor)

5. **Waypoints de Sequence Flows**:
   - Conexões horizontais: 2 waypoints
   - Conexões com curva: 3-4 waypoints
   - Conexões entre lanes: waypoints intermediários para curvas suaves

---

## VALIDAÇÃO E QUALIDADE

### Checklist de Validação por Arquivo

- [ ] Todos os elementos têm IDs únicos
- [ ] Todos os elementos referenciados nas Lanes existem (`<bpmn:flowNodeRef>`)
- [ ] Todos os Sequence Flows têm `sourceRef` e `targetRef` válidos
- [ ] Todos os elementos têm Shape correspondente no BPMNDiagram
- [ ] Todos os Flows têm Edge correspondente no BPMNDiagram
- [ ] Coordenadas não se sobrepõem
- [ ] Labels não se sobrepõem a elementos
- [ ] `isExecutable="true"` no processo
- [ ] `camunda:historyTimeToLive` definido
- [ ] Expressões usam `${}` (não `#{}`)

### Teste de Renderização

Após gerar cada arquivo:
1. Abrir no Camunda Modeler
2. Verificar se todos elementos são exibidos
3. Verificar se conexões estão corretas
4. Verificar se labels são legíveis
5. Fazer deploy de teste no Camunda Engine

---

## ENTREGÁVEIS ESPERADOS

| # | Arquivo | Descrição |
|---|---------|-----------|
| 1 | `ORCH_Ciclo_Receita_Hospital_Futuro.bpmn` | Processo Orquestrador Principal |
| 2 | `SUB_01_First_Contact.bpmn` | Primeiro Contato e Agendamento |
| 3 | `SUB_02_Pre_Authorization.bpmn` | Pré-Autorização e Elegibilidade |
| 4 | `SUB_03_Admission.bpmn` | Admissão e Registro |
| 5 | `SUB_04_Clinical_Production.bpmn` | Produção Assistencial |
| 6 | `SUB_05_Coding_Audit.bpmn` | Codificação e Auditoria |
| 7 | `SUB_06_Billing_Submission.bpmn` | Fechamento e Envio |
| 8 | `SUB_07_Denials_Management.bpmn` | Gestão de Glosas |
| 9 | `SUB_08_Revenue_Collection.bpmn` | Recebimento e Conciliação |
| 10 | `SUB_09_Analytics.bpmn` | Analytics e BI |
| 11 | `SUB_10_Maximization.bpmn` | Maximização de Receita |

---

## NOTAS FINAIS

- **Priorize completude sobre simplificação**: Inclua todos os elementos especificados
- **Mantenha consistência**: Use os mesmos padrões de nomenclatura em todos os arquivos
- **Documente decisões**: Use `<bpmn:documentation>` para explicar lógica complexa
- **Pense em manutenção**: Organize o código XML de forma legível
- **Teste incrementalmente**: Valide cada arquivo antes de prosseguir
