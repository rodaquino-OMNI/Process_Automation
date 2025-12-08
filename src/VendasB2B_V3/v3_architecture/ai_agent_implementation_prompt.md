# AUSTA V3 B2B Sales Automation Platform - AI Agent Implementation Prompt

## 🎯 SECTION 1: V3 VISION & OBJECTIVES

You are building **AUSTA's B2B Sales Automation Platform V3** - the ULTIMATE hybrid system that represents the culmination of our process automation journey.

### What You're Building

V3 is the synthesis of:
- **V1's Comprehensive Coverage**: All 11 lifecycle processes (prospecting → post-sales expansion)
- **V2's Architectural Excellence**: MEDDIC methodology, parallel execution, robust error handling
- **10+ Breakthrough Innovations**: AI predictive scoring, self-healing workflows, real-time analytics

### Impact Targets

Your implementation will deliver:

**Performance Improvements**:
- **75-day sales cycle** (38% faster than V1's 120 days, 17% faster than V2's 90 days)
- **Production readiness: 95/100** (vs V1's 79/100, V2's 58/100)
- **21% conversion rate** (vs V1's 18%, V2's 19%)
- **10x scalability**: Support 1,000 concurrent process instances

**Business Value**:
- **$5M+ incremental annual revenue** from cycle time reduction
- **14 KPI tracking metrics** (vs V1's 6, V2's 1) for data-driven decisions
- **95% on-time implementation success** (vs industry 70%)
- **+20 NPS points** (from 45 to 65) through superior customer experience

**Technical Excellence**:
- **180+ comprehensive tests** (vs V1's 78, V2's 0)
- **9,500+ lines of production code** (V1: 3,217 + V2: 6,356 combined and enhanced)
- **<500ms average response time** for all automated tasks
- **99.9% uptime SLA** with self-healing capabilities

### Why This Matters

Healthcare operators in Brazil's ANS-regulated market need:
1. **Compliance certainty**: Beneficiary onboarding is legally mandated
2. **Speed to revenue**: Every day matters in competitive B2B sales
3. **Predictable delivery**: Implementation delays cost deals
4. **Scalable growth**: Manual processes can't support expansion goals

V3 solves all four problems simultaneously.

---

## 🏗️ SECTION 2: ARCHITECTURE CONSTRAINTS

### Non-Negotiable Requirements

**Functional Preservation**:
- ✅ **MUST preserve 100% of V1 functionality** (zero feature loss)
- ✅ **MUST integrate 100% of V2 improvements** (MEDDIC, parallel, error handling)
- ✅ **MUST add all 10 V3 innovations** (AI scoring, self-healing, etc.)

**Quality Standards**:
- ✅ **MUST achieve 180+ test coverage** (unit + integration + E2E)
- ✅ **MUST score 95/100 production readiness** (use provided rubric)
- ✅ **MUST demonstrate <2s response time** under 1,000 concurrent loads
- ✅ **MUST achieve 99.9% availability** (max 43 minutes downtime/month)

**Regulatory Compliance**:
- ✅ **MUST comply with ANS regulations** (beneficiary onboarding mandatory)
- ✅ **MUST maintain audit trail** for all process decisions
- ✅ **MUST support data privacy** (LGPD/GDPR equivalent)

**Technology Stack**:
- ✅ **MUST use Camunda 7.x** as BPMN engine (or provide migration plan to Camunda 8)
- ✅ **MUST support REST API integration** for CRM/ERP systems
- ✅ **MUST use relational database** (PostgreSQL/MySQL) for process state
- ✅ **MUST implement message queuing** (RabbitMQ/Kafka) for async tasks

**Scalability**:
- ✅ **MUST support horizontal scaling** (stateless service design)
- ✅ **MUST handle 10x current load** (1,000 concurrent process instances)
- ✅ **MUST enable blue-green deployment** (zero-downtime releases)

### Technology Choices

**Core Stack**:
```yaml
BPMN Engine: Camunda 7.19+ (Java-based)
Backend Language: Java 17+ OR Node.js 18+ (choose based on team expertise)
Database: PostgreSQL 14+ (primary), Redis 7+ (cache)
Message Broker: RabbitMQ 3.12+ OR Kafka 3.5+
API Gateway: Kong 3.4+ OR Spring Cloud Gateway
Monitoring: Prometheus + Grafana + ELK Stack
```

**Integration Technologies**:
```yaml
CRM Integration: REST API + Webhooks (Salesforce/HubSpot compatible)
ERP Integration: SOAP/REST adapters (SAP/Oracle compatible)
Email/SMS: SendGrid + Twilio (transactional communications)
Document Storage: AWS S3 OR Azure Blob Storage
Analytics: Google Analytics 4 + Custom dashboards
```

**Development Tools**:
```yaml
Version Control: Git (GitHub/GitLab)
CI/CD: GitHub Actions OR GitLab CI
Testing: JUnit 5/Mockito OR Jest/Supertest
Code Quality: SonarQube + ESLint/Checkstyle
Documentation: OpenAPI 3.1 + Swagger UI
```

---

## 📋 SECTION 3: 13-SUBPROCESS SPECIFICATIONS

### Subprocess 1: Main Orchestrator Process

**File**: `main-orchestrator-v3.bpmn`
**Purpose**: End-to-end lifecycle coordination from lead generation to contract expansion

**Input Contract**:
```javascript
{
  "leadId": "string (UUID)",
  "leadSource": "string (webform|referral|event|cold_outreach)",
  "companyName": "string",
  "contactName": "string",
  "contactEmail": "string",
  "contactPhone": "string",
  "industry": "string (healthcare|education|retail|...)",
  "companySize": "number (employee count)",
  "estimatedBudget": "number (USD)",
  "urgency": "string (high|medium|low)",
  "capturedAt": "timestamp (ISO 8601)"
}
```

**Output Contract**:
```javascript
{
  "processInstanceId": "string (UUID)",
  "currentStage": "string (qualification|engagement|...)",
  "overallStatus": "string (active|won|lost|on_hold)",
  "qualificationScore": "number (0-100)",
  "opportunityValue": "number (USD)",
  "probabilityToWin": "number (0-100%)",
  "estimatedCloseDate": "timestamp",
  "actualCloseDate": "timestamp (if closed)",
  "totalCycleTime": "number (days)",
  "kpiSnapshot": "object (14 metrics)"
}
```

**BPMN Structure**:
```xml
<process id="main-orchestrator-v3" name="AUSTA Sales Automation V3" isExecutable="true">

  <!-- Start: Lead Captured -->
  <startEvent id="start-lead-captured" name="Lead Captured">
    <extensionElements>
      <camunda:formData>
        <camunda:formField id="leadId" type="string" label="Lead ID" />
        <camunda:formField id="companyName" type="string" label="Company Name" />
        <!-- ... all input fields ... -->
      </camunda:formData>
    </extensionElements>
  </startEvent>

  <!-- Phase 1: Qualification -->
  <callActivity id="call-qualification" name="Qualify Lead" calledElement="qualification-subprocess-v3">
    <extensionElements>
      <camunda:in source="leadId" target="leadId" />
      <camunda:in source="estimatedBudget" target="opportunityValue" />
      <camunda:out source="qualificationStatus" target="qualificationStatus" />
      <camunda:out source="qualificationScore" target="qualificationScore" />
    </extensionElements>
  </callActivity>

  <!-- Gateway: Qualification Decision -->
  <exclusiveGateway id="gateway-qualification-decision" name="Qualified?" />

  <sequenceFlow id="flow-to-qualification" sourceRef="start-lead-captured" targetRef="call-qualification" />
  <sequenceFlow id="flow-qualification-check" sourceRef="call-qualification" targetRef="gateway-qualification-decision" />

  <!-- Path 1: Disqualified -->
  <sequenceFlow id="flow-disqualified" sourceRef="gateway-qualification-decision" targetRef="end-disqualified">
    <conditionExpression xsi:type="tFormalExpression">
      ${qualificationStatus == 'DISQUALIFIED'}
    </conditionExpression>
  </sequenceFlow>

  <endEvent id="end-disqualified" name="Lead Disqualified">
    <extensionElements>
      <camunda:executionListener event="start" delegateExpression="${notifyDisqualificationDelegate}" />
    </extensionElements>
  </endEvent>

  <!-- Path 2: Qualified → Continue Sales Process -->
  <sequenceFlow id="flow-qualified" sourceRef="gateway-qualification-decision" targetRef="call-engagement">
    <conditionExpression xsi:type="tFormalExpression">
      ${qualificationStatus == 'HIGH' || qualificationStatus == 'MEDIUM'}
    </conditionExpression>
  </sequenceFlow>

  <!-- Phase 2: Engagement -->
  <callActivity id="call-engagement" name="Engage Stakeholders" calledElement="engagement-subprocess-v3">
    <extensionElements>
      <camunda:in businessKey="#{execution.processBusinessKey}" />
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Phase 3: Value Demonstration -->
  <callActivity id="call-value-demo" name="Demonstrate Value" calledElement="value-demonstration-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Phase 4: Negotiation -->
  <callActivity id="call-negotiation" name="Negotiate Terms" calledElement="negotiation-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Phase 5: Approval -->
  <callActivity id="call-approval" name="Internal Approval" calledElement="approval-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Gateway: Approval Decision -->
  <exclusiveGateway id="gateway-approval-decision" name="Approved?" />

  <sequenceFlow id="flow-to-approval-check" sourceRef="call-approval" targetRef="gateway-approval-decision" />

  <!-- Path: Rejected -->
  <sequenceFlow id="flow-rejected" sourceRef="gateway-approval-decision" targetRef="end-lost">
    <conditionExpression xsi:type="tFormalExpression">
      ${approvalStatus == 'REJECTED'}
    </conditionExpression>
  </sequenceFlow>

  <endEvent id="end-lost" name="Deal Lost">
    <extensionElements>
      <camunda:executionListener event="start" delegateExpression="${recordLostDealDelegate}" />
    </extensionElements>
  </endEvent>

  <!-- Path: Approved → Closing -->
  <sequenceFlow id="flow-approved" sourceRef="gateway-approval-decision" targetRef="call-closing">
    <conditionExpression xsi:type="tFormalExpression">
      ${approvalStatus == 'APPROVED'}
    </conditionExpression>
  </sequenceFlow>

  <!-- Phase 6: Closing -->
  <callActivity id="call-closing" name="Close Deal" calledElement="closing-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Gateway: Deal Closed Successfully? -->
  <exclusiveGateway id="gateway-closing-success" name="Signed?" />

  <sequenceFlow id="flow-closing-check" sourceRef="call-closing" targetRef="gateway-closing-success" />

  <!-- Path: Not Signed -->
  <sequenceFlow id="flow-not-signed" sourceRef="gateway-closing-success" targetRef="end-lost">
    <conditionExpression xsi:type="tFormalExpression">
      ${contractStatus != 'SIGNED'}
    </conditionExpression>
  </sequenceFlow>

  <!-- Path: Signed → Post-Sales -->
  <sequenceFlow id="flow-signed" sourceRef="gateway-closing-success" targetRef="parallel-post-sales-start">
    <conditionExpression xsi:type="tFormalExpression">
      ${contractStatus == 'SIGNED'}
    </conditionExpression>
  </sequenceFlow>

  <!-- Parallel Gateway: Start Post-Sales Activities -->
  <parallelGateway id="parallel-post-sales-start" name="Start Post-Sales" />

  <!-- Parallel Branch 1: Beneficiary Onboarding (Critical Path) -->
  <sequenceFlow id="flow-to-onboarding" sourceRef="parallel-post-sales-start" targetRef="call-beneficiary-onboarding" />

  <callActivity id="call-beneficiary-onboarding" name="Onboard Beneficiaries (ANS Required)" calledElement="beneficiary-onboarding-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Parallel Branch 2: Digital Services Activation -->
  <sequenceFlow id="flow-to-digital-activation" sourceRef="parallel-post-sales-start" targetRef="call-digital-services" />

  <callActivity id="call-digital-services" name="Activate Digital Services" calledElement="digital-services-activation-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Parallel Branch 3: Implementation Planning -->
  <sequenceFlow id="flow-to-impl-planning" sourceRef="parallel-post-sales-start" targetRef="call-implementation-planning" />

  <callActivity id="call-implementation-planning" name="Plan Implementation" calledElement="implementation-planning-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Parallel Gateway: Converge After Parallel Tasks -->
  <parallelGateway id="parallel-post-sales-end" name="Converge Post-Sales" />

  <sequenceFlow id="flow-onboarding-done" sourceRef="call-beneficiary-onboarding" targetRef="parallel-post-sales-end" />
  <sequenceFlow id="flow-digital-done" sourceRef="call-digital-services" targetRef="parallel-post-sales-end" />
  <sequenceFlow id="flow-planning-done" sourceRef="call-implementation-planning" targetRef="parallel-post-sales-end" />

  <!-- Phase 7: Project Execution -->
  <sequenceFlow id="flow-to-execution" sourceRef="parallel-post-sales-end" targetRef="call-project-execution" />

  <callActivity id="call-project-execution" name="Execute Implementation" calledElement="project-execution-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Phase 8: Post-Launch Monitoring -->
  <callActivity id="call-post-launch" name="Monitor Post-Launch" calledElement="post-launch-monitoring-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Phase 9: Contract Expansion (Upsell/Cross-sell) -->
  <callActivity id="call-expansion" name="Expand Contract" calledElement="contract-expansion-subprocess-v3">
    <extensionElements>
      <camunda:in variables="all" />
      <camunda:out variables="all" />
    </extensionElements>
  </callActivity>

  <!-- Sequential Flow Through Final Phases -->
  <sequenceFlow id="flow-to-post-launch" sourceRef="call-project-execution" targetRef="call-post-launch" />
  <sequenceFlow id="flow-to-expansion" sourceRef="call-post-launch" targetRef="call-expansion" />

  <!-- End: Process Complete -->
  <sequenceFlow id="flow-to-end-success" sourceRef="call-expansion" targetRef="end-success" />

  <endEvent id="end-success" name="Lifecycle Complete">
    <extensionElements>
      <camunda:executionListener event="start" delegateExpression="${recordSuccessMetricsDelegate}" />
    </extensionElements>
  </endEvent>

  <!-- Boundary Events for Main Process -->

  <!-- Error Boundary: Any subprocess fails -->
  <boundaryEvent id="boundary-subprocess-error" name="Subprocess Error" attachedToRef="call-qualification">
    <errorEventDefinition errorRef="subprocess-error" />
  </boundaryEvent>

  <sequenceFlow id="flow-error-to-handler" sourceRef="boundary-subprocess-error" targetRef="task-error-handler" />

  <serviceTask id="task-error-handler" name="Handle Error" camunda:delegateExpression="${errorHandlerDelegate}" />

  <!-- Timer Boundary: Process exceeds 120 days -->
  <boundaryEvent id="boundary-cycle-time-exceeded" name="120 Days Exceeded" attachedToRef="main-orchestrator-v3" cancelActivity="false">
    <timerEventDefinition>
      <timeDuration>P120D</timeDuration>
    </timerEventDefinition>
  </boundaryEvent>

  <sequenceFlow id="flow-timer-to-escalation" sourceRef="boundary-cycle-time-exceeded" targetRef="task-escalate-long-cycle" />

  <userTask id="task-escalate-long-cycle" name="Escalate Long Sales Cycle" camunda:assignee="${salesDirector}">
    <documentation>This deal has exceeded 120 days. Review blockers and decide next steps.</documentation>
  </userTask>

  <!-- Compensation Handler: Rollback all changes on cancel -->
  <boundaryEvent id="boundary-compensation" name="Compensation" attachedToRef="main-orchestrator-v3">
    <compensateEventDefinition />
  </boundaryEvent>

</process>
```

**Success Criteria**:
- ✅ Process deploys without BPMN validation errors
- ✅ All 13 subprocesses are callable
- ✅ Happy path (lead → expansion) completes in <75 days (simulated)
- ✅ Error path (disqualified) terminates cleanly with audit log
- ✅ Parallel execution reduces post-sales phase time by 30%
- ✅ All process variables properly scoped (no leakage between subprocesses)

**Integration Points**:
- **CRM**: Update deal stage at each phase transition (Salesforce/HubSpot API)
- **Analytics**: Send process metrics to data warehouse (REST POST)
- **Notifications**: Email/SMS alerts for key milestones (SendGrid/Twilio)
- **Audit Log**: Write all decisions to compliance database (PostgreSQL)

**Error Scenarios**:
1. **Subprocess failure**: Catch error, log to monitoring, retry 3x with backoff
2. **Timeout (120 days)**: Trigger non-interrupting escalation to sales director
3. **Process cancellation**: Execute compensation handlers to rollback CRM/ERP changes
4. **Invalid state transition**: Rollback to last valid state, alert admin

**Test Scenarios** (20 required):
- T1: Happy path (qualified → won → expansion) completes successfully
- T2: Disqualified lead terminates at qualification gateway
- T3: Rejected approval terminates at approval gateway
- T4: Unsigned contract terminates at closing gateway
- T5: Parallel post-sales tasks execute concurrently (30% time reduction)
- T6: Beneficiary onboarding failure blocks process execution (ANS compliance)
- T7: Error boundary catches subprocess exception and triggers handler
- T8: 120-day timer triggers escalation without canceling process
- T9: Compensation handler rolls back CRM updates on cancel
- T10: Process variables flow correctly through all 13 subprocesses
- T11: Multiple concurrent instances (100) don't interfere with each other
- T12: Process restart from failure point (idempotency)
- T13: Historical data preserved in audit trail
- T14: KPI snapshot calculated correctly at end
- T15: Performance: <2s response time for process start
- T16: Invalid input data handled gracefully (validation errors)
- T17: External system timeout (CRM unreachable) triggers retry logic
- T18: Process migration (V1→V3) preserves in-flight instances
- T19: Blue-green deployment doesn't drop active process instances
- T20: Monitoring alerts triggered for all error conditions

**Performance SLA**:
- **Total cycle time**: 75 days (business time)
- **System processing time**: <500ms per phase transition
- **Subprocess invocation overhead**: <50ms per call
- **Database queries**: <100ms per transaction
- **External API calls**: <2s timeout with retry

---

### Subprocess 2: Qualification Subprocess V3

**File**: `qualification-subprocess-v3.bpmn`
**Purpose**: Dual-methodology lead qualification (MEDDIC for enterprise, Fit Score for SMB)

**Business Problem**:
- Enterprise deals ($500K+) require rigorous qualification methodology (MEDDIC)
- SMB deals (<$500K) need faster qualification (traditional fit scoring)
- Sales reps waste time on unqualified leads without structured evaluation
- Inconsistent qualification criteria across sales team

**Input Contract**:
```javascript
{
  "leadId": "string (UUID)",
  "lead": {
    "companyName": "string",
    "contactName": "string",
    "contactEmail": "string",
    "contactPhone": "string",
    "industry": "string (healthcare|education|retail|manufacturing|...)",
    "companySize": "number (employee count)",
    "annualRevenue": "number (USD)",
    "currentSolution": "string (competitor name or 'none')",
    "painPoints": "array<string> (list of challenges)",
    "budget": "number (USD allocated)",
    "timeline": "string (immediate|3months|6months|12months)",
    "decisionMaker": "string (name of budget holder)",
    "location": "string (city, state, country)"
  },
  "opportunityValue": "number (estimated deal size in USD)"
}
```

**Output Contract**:
```javascript
{
  "qualificationMethod": "string ('MEDDIC' | 'FIT_SCORE')",
  "qualificationStatus": "string ('HIGH' | 'MEDIUM' | 'LOW' | 'DISQUALIFIED')",
  "qualificationScore": "number (0-100 normalized score)",

  // MEDDIC-specific outputs (only if enterprise)
  "scoreMEDDIC": {
    "metrics": "number (0-10, quantified business value)",
    "economicBuyer": "number (0-10, budget holder identified)",
    "decisionCriteria": "number (0-10, evaluation process understood)",
    "decisionProcess": "number (0-10, buying stages mapped)",
    "identifyPain": "number (0-10, problem severity confirmed)",
    "champion": "number (0-10, internal advocate found)",
    "overallScore": "number (0-10, average of 6 components)"
  },

  // Fit Score-specific outputs (only if SMB)
  "fitScore": {
    "industryMatch": "number (0-25, industry alignment points)",
    "sizeMatch": "number (0-25, company size fit points)",
    "budgetMatch": "number (0-25, budget alignment points)",
    "geoMatch": "number (0-25, geographic coverage points)",
    "totalScore": "number (0-100, sum of 4 components)"
  },

  // AI-generated recommendations
  "nextActions": [
    {
      "action": "string (schedule_demo | send_proposal | escalate_to_manager | ...)",
      "priority": "string (high|medium|low)",
      "dueDate": "timestamp (suggested deadline)",
      "rationale": "string (why this action is recommended)"
    }
  ],

  // Historical tracking
  "scoreHistorico": [
    {
      "scoreDate": "timestamp",
      "scoreValue": "number",
      "scoreMethod": "string (MEDDIC|FIT_SCORE)"
    }
  ],

  "qualifiedAt": "timestamp (ISO 8601)",
  "qualifiedBy": "string (sales rep user ID)"
}
```

**BPMN Structure**:
```xml
<process id="qualification-subprocess-v3" name="Lead Qualification V3" isExecutable="true">

  <!-- Start -->
  <startEvent id="start-qualification" name="Start Qualification" />

  <!-- Task 1: Enrich Lead Data (AI) -->
  <serviceTask id="task-enrich-lead" name="Enrich Lead with AI Data" camunda:delegateExpression="${aiLeadEnrichmentDelegate}">
    <documentation>
      Use AI to enrich lead data:
      - Company firmographics (Clearbit/ZoomInfo API)
      - Technology stack (BuiltWith/Datanyze)
      - Recent news/funding (Crunchbase)
      - Social media signals (LinkedIn)
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-start-to-enrich" sourceRef="start-qualification" targetRef="task-enrich-lead" />

  <!-- Gateway: Routing by Deal Size -->
  <exclusiveGateway id="gateway-deal-size-routing" name="Deal Size?" />

  <sequenceFlow id="flow-enrich-to-routing" sourceRef="task-enrich-lead" targetRef="gateway-deal-size-routing" />

  <!-- Path 1: Enterprise (MEDDIC) -->
  <sequenceFlow id="flow-to-meddic" sourceRef="gateway-deal-size-routing" targetRef="task-meddic-assessment">
    <conditionExpression xsi:type="tFormalExpression">
      ${opportunityValue >= 500000}
    </conditionExpression>
  </sequenceFlow>

  <userTask id="task-meddic-assessment" name="Complete MEDDIC Assessment"
            camunda:formKey="embedded:app:forms/meddic-form.html"
            camunda:assignee="${leadOwner}">
    <documentation>
      Complete MEDDIC qualification by scoring each component 0-10:

      **Metrics** (0-10):
      - What quantifiable business value will solution deliver?
      - How will ROI be measured?
      - What is the baseline (current state metrics)?

      **Economic Buyer** (0-10):
      - Who has budget authority?
      - Have you engaged with them directly?
      - Do they understand the investment required?

      **Decision Criteria** (0-10):
      - What criteria will be used to evaluate solutions?
      - How does our solution stack up vs competitors?
      - Are there technical, financial, or strategic criteria?

      **Decision Process** (0-10):
      - What steps must occur before contract signing?
      - Who are all decision influencers?
      - What is the timeline for each stage?

      **Identify Pain** (0-10):
      - What is the severity of the problem (hair on fire)?
      - What happens if they don't solve it?
      - Is there urgency and consequence?

      **Champion** (0-10):
      - Do you have an internal advocate?
      - Are they influential in the organization?
      - Will they actively sell on your behalf?
    </documentation>

    <extensionElements>
      <camunda:formData>
        <camunda:formField id="metrics" type="long" label="Metrics Score (0-10)" />
        <camunda:formField id="metricsNotes" type="string" label="Metrics Notes" />
        <camunda:formField id="economicBuyer" type="long" label="Economic Buyer Score (0-10)" />
        <camunda:formField id="economicBuyerName" type="string" label="Economic Buyer Name" />
        <camunda:formField id="decisionCriteria" type="long" label="Decision Criteria Score (0-10)" />
        <camunda:formField id="decisionCriteriaNotes" type="string" label="Decision Criteria Details" />
        <camunda:formField id="decisionProcess" type="long" label="Decision Process Score (0-10)" />
        <camunda:formField id="decisionProcessSteps" type="string" label="Process Steps" />
        <camunda:formField id="identifyPain" type="long" label="Pain Severity Score (0-10)" />
        <camunda:formField id="painDescription" type="string" label="Pain Description" />
        <camunda:formField id="champion" type="long" label="Champion Score (0-10)" />
        <camunda:formField id="championName" type="string" label="Champion Name" />
      </camunda:formData>

      <camunda:taskListener event="create" delegateExpression="${notifyMEDDICTaskDelegate}" />
      <camunda:taskListener event="complete" delegateExpression="${calculateMEDDICScoreDelegate}" />
    </extensionElements>
  </userTask>

  <!-- Task: Calculate MEDDIC Score -->
  <serviceTask id="task-calculate-meddic" name="Calculate MEDDIC Score" camunda:delegateExpression="${meddic ScoreCalculatorDelegate}">
    <documentation>
      Calculate MEDDIC score:
      overallScore = (metrics + economicBuyer + decisionCriteria + decisionProcess + identifyPain + champion) / 6
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-meddic-assessment-to-calc" sourceRef="task-meddic-assessment" targetRef="task-calculate-meddic" />

  <!-- Gateway: MEDDIC Score Routing -->
  <exclusiveGateway id="gateway-meddic-score" name="MEDDIC Score?" />

  <sequenceFlow id="flow-meddic-calc-to-gateway" sourceRef="task-calculate-meddic" targetRef="gateway-meddic-score" />

  <!-- MEDDIC Score Paths -->
  <sequenceFlow id="flow-meddic-high" sourceRef="gateway-meddic-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${scoreMEDDIC.overallScore >= 8.0}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-meddic-medium" sourceRef="gateway-meddic-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${scoreMEDDIC.overallScore >= 6.0 &amp;&amp; scoreMEDDIC.overallScore < 8.0}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-meddic-low" sourceRef="gateway-meddic-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${scoreMEDDIC.overallScore >= 4.0 &amp;&amp; scoreMEDDIC.overallScore < 6.0}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-meddic-disqualified" sourceRef="gateway-meddic-score" targetRef="end-disqualified">
    <conditionExpression xsi:type="tFormalExpression">
      ${scoreMEDDIC.overallScore < 4.0}
    </conditionExpression>
  </sequenceFlow>

  <!-- Path 2: SMB (Fit Score) -->
  <sequenceFlow id="flow-to-fit-score" sourceRef="gateway-deal-size-routing" targetRef="task-calculate-fit-score">
    <conditionExpression xsi:type="tFormalExpression">
      ${opportunityValue < 500000}
    </conditionExpression>
  </sequenceFlow>

  <serviceTask id="task-calculate-fit-score" name="Calculate Fit Score" camunda:delegateExpression="${fitScoreCalculatorDelegate}">
    <documentation>
      Calculate fit score using V1 algorithm:

      Industry Match (0-25 points):
      - Healthcare: 25 (ideal)
      - Education: 20
      - Retail: 15
      - Manufacturing: 10
      - Other: 5

      Company Size Match (0-25 points):
      - 500-2000 employees: 25 (sweet spot)
      - 200-499: 20
      - 2001-5000: 15
      - <200 or >5000: 10

      Budget Alignment (0-25 points):
      - Budget >= expectedPrice: 25
      - Budget >= 75% expectedPrice: 20
      - Budget >= 50% expectedPrice: 10
      - Budget < 50% expectedPrice: 0

      Geographic Coverage (0-25 points):
      - Primary markets (São Paulo, Rio): 25
      - Secondary markets (Brasília, Minas): 20
      - Tertiary markets: 10
      - Outside coverage area: 0

      Total Score = Sum of all components (0-100)
    </documentation>
  </serviceTask>

  <!-- Gateway: Fit Score Routing -->
  <exclusiveGateway id="gateway-fit-score" name="Fit Score?" />

  <sequenceFlow id="flow-fit-calc-to-gateway" sourceRef="task-calculate-fit-score" targetRef="gateway-fit-score" />

  <!-- Fit Score Paths -->
  <sequenceFlow id="flow-fit-high" sourceRef="gateway-fit-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${fitScore.totalScore >= 70}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-fit-medium" sourceRef="gateway-fit-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${fitScore.totalScore >= 50 &amp;&amp; fitScore.totalScore < 70}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-fit-low" sourceRef="gateway-fit-score" targetRef="task-ai-recommendations">
    <conditionExpression xsi:type="tFormalExpression">
      ${fitScore.totalScore >= 30 &amp;&amp; fitScore.totalScore < 50}
    </conditionExpression>
  </sequenceFlow>

  <sequenceFlow id="flow-fit-disqualified" sourceRef="gateway-fit-score" targetRef="end-disqualified">
    <conditionExpression xsi:type="tFormalExpression">
      ${fitScore.totalScore < 30}
    </conditionExpression>
  </sequenceFlow>

  <!-- Converge: AI Recommendations (Both Paths) -->
  <serviceTask id="task-ai-recommendations" name="Generate AI Next Actions" camunda:delegateExpression="${aiRecommendationsDelegate}">
    <documentation>
      Use AI (GPT-4/Claude) to suggest next actions based on:
      - Qualification score and method
      - Lead enrichment data
      - Historical CRM data for similar leads
      - Current sales rep workload

      Generate 3-5 prioritized actions with:
      - Action type (schedule_demo, send_case_study, escalate, etc.)
      - Priority level (high/medium/low)
      - Suggested due date
      - Rationale (why this action makes sense)
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-recommendations-to-update" sourceRef="task-ai-recommendations" targetRef="task-update-crm" />

  <!-- Task: Update CRM -->
  <serviceTask id="task-update-crm" name="Update CRM with Qualification" camunda:delegateExpression="${crmUpdateDelegate}">
    <documentation>
      Update CRM (Salesforce/HubSpot) with:
      - Qualification status (HIGH/MEDIUM/LOW)
      - Qualification score (normalized 0-100)
      - Qualification method (MEDDIC/FIT_SCORE)
      - Next actions (AI recommendations)
      - Timestamp
    </documentation>
  </serviceTask>

  <!-- Task: Send Notification -->
  <serviceTask id="task-notify-rep" name="Notify Sales Rep" camunda:delegateExpression="${notificationDelegate}">
    <documentation>
      Send notification to sales rep via:
      - Email (summary + link to CRM)
      - SMS (if high-priority lead)
      - Slack (team channel)
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-update-to-notify" sourceRef="task-update-crm" targetRef="task-notify-rep" />

  <!-- End: Qualified -->
  <sequenceFlow id="flow-notify-to-end" sourceRef="task-notify-rep" targetRef="end-qualified" />

  <endEvent id="end-qualified" name="Lead Qualified">
    <extensionElements>
      <camunda:executionListener event="start" delegateExpression="${recordQualificationMetricsDelegate}" />
    </extensionElements>
  </endEvent>

  <!-- End: Disqualified -->
  <endEvent id="end-disqualified" name="Lead Disqualified">
    <extensionElements>
      <camunda:executionListener event="start" delegateExpression="${recordDisqualificationDelegate}" />
    </extensionElements>
  </endEvent>

  <!-- Boundary Events -->

  <!-- Timer: 48h to complete MEDDIC assessment -->
  <boundaryEvent id="boundary-meddic-timeout" name="48h Timeout" attachedToRef="task-meddic-assessment" cancelActivity="false">
    <timerEventDefinition>
      <timeDuration>PT48H</timeDuration>
    </timerEventDefinition>
  </boundaryEvent>

  <sequenceFlow id="flow-timeout-to-escalation" sourceRef="boundary-meddic-timeout" targetRef="task-escalate-meddic" />

  <userTask id="task-escalate-meddic" name="Escalate MEDDIC Delay" camunda:assignee="${salesManager}">
    <documentation>Sales rep has not completed MEDDIC assessment within 48 hours. Review and take action.</documentation>
  </userTask>

  <!-- Error: Scoring calculation failure -->
  <boundaryEvent id="boundary-scoring-error" name="Scoring Error" attachedToRef="task-calculate-meddic">
    <errorEventDefinition errorRef="scoring-error" />
  </boundaryEvent>

  <sequenceFlow id="flow-error-to-default" sourceRef="boundary-scoring-error" targetRef="task-default-qualification" />

  <serviceTask id="task-default-qualification" name="Apply Default Qualification" camunda:delegateExpression="${defaultQualificationDelegate}">
    <documentation>Scoring failed. Apply default qualification based on lead enrichment data alone.</documentation>
  </serviceTask>

  <sequenceFlow id="flow-default-to-end" sourceRef="task-default-qualification" targetRef="end-qualified" />

  <!-- Compensation: Rollback CRM update -->
  <boundaryEvent id="boundary-compensation" name="Compensation" attachedToRef="task-update-crm">
    <compensateEventDefinition />
  </boundaryEvent>

</process>
```

**Success Criteria**:
- ✅ Enterprise deals ($500K+) route to MEDDIC assessment
- ✅ SMB deals (<$500K) route to fit score calculation
- ✅ MEDDIC score 8.0+ → HIGH qualification
- ✅ MEDDIC score <4.0 → DISQUALIFIED
- ✅ Fit score 70+ → HIGH qualification
- ✅ Fit score <30 → DISQUALIFIED
- ✅ AI generates 3-5 relevant next actions for qualified leads
- ✅ CRM updated successfully with qualification data
- ✅ Sales rep receives notification within 5 minutes
- ✅ 48h timer triggers escalation to manager
- ✅ Scoring error falls back to default qualification
- ✅ Process variables properly scoped (no leakage)
- ✅ Historical scores tracked in scoreHistorico array

**Integration Points**:
- **AI Enrichment API**: Clearbit/ZoomInfo for company data (REST)
- **CRM**: Salesforce/HubSpot update lead status (REST)
- **AI Recommendations**: OpenAI GPT-4 or Anthropic Claude (REST)
- **Notifications**: SendGrid (email) + Twilio (SMS) + Slack (webhook)
- **Analytics**: Data warehouse for qualification metrics (JDBC)

**Error Scenarios**:
1. **AI enrichment fails**: Proceed with manual lead data
2. **MEDDIC assessment timeout**: Escalate to sales manager, don't block process
3. **Scoring calculation error**: Use default qualification based on enrichment data
4. **CRM update fails**: Retry 3x, then queue for manual sync
5. **Notification delivery fails**: Log error, don't block process completion

**Test Scenarios** (15 required):
- T1: Enterprise deal ($1M) routes to MEDDIC path
- T2: SMB deal ($100K) routes to fit score path
- T3: MEDDIC score 9.5 → HIGH qualification status
- T4: MEDDIC score 3.2 → DISQUALIFIED status
- T5: Fit score 85 → HIGH qualification status
- T6: Fit score 25 → DISQUALIFIED status
- T7: AI enrichment adds firmographic data successfully
- T8: AI recommendations generate 3-5 actionable next steps
- T9: CRM update successful, verified in external system
- T10: Sales rep receives email notification within 5 minutes
- T11: 48h timer triggers escalation to manager
- T12: Scoring service failure triggers error handler and default qualification
- T13: Compensation handler rolls back CRM update on process cancel
- T14: Parallel execution: 100 concurrent qualification subprocesses
- T15: Historical scores appended to scoreHistorico array

**Performance SLA**:
- **Human time**: 2 hours for MEDDIC assessment (user task)
- **System processing time**: <500ms for all automated tasks
- **AI enrichment**: <3s per lead
- **AI recommendations**: <2s generation time
- **CRM update**: <1s per record
- **Overall subprocess duration**: <3 hours wall-clock time

---

### Subprocess 3: Engagement Subprocess V3

**File**: `engagement-subprocess-v3.bpmn`
**Purpose**: Multi-stakeholder engagement and relationship building

**Business Problem**:
- B2B deals involve 6-10 stakeholders (economic buyer, users, IT, legal, procurement)
- Sales reps lose deals by failing to engage all decision influencers
- Lack of structured engagement plan leads to missed touchpoints
- No visibility into relationship strength across stakeholder map

**Input Contract**:
```javascript
{
  "leadId": "string (UUID)",
  "qualificationScore": "number (0-100)",
  "qualificationMethod": "string (MEDDIC|FIT_SCORE)",
  "companyName": "string",
  "opportunityValue": "number (USD)",
  "stakeholders": [
    {
      "name": "string",
      "role": "string (economic_buyer|champion|influencer|blocker|user)",
      "department": "string",
      "email": "string",
      "phone": "string",
      "engagementLevel": "string (none|low|medium|high)",
      "sentiment": "string (positive|neutral|negative|unknown)"
    }
  ]
}
```

**Output Contract**:
```javascript
{
  "engagementStatus": "string (progressing|stalled|at_risk)",
  "stakeholderMap": [
    {
      "stakeholderId": "string (UUID)",
      "name": "string",
      "role": "string",
      "engagementLevel": "string (none|low|medium|high)",
      "sentiment": "string (positive|neutral|negative)",
      "lastContactDate": "timestamp",
      "touchpointCount": "number",
      "relationshipScore": "number (0-100)"
    }
  ],
  "overallEngagementScore": "number (0-100, average across stakeholders)",
  "nextMilestone": "string (demo_scheduled|proposal_sent|contract_negotiation|...)",
  "engagementPlan": [
    {
      "stakeholder": "string",
      "action": "string (call|email|meeting|demo|...)",
      "dueDate": "timestamp",
      "status": "string (planned|completed|overdue)"
    }
  ],
  "engagedAt": "timestamp",
  "completedAt": "timestamp"
}
```

**BPMN Structure**:
```xml
<process id="engagement-subprocess-v3" name="Stakeholder Engagement V3" isExecutable="true">

  <startEvent id="start-engagement" name="Start Engagement" />

  <!-- Task 1: Build Stakeholder Map -->
  <userTask id="task-map-stakeholders" name="Map All Stakeholders"
            camunda:formKey="embedded:app:forms/stakeholder-map-form.html"
            camunda:assignee="${leadOwner}">
    <documentation>
      Identify all decision makers and influencers:
      - Economic Buyer (budget authority)
      - Champion (internal advocate)
      - Influencers (department heads, users)
      - Blockers (skeptics, competitors' allies)
      - Users (end users who will use the solution)

      For each stakeholder, capture:
      - Name, role, department
      - Contact information
      - Current engagement level (none/low/medium/high)
      - Sentiment (positive/neutral/negative)
    </documentation>
  </userTask>

  <sequenceFlow id="flow-start-to-map" sourceRef="start-engagement" targetRef="task-map-stakeholders" />

  <!-- Task 2: Generate Engagement Plan (AI) -->
  <serviceTask id="task-generate-plan" name="Generate AI Engagement Plan" camunda:delegateExpression="${aiEngagementPlanDelegate}">
    <documentation>
      Use AI to create personalized engagement plan for each stakeholder:
      - Recommend touchpoint sequence (call → email → demo → proposal)
      - Suggest content personalization based on role (technical for IT, ROI for finance)
      - Propose timeline based on qualification score and deal urgency
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-map-to-plan" sourceRef="task-map-stakeholders" targetRef="task-generate-plan" />

  <!-- Parallel Gateway: Engage Multiple Stakeholders Concurrently -->
  <parallelGateway id="parallel-stakeholder-engagement-start" name="Start Parallel Engagement" />

  <sequenceFlow id="flow-plan-to-parallel" sourceRef="task-generate-plan" targetRef="parallel-stakeholder-engagement-start" />

  <!-- Branch 1: Engage Economic Buyer -->
  <callActivity id="call-engage-economic-buyer" name="Engage Economic Buyer" calledElement="single-stakeholder-engagement">
    <extensionElements>
      <camunda:in source="economicBuyer" target="stakeholder" />
      <camunda:out source="engagementResult" target="economicBuyerResult" />
    </extensionElements>
  </callActivity>

  <sequenceFlow id="flow-to-economic-buyer" sourceRef="parallel-stakeholder-engagement-start" targetRef="call-engage-economic-buyer" />

  <!-- Branch 2: Engage Champion -->
  <callActivity id="call-engage-champion" name="Engage Champion" calledElement="single-stakeholder-engagement">
    <extensionElements>
      <camunda:in source="champion" target="stakeholder" />
      <camunda:out source="engagementResult" target="championResult" />
    </extensionElements>
  </callActivity>

  <sequenceFlow id="flow-to-champion" sourceRef="parallel-stakeholder-engagement-start" targetRef="call-engage-champion" />

  <!-- Branch 3: Engage Influencers (Multi-Instance) -->
  <subProcess id="subprocess-engage-influencers" name="Engage All Influencers">
    <multiInstanceLoopCharacteristics isSequential="false" camunda:collection="${influencers}" camunda:elementVariable="influencer" />

    <startEvent id="start-influencer-engagement" />

    <callActivity id="call-engage-influencer" name="Engage Influencer" calledElement="single-stakeholder-engagement">
      <extensionElements>
        <camunda:in source="influencer" target="stakeholder" />
        <camunda:out source="engagementResult" target="influencerResult" />
      </extensionElements>
    </callActivity>

    <endEvent id="end-influencer-engagement" />

    <sequenceFlow sourceRef="start-influencer-engagement" targetRef="call-engage-influencer" />
    <sequenceFlow sourceRef="call-engage-influencer" targetRef="end-influencer-engagement" />
  </subProcess>

  <sequenceFlow id="flow-to-influencers" sourceRef="parallel-stakeholder-engagement-start" targetRef="subprocess-engage-influencers" />

  <!-- Parallel Gateway: Converge After Stakeholder Engagement -->
  <parallelGateway id="parallel-stakeholder-engagement-end" name="Converge Engagement" />

  <sequenceFlow id="flow-economic-buyer-done" sourceRef="call-engage-economic-buyer" targetRef="parallel-stakeholder-engagement-end" />
  <sequenceFlow id="flow-champion-done" sourceRef="call-engage-champion" targetRef="parallel-stakeholder-engagement-end" />
  <sequenceFlow id="flow-influencers-done" sourceRef="subprocess-engage-influencers" targetRef="parallel-stakeholder-engagement-end" />

  <!-- Task 3: Calculate Overall Engagement Score -->
  <serviceTask id="task-calculate-engagement-score" name="Calculate Engagement Score" camunda:delegateExpression="${engagementScoreCalculatorDelegate}">
    <documentation>
      Calculate overall engagement score:
      - Economic Buyer: 40% weight
      - Champion: 30% weight
      - Influencers: 30% weight (average across all)

      Score = (economicBuyerScore * 0.4) + (championScore * 0.3) + (avgInfluencerScore * 0.3)
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-converge-to-score" sourceRef="parallel-stakeholder-engagement-end" targetRef="task-calculate-engagement-score" />

  <!-- Gateway: Engagement Status Decision -->
  <exclusiveGateway id="gateway-engagement-status" name="Engagement Sufficient?" />

  <sequenceFlow id="flow-score-to-gateway" sourceRef="task-calculate-engagement-score" targetRef="gateway-engagement-status" />

  <!-- Path 1: Engagement Sufficient (Score >= 70) -->
  <sequenceFlow id="flow-engagement-sufficient" sourceRef="gateway-engagement-status" targetRef="task-schedule-next-milestone">
    <conditionExpression xsi:type="tFormalExpression">
      ${overallEngagementScore >= 70}
    </conditionExpression>
  </sequenceFlow>

  <serviceTask id="task-schedule-next-milestone" name="Schedule Next Milestone" camunda:delegateExpression="${milestoneSchedulerDelegate}">
    <documentation>
      Schedule next milestone based on engagement success:
      - Demo (if product demonstration needed)
      - Proposal presentation (if ready for formal proposal)
      - POC/pilot (if technical validation required)
    </documentation>
  </serviceTask>

  <sequenceFlow id="flow-milestone-to-end" sourceRef="task-schedule-next-milestone" targetRef="end-engagement-success" />

  <endEvent id="end-engagement-success" name="Engagement Complete" />

  <!-- Path 2: Engagement Insufficient (Score < 70) -->
  <sequenceFlow id="flow-engagement-insufficient" sourceRef="gateway-engagement-status" targetRef="task-create-remediation-plan">
    <conditionExpression xsi:type="tFormalExpression">
      ${overallEngagementScore < 70}
    </conditionExpression>
  </sequenceFlow>

  <userTask id="task-create-remediation-plan" name="Create Remediation Plan"
            camunda:assignee="${salesManager}">
    <documentation>
      Engagement score is below threshold. Create plan to improve stakeholder relationships:
      - Identify gaps (which stakeholders are not engaged?)
      - Determine root causes (access issues, wrong messaging, competitor influence?)
      - Define corrective actions (executive sponsor engagement, partner introduction, etc.)
    </documentation>
  </userTask>

  <sequenceFlow id="flow-remediation-to-retry" sourceRef="task-create-remediation-plan" targetRef="parallel-stakeholder-engagement-start" />

  <!-- Boundary Events -->

  <!-- Timer: 14 days to complete stakeholder engagement -->
  <boundaryEvent id="boundary-engagement-timeout" name="14 Days Exceeded" attachedToRef="engagement-subprocess-v3" cancelActivity="false">
    <timerEventDefinition>
      <timeDuration>P14D</timeDuration>
    </timerEventDefinition>
  </boundaryEvent>

  <sequenceFlow id="flow-timeout-to-alert" sourceRef="boundary-engagement-timeout" targetRef="task-alert-stalled-engagement" />

  <serviceTask id="task-alert-stalled-engagement" name="Alert Stalled Engagement" camunda:delegateExpression="${stalledEngagementAlertDelegate}">
    <documentation>Engagement has exceeded 14 days without completion. Alert sales director.</documentation>
  </serviceTask>

</process>

<!-- Reusable Sub-Process: Single Stakeholder Engagement -->
<process id="single-stakeholder-engagement" name="Engage Single Stakeholder" isExecutable="true">

  <startEvent id="start-single-engagement" />

  <!-- Task 1: Send Initial Outreach -->
  <serviceTask id="task-send-outreach" name="Send Personalized Outreach" camunda:delegateExpression="${outreachDelegate}">
    <documentation>
      Send personalized email based on stakeholder role:
      - Economic Buyer: ROI-focused messaging
      - Champion: Partnership and success stories
      - Influencer: Role-specific pain points and solutions
    </documentation>
  </serviceTask>

  <!-- Task 2: Schedule Meeting -->
  <userTask id="task-schedule-meeting" name="Schedule Meeting" camunda:assignee="${leadOwner}">
    <documentation>Attempt to schedule 30-minute discovery call or demo.</documentation>
  </userTask>

  <!-- Task 3: Conduct Meeting -->
  <userTask id="task-conduct-meeting" name="Conduct Meeting" camunda:assignee="${leadOwner}">
    <documentation>
      Conduct meeting and capture:
      - Key concerns and priorities
      - Decision influence level
      - Sentiment (positive/neutral/negative)
      - Next steps committed
    </documentation>
  </userTask>

  <!-- Task 4: Follow-Up -->
  <serviceTask id="task-follow-up" name="Send Follow-Up" camunda:delegateExpression="${followUpDelegate}">
    <documentation>Send thank-you email with meeting summary and next steps within 24 hours.</documentation>
  </serviceTask>

  <!-- Task 5: Update Stakeholder Status -->
  <serviceTask id="task-update-stakeholder" name="Update Stakeholder Record" camunda:delegateExpression="${stakeholderUpdateDelegate}">
    <documentation>
      Update stakeholder record in CRM:
      - Engagement level (low→medium or medium→high)
      - Sentiment (based on meeting feedback)
      - Relationship score (0-100)
    </documentation>
  </serviceTask>

  <endEvent id="end-single-engagement" name="Stakeholder Engaged" />

  <sequenceFlow sourceRef="start-single-engagement" targetRef="task-send-outreach" />
  <sequenceFlow sourceRef="task-send-outreach" targetRef="task-schedule-meeting" />
  <sequenceFlow sourceRef="task-schedule-meeting" targetRef="task-conduct-meeting" />
  <sequenceFlow sourceRef="task-conduct-meeting" targetRef="task-follow-up" />
  <sequenceFlow sourceRef="task-follow-up" targetRef="task-update-stakeholder" />
  <sequenceFlow sourceRef="task-update-stakeholder" targetRef="end-single-engagement" />

</process>
```

**Success Criteria**:
- ✅ All stakeholders mapped (economic buyer, champion, influencers identified)
- ✅ AI generates personalized engagement plan for each stakeholder
- ✅ Parallel engagement reduces phase duration by 40% vs sequential
- ✅ Overall engagement score calculated correctly (weighted average)
- ✅ Score >= 70 → proceed to next phase
- ✅ Score < 70 → remediation plan created
- ✅ 14-day timer triggers alert for stalled engagement
- ✅ CRM updated with stakeholder engagement data

**Integration Points**:
- **CRM**: Retrieve stakeholder list, update engagement status
- **Email/Calendar**: SendGrid for outreach, Google Calendar API for meeting scheduling
- **AI**: GPT-4/Claude for personalized messaging generation
- **Analytics**: Track stakeholder engagement metrics

**Error Scenarios**:
1. **Stakeholder unavailable**: Retry outreach 3x over 2 weeks, then mark as "unable to engage"
2. **Meeting cancellation**: Reschedule automatically, alert sales rep
3. **Negative sentiment**: Escalate to sales manager for intervention
4. **Blocker identified**: Create blocker mitigation strategy task

**Test Scenarios** (12 required):
- T1: Stakeholder map created with 5+ stakeholders
- T2: AI generates personalized outreach for economic buyer
- T3: Parallel engagement executes 3 branches concurrently
- T4: Single stakeholder engagement completes successfully
- T5: Overall engagement score calculates correctly (weighted)
- T6: Score 85 → proceed to next milestone
- T7: Score 55 → remediation plan required
- T8: 14-day timer triggers stalled engagement alert
- T9: CRM updated with engagement results
- T10: Multi-instance loop engages all influencers
- T11: Blocker sentiment triggers escalation
- T12: Meeting rescheduling works correctly

**Performance SLA**:
- **Total duration**: 14 days (business time)
- **System processing**: <1s per automated task
- **Parallel engagement**: 40% time reduction vs sequential
- **AI plan generation**: <5s

---

### Subprocess 4-13: Specifications Continue...

**NOTE**: Due to length constraints, I'm providing the complete structure for remaining subprocesses in condensed format. Full BPMN XML and detailed specifications follow same pattern as Subprocesses 1-3.

---

### Subprocess 4: Value Demonstration V3

**Purpose**: Product demos, POCs, ROI calculations
**Key Features**:
- Multi-format demos (live, recorded, sandbox)
- POC/pilot program management
- ROI calculator with industry benchmarks
- Competitive differentiation showcase

**Input**: Stakeholder map, engagement score
**Output**: Demo completion, POC results, ROI projection
**Duration**: 7-14 days
**Tests**: 12 scenarios

---

### Subprocess 5: Negotiation V3

**Purpose**: Terms negotiation, pricing, and contract structure
**Key Features**:
- Dynamic pricing based on customer segment
- Discount approval workflow
- Terms negotiation tracking
- Legal review coordination

**Input**: Proposed solution, pricing
**Output**: Negotiated contract terms
**Duration**: 10-20 days
**Tests**: 15 scenarios

---

### Subprocess 6: Approval V3

**Purpose**: Internal approval (finance, legal, executive)
**Key Features**:
- Multi-level approval routing
- Risk assessment scoring
- Margin analysis
- Executive escalation

**Input**: Negotiated contract
**Output**: Approval status (approved/rejected)
**Duration**: 3-7 days
**Tests**: 10 scenarios

---

### Subprocess 7: Closing V3

**Purpose**: Contract execution and deal finalization
**Key Features**:
- E-signature integration (DocuSign)
- Payment terms setup
- Kickoff meeting scheduling
- Deal won celebration

**Input**: Approved contract
**Output**: Signed contract, kickoff date
**Duration**: 2-5 days
**Tests**: 10 scenarios

---

### Subprocess 8: Beneficiary Onboarding V3 (ANS REQUIRED)

**Purpose**: Legally mandated beneficiary registration
**Key Features**:
- ANS compliance validation
- Beneficiary data collection
- Regulatory submission
- Onboarding status tracking

**Input**: Signed contract, beneficiary list
**Output**: ANS registration confirmation
**Duration**: 5-10 days
**Tests**: 18 scenarios (compliance-critical)

---

### Subprocess 9: Digital Services Activation V3

**Purpose**: Portal access, credentials, training
**Key Features**:
- User provisioning
- Credential generation
- Training materials delivery
- Support ticket system setup

**Input**: Beneficiary list
**Output**: Active user accounts
**Duration**: 2-3 days
**Tests**: 10 scenarios

---

### Subprocess 10: Implementation Planning V3

**Purpose**: Project plan, resource allocation, timeline
**Key Features**:
- Gantt chart generation
- Resource allocation optimization
- Risk assessment
- Stakeholder communication plan

**Input**: Contract scope
**Output**: Approved implementation plan
**Duration**: 5-7 days
**Tests**: 12 scenarios

---

### Subprocess 11: Project Execution V3

**Purpose**: System deployment, customization, integration
**Key Features**:
- Sprint-based execution (Agile)
- Change request management
- Quality assurance checkpoints
- Client UAT coordination

**Input**: Implementation plan
**Output**: Deployed system
**Duration**: 30-60 days
**Tests**: 20 scenarios

---

### Subprocess 12: Post-Launch Monitoring V3

**Purpose**: First 90 days monitoring, issue resolution
**Key Features**:
- Health dashboard
- Proactive issue detection
- Customer satisfaction tracking
- Performance optimization

**Input**: Live system
**Output**: 90-day health report
**Duration**: 90 days
**Tests**: 15 scenarios

---

### Subprocess 13: Contract Expansion V3

**Purpose**: Upsell, cross-sell, contract renewal
**Key Features**:
- Usage analytics for upsell triggers
- Renewal forecasting
- Expansion opportunity scoring
- Executive business review (EBR) scheduling

**Input**: Customer usage data
**Output**: Expansion contract
**Duration**: Ongoing
**Tests**: 12 scenarios

---

## 📅 SECTION 4: IMPLEMENTATION SEQUENCE

### Phase 1: Foundation (Weeks 1-8)

**Deliverables**:
1. **Main Orchestrator Process** (`main-orchestrator-v3.bpmn`)
   - 450-500 lines BPMN XML
   - 13 call activities to subprocesses
   - 3 parallel gateways (post-sales optimization)
   - 5 error boundaries
   - 2 timer boundaries (48h, 120d)
   - 1 compensation handler

2. **Core Infrastructure**
   - PostgreSQL database (process state, audit log)
   - Redis cache (session management, frequent queries)
   - RabbitMQ/Kafka (async messaging)
   - Docker Compose setup (local development)

3. **Integration Layer**
   - API Gateway (Kong/Spring Cloud Gateway)
   - Service Registry (Eureka/Consul)
   - CRM connector (Salesforce/HubSpot adapter)
   - Email/SMS service (SendGrid/Twilio wrapper)

4. **Monitoring Setup**
   - Prometheus metrics export
   - Grafana dashboards (process KPIs, system health)
   - ELK stack (centralized logging)
   - Camunda Cockpit (process monitoring)

**Quality Gate 1 Criteria**:
- ✅ Main orchestrator deploys without validation errors
- ✅ All infrastructure services healthy (docker ps shows all green)
- ✅ Integration layer responds to health checks
- ✅ Monitoring dashboards display data
- ✅ Test: Start process with mock subprocesses, verify execution

**Dependencies**: None (greenfield)

**Estimated Effort**: 320 hours (2 architects, 2 developers, 4 weeks)

---

### Phase 2: Sales Subprocesses (Weeks 9-16)

**Deliverables**:
1. `qualification-subprocess-v3.bpmn` (MEDDIC + Fit Score)
2. `engagement-subprocess-v3.bpmn` (Stakeholder mapping)
3. `value-demonstration-subprocess-v3.bpmn` (Demos/POCs)
4. `negotiation-subprocess-v3.bpmn` (Terms negotiation)
5. `approval-subprocess-v3.bpmn` (Internal approval workflow)
6. `closing-subprocess-v3.bpmn` (Contract execution)

**Supporting Components**:
- 15 service delegates (Java/Node.js classes)
- 8 DMN decision tables (scoring logic)
- 12 user task forms (HTML5/React)
- 6 AI integration services (GPT-4/Claude adapters)

**Quality Gate 2 Criteria**:
- ✅ All 6 sales subprocesses deployed and callable
- ✅ Unit tests: 80+ tests, 100% passing
- ✅ Integration tests: Happy path (qualification → closing) passes
- ✅ MEDDIC methodology validated by sales team
- ✅ Fit score algorithm matches V1 results (regression test)
- ✅ CRM integration tested with sandbox account

**Dependencies**: Phase 1 complete

**Estimated Effort**: 480 hours (3 developers, 1 tester, 8 weeks)

---

### Phase 3: Post-Sales Subprocesses (Weeks 17-24)

**Deliverables**:
1. `beneficiary-onboarding-subprocess-v3.bpmn` (ANS compliance)
2. `digital-services-activation-subprocess-v3.bpmn` (User provisioning)
3. `implementation-planning-subprocess-v3.bpmn` (Project planning)
4. `project-execution-subprocess-v3.bpmn` (Agile delivery)
5. `post-launch-monitoring-subprocess-v3.bpmn` (Health monitoring)
6. `contract-expansion-subprocess-v3.bpmn` (Upsell/renewal)

**Supporting Components**:
- 12 service delegates
- 4 DMN tables
- 10 user task forms
- ANS API integration (regulatory submission)
- Project management tool integration (Jira/Asana)

**Quality Gate 3 Criteria**:
- ✅ All 13 subprocesses operational
- ✅ End-to-end test: Lead → expansion completes successfully
- ✅ ANS compliance validated by regulatory team
- ✅ Parallel post-sales execution demonstrates 30% time reduction
- ✅ All 14 KPIs calculating correctly
- ✅ Integration tests: 120+ tests, 100% passing

**Dependencies**: Phase 2 complete

**Estimated Effort**: 560 hours (4 developers, 1 tester, 8 weeks)

---

### Phase 4: Advanced Features (Weeks 25-32)

**Deliverables**:
1. **AI Predictive Scoring**
   - Train ML models on historical deal data
   - Real-time win probability calculation
   - Next-best-action recommendations
   - Churn prediction for existing customers

2. **Parallel Optimization Engine**
   - Identify parallelizable tasks dynamically
   - Optimize process paths based on runtime data
   - A/B test process variants

3. **Self-Healing Workflows**
   - Automatic retry logic for transient failures
   - Circuit breaker pattern for external integrations
   - Dead letter queue for failed messages
   - Auto-recovery from common error states

4. **Real-Time Analytics Dashboards**
   - Sales funnel visualization
   - Conversion rate tracking
   - Cycle time heatmaps
   - Revenue forecasting

**Quality Gate 4 Criteria**:
- ✅ AI scoring accuracy >85% (tested on historical data)
- ✅ Parallel execution 30% faster than sequential baseline
- ✅ Self-healing recovers 90% of transient errors automatically
- ✅ Dashboards update in <1s, display 14 KPIs
- ✅ Performance tests: 1000 concurrent instances, <2s response

**Dependencies**: Phase 3 complete, 90 days historical data

**Estimated Effort**: 640 hours (2 ML engineers, 2 developers, 1 architect, 8 weeks)

---

### Phase 5: Validation (Weeks 33-36)

**Deliverables**:
1. **Comprehensive Testing**
   - Execute 180+ test scenarios
   - Performance load testing (1000 concurrent processes)
   - Security penetration testing (OWASP Top 10)
   - Chaos engineering (failure injection)

2. **Production Readiness Assessment**
   - Score all 20 criteria using provided rubric
   - Address gaps to achieve 95/100 target
   - Conduct pre-launch review with stakeholders

3. **Deployment & Cutover**
   - Blue-green deployment to production
   - Data migration from V1/V2 (in-flight processes)
   - User training (sales team, admins)
   - Go-live support (24/7 for first week)

**Quality Gate 5 Criteria**:
- ✅ 180+ tests: 100% passing (zero P0/P1 defects)
- ✅ Performance: 1000 concurrent instances, <2s response time
- ✅ Security: Zero critical vulnerabilities (OWASP scan clean)
- ✅ Production readiness: 95/100 score
- ✅ User acceptance: Sales team trained, sign-off obtained
- ✅ Go-live: Zero rollback, <1% error rate first 48h

**Dependencies**: Phase 4 complete

**Estimated Effort**: 320 hours (2 testers, 1 architect, 1 DevOps, 4 weeks)

---

## 👥 SECTION 5: AGENT ROLES & RESPONSIBILITIES

### Agent Team Structure (12 Agents Total)

#### **Architect Agents** (2 agents)

**Agent 1: Solution Architect**
- **Responsibilities**:
  - Design overall system architecture (microservices, data flow, integration patterns)
  - Make technology choices (Camunda version, database, message broker)
  - Review subprocess interactions for coherence
  - Ensure scalability and performance targets
  - Create architecture documentation

- **Key Deliverables**:
  - System architecture diagram (C4 model)
  - Technology stack decisions document
  - Integration patterns guide
  - Non-functional requirements (NFRs) specification

- **Success Metrics**:
  - Architecture passes scalability review (10x load)
  - Zero architectural rework in Phase 3+
  - Performance SLAs met (<2s response time)

**Agent 2: Process Architect**
- **Responsibilities**:
  - Design BPMN subprocess interactions (call activities, message events)
  - Define process variable scoping and data contracts
  - Ensure MEDDIC methodology correctly implemented
  - Review DMN decision logic
  - Validate error handling patterns

- **Key Deliverables**:
  - Process interaction diagram (all 13 subprocesses)
  - Data contract specifications (input/output for each subprocess)
  - Error handling patterns guide
  - DMN decision table designs

- **Success Metrics**:
  - Zero BPMN validation errors at deployment
  - Process variables properly scoped (no leakage)
  - Error boundaries catch 100% of exceptions

---

#### **Coder Agents** (5 agents)

**Agent 3: BPMN Developer 1 (Sales Processes)**
- **Responsibilities**:
  - Implement subprocesses 1-6 (qualification → closing)
  - Write BPMN XML with proper gateways, events, boundaries
  - Create user task forms (HTML5/React)
  - Implement service delegates for automated tasks

- **Assigned Files**:
  - `main-orchestrator-v3.bpmn`
  - `qualification-subprocess-v3.bpmn`
  - `engagement-subprocess-v3.bpmn`
  - `value-demonstration-subprocess-v3.bpmn`

- **Success Metrics**:
  - 4 BPMN files deployed without errors
  - 40+ unit tests passing
  - Forms render correctly in Camunda Tasklist

**Agent 4: BPMN Developer 2 (Post-Sales Processes)**
- **Responsibilities**:
  - Implement subprocesses 7-13 (closing → expansion)
  - Focus on ANS compliance (beneficiary onboarding)
  - Implement parallel gateways for post-sales optimization
  - Create project management integrations (Jira/Asana)

- **Assigned Files**:
  - `negotiation-subprocess-v3.bpmn`
  - `approval-subprocess-v3.bpmn`
  - `closing-subprocess-v3.bpmn`
  - `beneficiary-onboarding-subprocess-v3.bpmn`
  - `digital-services-activation-subprocess-v3.bpmn`
  - `implementation-planning-subprocess-v3.bpmn`
  - `project-execution-subprocess-v3.bpmn`
  - `post-launch-monitoring-subprocess-v3.bpmn`
  - `contract-expansion-subprocess-v3.bpmn`

- **Success Metrics**:
  - 9 BPMN files deployed without errors
  - ANS compliance validated by regulatory team
  - Parallel execution reduces post-sales time by 30%

**Agent 5: Backend Developer (Integration Layer)**
- **Responsibilities**:
  - Implement CRM integration (Salesforce/HubSpot REST API)
  - Build ERP connector (SAP/Oracle SOAP/REST)
  - Create email/SMS service (SendGrid/Twilio wrapper)
  - Implement AI service adapters (GPT-4/Claude)
  - Build analytics data pipeline (process metrics → data warehouse)

- **Key Deliverables**:
  - `CRMConnector.java` (Salesforce/HubSpot adapter)
  - `ERPConnector.java` (SAP/Oracle adapter)
  - `NotificationService.java` (SendGrid/Twilio wrapper)
  - `AIService.java` (GPT-4/Claude adapter)
  - `AnalyticsPublisher.java` (metrics exporter)

- **Success Metrics**:
  - CRM sync <1s per record
  - External API timeout handling (retry 3x with backoff)
  - AI recommendations generate in <2s

**Agent 6: Full-Stack Developer (Dashboards & UI)**
- **Responsibilities**:
  - Build real-time analytics dashboards (React + Grafana)
  - Create process monitoring UI (Camunda Cockpit extensions)
  - Implement user task forms (React components)
  - Build admin panel (process management, user management)

- **Key Deliverables**:
  - Sales funnel dashboard (React app)
  - KPI tracking dashboard (14 metrics)
  - User task forms (12 forms total)
  - Admin panel (process deployment, user roles)

- **Success Metrics**:
  - Dashboards update in <1s
  - Forms pass accessibility audit (WCAG 2.1 AA)
  - Admin panel supports all CRUD operations

**Agent 7: ML Engineer (AI Features)**
- **Responsibilities**:
  - Train predictive models (win probability, churn prediction)
  - Implement real-time scoring service (REST API)
  - Build AI recommendation engine (next-best-action)
  - Create model monitoring and retraining pipeline

- **Key Deliverables**:
  - Win probability model (scikit-learn/TensorFlow)
  - Churn prediction model (XGBoost)
  - Recommendation engine (collaborative filtering)
  - Model API (Flask/FastAPI)
  - Retraining pipeline (Airflow/Kubeflow)

- **Success Metrics**:
  - Model accuracy >85% on test set
  - Inference latency <100ms
  - Model retraining weekly with new data

---

#### **Tester Agents** (2 agents)

**Agent 8: QA Engineer 1 (Functional Testing)**
- **Responsibilities**:
  - Write 180+ test scenarios (unit + integration + E2E)
  - Execute manual exploratory testing
  - Create test data sets (leads, companies, contracts)
  - Validate business logic (MEDDIC scoring, fit score)
  - Test CRM integration end-to-end

- **Test Breakdown**:
  - Unit tests: 80 (service delegates, calculators)
  - Integration tests: 60 (subprocess interactions)
  - End-to-end tests: 40 (lead → expansion)

- **Success Metrics**:
  - 180+ tests, 100% passing
  - Zero P0/P1 defects in production first 90 days
  - Test coverage >80% (code coverage report)

**Agent 9: QA Engineer 2 (Performance & Security)**
- **Responsibilities**:
  - Conduct performance load testing (JMeter/Gatling)
  - Execute security penetration testing (OWASP ZAP)
  - Perform chaos engineering (failure injection)
  - Validate scalability (1000 concurrent processes)
  - Test self-healing workflows (error recovery)

- **Test Scenarios**:
  - Load test: 1000 concurrent process instances
  - Stress test: Ramp up to 5000 instances, measure degradation
  - Security scan: OWASP Top 10 vulnerabilities
  - Chaos: Kill database, message broker, external APIs

- **Success Metrics**:
  - Performance: <2s response time at 1000 concurrent
  - Security: Zero critical vulnerabilities
  - Self-healing: 90% error auto-recovery

---

#### **Reviewer Agents** (2 agents)

**Agent 10: Code Reviewer 1 (BPMN & Process Logic)**
- **Responsibilities**:
  - Review all BPMN files for correctness
  - Validate against requirements (13 subprocess specs)
  - Check error handling patterns (boundaries, compensation)
  - Ensure process variables properly scoped
  - Verify DMN decision logic

- **Review Checklist**:
  - ✅ BPMN validates without errors (Camunda Modeler)
  - ✅ All gateways have outgoing flows
  - ✅ Error boundaries catch expected exceptions
  - ✅ Timers have correct durations
  - ✅ Process variables follow naming conventions

- **Success Metrics**:
  - Zero BPMN defects post-review
  - 100% of PRs reviewed within 24 hours
  - Quality gate enforcement (no merge without approval)

**Agent 11: Code Reviewer 2 (Backend & Integration)**
- **Responsibilities**:
  - Review Java/Node.js code (service delegates)
  - Validate integration patterns (REST, SOAP)
  - Check error handling (try-catch, retries, timeouts)
  - Ensure security best practices (no hardcoded secrets)
  - Verify logging and monitoring instrumentation

- **Review Checklist**:
  - ✅ Code follows style guide (Checkstyle/ESLint clean)
  - ✅ Unit tests included (>80% coverage)
  - ✅ Error handling comprehensive
  - ✅ Secrets externalized (environment variables)
  - ✅ Logging includes correlation IDs

- **Success Metrics**:
  - Zero code defects post-review
  - SonarQube quality gate passes (A rating)
  - Security scan clean (no SQL injection, XSS, etc.)

---

#### **Documentation Agent** (1 agent)

**Agent 12: Technical Writer**
- **Responsibilities**:
  - Write technical specifications for all 13 subprocesses
  - Create user guides (sales reps, admins)
  - Document API contracts (OpenAPI 3.1)
  - Write deployment runbooks (Docker, Kubernetes)
  - Create troubleshooting guides

- **Key Deliverables**:
  - Technical specification (50+ pages)
  - User guide (25+ pages with screenshots)
  - API documentation (Swagger UI)
  - Deployment runbook (step-by-step guide)
  - Troubleshooting guide (common issues + solutions)

- **Success Metrics**:
  - Documentation reviewed by 3+ stakeholders
  - User guide tested with sales team (5+ users)
  - API docs match implementation (validated by tests)

---

## 🚦 SECTION 6: QUALITY GATES

### Gate 1: End of Phase 1 (Week 8)

**Criteria**:
1. ✅ **Main orchestrator deploys successfully**
   - Validation: Deploy to local Camunda, start process instance
   - Expected: Process starts without errors, reaches first call activity

2. ✅ **All infrastructure services operational**
   - Validation: `docker ps` shows all services healthy
   - Expected: PostgreSQL, Redis, RabbitMQ, Camunda all green

3. ✅ **Integration layer tested with mocks**
   - Validation: Call CRM mock API, verify response
   - Expected: API gateway routes requests, returns 200 OK

4. ✅ **Monitoring dashboards display data**
   - Validation: Open Grafana, check process metrics
   - Expected: At least 5 metrics visible (process count, duration, etc.)

**Approval Process**:
- **Reviewers**: Solution Architect, Process Architect
- **Artifacts**: Architecture diagram, deployment logs, health check results
- **Decision**: GO/NO-GO to Phase 2

**Contingency Plan (if NO-GO)**:
- Extend Phase 1 by 2 weeks
- Address infrastructure issues
- Re-test before Phase 2 kickoff

---

### Gate 2: End of Phase 2 (Week 16)

**Criteria**:
1. ✅ **All 6 sales subprocesses passing unit tests**
   - Validation: Run `mvn test` or `npm test`
   - Expected: 80+ tests, 100% passing, <5min execution time

2. ✅ **Integration tests between subprocesses passing**
   - Validation: Run E2E test (qualification → engagement → demo → negotiation → approval → closing)
   - Expected: Happy path completes in <5min (simulated time), no errors

3. ✅ **MEDDIC + fit score dual methodology validated**
   - Validation: Test enterprise deal ($1M) routes to MEDDIC, SMB deal ($100K) routes to fit score
   - Expected: Correct routing, scores calculate as expected

4. ✅ **CRM integration tested end-to-end**
   - Validation: Create lead in BPMN, verify update in Salesforce sandbox
   - Expected: Lead status updated within 5 seconds

**Approval Process**:
- **Reviewers**: Process Architect, Code Reviewer 1, QA Engineer 1
- **Artifacts**: Test reports, CRM integration logs, demo recording
- **Decision**: GO/NO-GO to Phase 3

**Contingency Plan (if NO-GO)**:
- Fix failing tests (allocate 1 week)
- Re-validate CRM integration
- Re-run gate criteria

---

### Gate 3: End of Phase 3 (Week 24)

**Criteria**:
1. ✅ **All 13 subprocesses deployed and operational**
   - Validation: Deploy all subprocesses to test environment
   - Expected: All processes callable, no deployment errors

2. ✅ **End-to-end test: Lead → expansion passing**
   - Validation: Run full lifecycle test (simulated data)
   - Expected: Process completes successfully, all phases executed, <75 days (simulated)

3. ✅ **ANS compliance validated**
   - Validation: Submit test beneficiary data to ANS sandbox API
   - Expected: Registration accepted, confirmation received

4. ✅ **Parallel post-sales execution demonstrates 30% time reduction**
   - Validation: Compare sequential vs parallel execution time
   - Expected: Parallel saves ~30% time (e.g., 10 days → 7 days)

5. ✅ **All 14 KPIs calculating correctly**
   - Validation: Inspect end-of-process KPI snapshot
   - Expected: All 14 metrics present, values reasonable

6. ✅ **Integration tests: 120+ passing**
   - Validation: Run full integration test suite
   - Expected: 120+ tests, 100% passing, <15min execution

**Approval Process**:
- **Reviewers**: Process Architect, Solution Architect, Regulatory Specialist (ANS)
- **Artifacts**: Test reports, ANS submission logs, KPI snapshot, performance comparison
- **Decision**: GO/NO-GO to Phase 4

**Contingency Plan (if NO-GO)**:
- Address ANS compliance issues (regulatory priority)
- Fix KPI calculation bugs
- Re-test parallel execution
- Extend timeline 2 weeks if needed

---

### Gate 4: End of Phase 4 (Week 32)

**Criteria**:
1. ✅ **AI scoring accuracy >85%**
   - Validation: Test ML model on historical deal data (last 12 months)
   - Expected: Win probability prediction accuracy >85%, precision/recall balanced

2. ✅ **Parallel optimization 30% faster**
   - Validation: Benchmark baseline (sequential) vs optimized (parallel) process execution
   - Expected: Optimized path reduces cycle time by ≥30%

3. ✅ **Self-healing recovers 90% of transient errors**
   - Validation: Inject 100 transient errors (network timeout, DB deadlock, etc.)
   - Expected: ≥90 errors auto-recovered without manual intervention

4. ✅ **Dashboards update in <1s, display 14 KPIs**
   - Validation: Load dashboard, measure time to first paint
   - Expected: <1s load time, all 14 KPIs visible and accurate

5. ✅ **Performance: 1000 concurrent instances, <2s response**
   - Validation: Run JMeter load test (1000 concurrent process starts)
   - Expected: Average response time <2s, 95th percentile <5s, zero errors

**Approval Process**:
- **Reviewers**: Solution Architect, ML Engineer, QA Engineer 2
- **Artifacts**: ML model evaluation report, performance benchmark results, self-healing logs, dashboard screenshots
- **Decision**: GO/NO-GO to Phase 5

**Contingency Plan (if NO-GO)**:
- Retrain ML models if accuracy <85%
- Profile and optimize performance bottlenecks
- Enhance self-healing logic for additional error types
- Extend timeline 2 weeks if needed

---

### Gate 5: End of Phase 5 (Week 36)

**Criteria**:
1. ✅ **180+ tests: 100% passing**
   - Validation: Run full test suite (unit + integration + E2E)
   - Expected: 180+ tests, 100% passing, zero P0/P1 defects

2. ✅ **Performance: 1000 concurrent instances, <2s response time**
   - Validation: Re-run load test on production-like environment
   - Expected: Same performance as Gate 4, no degradation

3. ✅ **Security: Zero critical vulnerabilities**
   - Validation: Run OWASP ZAP scan, review report
   - Expected: Zero high/critical findings, low/medium acceptable with mitigation plan

4. ✅ **Production readiness: 95/100 score**
   - Validation: Complete production readiness rubric (20 criteria)
   - Expected: Score ≥95/100, all critical criteria met

5. ✅ **User acceptance: Sales team trained, sign-off obtained**
   - Validation: Conduct training sessions (3+), collect feedback surveys
   - Expected: ≥80% satisfaction, sign-off from sales director

6. ✅ **Go-live: Zero rollback, <1% error rate first 48h**
   - Validation: Monitor production for 48 hours post-launch
   - Expected: No rollback triggered, error rate <1%, zero data loss

**Approval Process**:
- **Reviewers**: Solution Architect, Product Owner, Sales Director, Executive Sponsor
- **Artifacts**: Test reports, security scan results, readiness scorecard, training feedback, go-live metrics
- **Decision**: GO/NO-GO to production launch

**Contingency Plan (if NO-GO)**:
- Address critical defects (allocate 1 week)
- Re-run security scan if vulnerabilities found
- Conduct additional user training if satisfaction <80%
- Delay launch by 2 weeks, re-run gate criteria

---

## 📊 SECTION 7: SUCCESS METRICS

### Technical Metrics

**Code Quality**:
- ✅ **9,500+ lines of BPMN XML** (13 subprocesses + main orchestrator)
- ✅ **2,500+ lines of service delegate code** (Java/Node.js)
- ✅ **180+ comprehensive tests** (unit + integration + E2E)
- ✅ **>80% code coverage** (SonarQube report)
- ✅ **Zero P0/P1 defects** in production first 90 days

**Performance**:
- ✅ **75-day average sales cycle** (vs 120 days V1, 90 days V2)
- ✅ **<2s response time** for process start (at 1000 concurrent instances)
- ✅ **<500ms per phase transition** (subprocess call overhead)
- ✅ **30% time reduction** via parallel post-sales execution
- ✅ **10x scalability** (1000 concurrent process instances vs 100 baseline)

**Reliability**:
- ✅ **99.9% uptime SLA** (max 43 minutes downtime/month)
- ✅ **<1% process instance failure rate** (transient errors auto-recovered)
- ✅ **90% self-healing success rate** (errors resolved without manual intervention)
- ✅ **Zero data loss** (all process state persisted)

**Production Readiness**:
- ✅ **95/100 production readiness score** (using provided rubric)
- ✅ **Zero critical security vulnerabilities** (OWASP scan clean)
- ✅ **ANS compliance validated** (beneficiary onboarding meets regulatory requirements)

---

### Business Metrics

**Sales Performance**:
- ✅ **21% conversion rate** (vs 18% V1, 19% V2) = **+15% improvement**
- ✅ **75-day cycle time** (vs 120 days V1, 90 days V2) = **38% reduction vs V1, 17% vs V2**
- ✅ **$5M+ incremental annual revenue** (from faster cycles + higher conversion)
- ✅ **65 NPS score** (vs 45 V1) = **+20 points improvement**

**Operational Efficiency**:
- ✅ **95% on-time implementations** (vs industry 70%)
- ✅ **40% reduction in sales rep manual tasks** (automation + AI recommendations)
- ✅ **14 KPI tracking metrics** (vs 6 V1, 1 V2) = **data-driven decision making**
- ✅ **50% reduction in escalations** (self-healing + proactive alerts)

**Customer Satisfaction**:
- ✅ **90% customer satisfaction score** (post-implementation surveys)
- ✅ **85% adoption rate** (active usage of digital services)
- ✅ **25% reduction in support tickets** (better onboarding + documentation)
- ✅ **30% increase in upsell opportunities** (expansion subprocess success)

---

### Quality Metrics

**Testing Coverage**:
- ✅ **180+ test scenarios executed**
- ✅ **100% passing rate** (zero flaky tests)
- ✅ **>80% code coverage** (SonarQube)
- ✅ **Zero P0/P1 defects** first 90 days production

**Process Quality**:
- ✅ **13 subprocesses operational** (100% feature completeness)
- ✅ **Zero BPMN validation errors** (all processes deployable)
- ✅ **100% input/output contract adherence** (no variable leakage)
- ✅ **Error boundaries catch 100% of exceptions** (no unhandled errors)

**Documentation**:
- ✅ **Technical specification: 50+ pages** (all 13 subprocesses documented)
- ✅ **User guide: 25+ pages** (with screenshots and examples)
- ✅ **API documentation: OpenAPI 3.1** (Swagger UI available)
- ✅ **Deployment runbook: Step-by-step guide** (tested by DevOps)

---

### Comparison: V1 vs V2 vs V3

| Metric | V1 (Baseline) | V2 (Current) | V3 (Target) | Improvement |
|--------|---------------|--------------|-------------|-------------|
| **Processes** | 11 | 1 (orchestrator only) | 13 | +18% coverage |
| **Lines of Code** | 3,217 | 6,356 | 9,500+ | +195% vs V1 |
| **Tests** | 78 | 0 | 180+ | +131% vs V1 |
| **Sales Cycle** | 120 days | 90 days | 75 days | -38% vs V1 |
| **Conversion Rate** | 18% | 19% | 21% | +17% vs V1 |
| **Production Readiness** | 79/100 | 58/100 | 95/100 | +20% vs V1 |
| **KPI Tracking** | 6 metrics | 1 metric | 14 metrics | +133% vs V1 |
| **NPS Score** | 45 | N/A | 65 | +44% vs V1 |
| **Scalability** | 100 concurrent | Unknown | 1,000 concurrent | 10x vs V1 |

**Key Insights**:
- V3 achieves **best of both worlds**: V1's breadth + V2's depth
- **38% cycle time reduction** translates to **$5M+ annual revenue** (assuming $50K average deal size, 100 deals/year)
- **95/100 production readiness** ensures smooth launch and minimal disruptions
- **180+ tests** provide confidence for continuous delivery (vs V2's zero tests)

---

## 🎯 FINAL DELIVERABLE CHECKLIST

### Code Artifacts
- ✅ 13 BPMN files (`*-subprocess-v3.bpmn`)
- ✅ 1 main orchestrator (`main-orchestrator-v3.bpmn`)
- ✅ 27 service delegate classes (Java/Node.js)
- ✅ 12 DMN decision tables
- ✅ 22 user task forms (HTML5/React)
- ✅ 5 AI service adapters (GPT-4/Claude)
- ✅ 10 integration connectors (CRM, ERP, email, SMS, etc.)

### Test Artifacts
- ✅ 80 unit tests (service delegates)
- ✅ 60 integration tests (subprocess interactions)
- ✅ 40 end-to-end tests (full lifecycle)
- ✅ Load test suite (JMeter/Gatling)
- ✅ Security test suite (OWASP ZAP)
- ✅ Chaos engineering suite (failure injection)

### Documentation
- ✅ Technical specification (50+ pages)
- ✅ User guide (25+ pages)
- ✅ API documentation (OpenAPI 3.1)
- ✅ Deployment runbook
- ✅ Troubleshooting guide
- ✅ Architecture diagrams (C4 model)

### Infrastructure
- ✅ Docker Compose (local development)
- ✅ Kubernetes manifests (production deployment)
- ✅ CI/CD pipelines (GitHub Actions/GitLab CI)
- ✅ Monitoring setup (Prometheus + Grafana + ELK)
- ✅ Terraform scripts (infrastructure as code)

### Quality Assurance
- ✅ Production readiness scorecard (95/100)
- ✅ Security scan report (zero critical vulnerabilities)
- ✅ Performance benchmark report (<2s response time)
- ✅ User acceptance test results (80%+ satisfaction)

---

## 🚀 AGENT EXECUTION INSTRUCTIONS

### For Architect Agents:
1. Read this entire prompt (estimated 30 minutes)
2. Design system architecture diagram using C4 model
3. Document technology choices with rationale
4. Create subprocess interaction diagram (all 13 subprocesses)
5. Define data contracts for each subprocess (input/output)
6. Review with peer architect before proceeding

### For Coder Agents:
1. Read assigned subprocess specifications (Section 3)
2. Set up local development environment (Docker Compose)
3. Implement BPMN files using Camunda Modeler
4. Write service delegates for automated tasks
5. Create user task forms (HTML5/React)
6. Write unit tests (aim for >80% coverage)
7. Submit code for review (create pull request)
8. Address review feedback
9. Deploy to test environment
10. Validate with integration tests

### For Tester Agents:
1. Read all subprocess specifications
2. Create test plan (180+ scenarios)
3. Write automated tests (unit + integration + E2E)
4. Execute manual exploratory testing
5. Conduct performance testing (JMeter load tests)
6. Run security scans (OWASP ZAP)
7. Document bugs in issue tracker
8. Re-test after fixes
9. Sign off on quality gates

### For Reviewer Agents:
1. Review all pull requests within 24 hours
2. Use provided checklists for consistency
3. Validate BPMN against specifications
4. Check code quality (SonarQube scan)
5. Ensure security best practices
6. Verify test coverage >80%
7. Approve or request changes
8. Track quality metrics

### For Documentation Agent:
1. Review all implemented subprocesses
2. Interview developers for technical details
3. Create user guides with screenshots
4. Write API documentation (OpenAPI 3.1)
5. Develop deployment runbooks
6. Create troubleshooting guides
7. Review with stakeholders
8. Publish to wiki/knowledge base

---

## 💡 ADDITIONAL CONTEXT

### MEDDIC Methodology Reference
- **Metrics**: Quantifiable economic benefits (ROI, cost savings)
- **Economic Buyer**: Person with budget authority and power to say yes
- **Decision Criteria**: Formal and informal criteria used to evaluate solutions
- **Decision Process**: Steps required to make a purchase (legal, procurement, approvals)
- **Identify Pain**: Business pain with consequences (hair on fire, bleeding neck)
- **Champion**: Internal advocate who actively sells on your behalf

### ANS (Agência Nacional de Saúde Suplementar) Compliance
- **Beneficiary onboarding is legally required** within 10 days of contract signature
- **Data requirements**: Full name, CPF, birth date, address, health plan tier
- **Submission**: Electronic submission to ANS portal (API integration)
- **Confirmation**: ANS returns registration number (must be stored in process)
- **Penalty**: Non-compliance results in fines (R$10K-R$100K per case)

### KPI Definitions (14 Metrics)
1. **Lead-to-Opportunity Conversion Rate**: % of leads that become qualified opportunities
2. **Opportunity-to-Win Rate**: % of opportunities that close as won
3. **Average Sales Cycle Time**: Days from lead capture to contract signed
4. **Sales Velocity**: (# of opportunities × average deal size × win rate) / cycle time
5. **MEDDIC Score Distribution**: % of deals by score range (HIGH/MEDIUM/LOW)
6. **Stakeholder Engagement Score**: Average engagement across all stakeholders
7. **Demo-to-Proposal Conversion**: % of demos that result in proposals
8. **Proposal-to-Close Rate**: % of proposals that result in signed contracts
9. **Discount Rate**: Average discount given from list price
10. **Implementation On-Time Rate**: % of implementations completed by target date
11. **Customer NPS Score**: Net Promoter Score post-implementation
12. **Upsell Opportunity Rate**: % of customers with expansion opportunities
13. **Churn Risk Score**: AI-predicted likelihood of churn (0-100)
14. **Revenue per Customer**: Average annual recurring revenue per customer

---

## 🎬 GETTING STARTED

### Immediate Next Steps:
1. **Week 1**: Architect agents design system architecture and subprocess interactions
2. **Week 2**: All agents set up local development environments
3. **Week 3-8**: Phase 1 implementation (foundation)
4. **Week 8**: Quality Gate 1 review
5. **Continue per implementation sequence...**

### Agent Coordination:
- **Daily standup**: 15-minute sync on progress and blockers
- **Weekly demo**: Show completed work to stakeholders
- **Bi-weekly retrospective**: Improve team processes
- **Quality gate reviews**: Formal approval before phase transitions

### Communication Channels:
- **Slack**: Real-time chat (#v3-development)
- **Jira**: Task tracking and bug management
- **Confluence**: Documentation and knowledge sharing
- **GitHub**: Code repository and pull requests
- **Zoom**: Video calls for complex discussions

---

## ✅ READY TO BUILD?

**This prompt contains everything you need to build AUSTA V3:**
- ✅ Vision and objectives (why we're building this)
- ✅ Architecture constraints (what must be true)
- ✅ 13 subprocess specifications (what to build)
- ✅ Implementation sequence (how to build)
- ✅ Agent roles (who builds what)
- ✅ Quality gates (how to validate)
- ✅ Success metrics (how to measure)

**You can start immediately. Go build something amazing! 🚀**
