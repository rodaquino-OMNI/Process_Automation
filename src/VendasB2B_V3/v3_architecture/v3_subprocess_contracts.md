# V3 Subprocess Contracts Specification
**AUSTA B2B Sales Automation Platform - Version 3**

**Document Purpose**: Define input/output contracts for all 13 subprocesses in the V3 orchestrator

**Date**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Version**: 1.0.0

---

## OVERVIEW

This document specifies the data contracts (input/output variables) for all 13 subprocesses in the V3 architecture. These contracts enable:
- **Loose coupling**: Subprocesses can be independently developed and tested
- **Clear boundaries**: Explicit variable mapping prevents data leakage
- **Easier debugging**: Input/output inspection at subprocess boundaries
- **Version compatibility**: Contract versioning enables parallel subprocess versions

**Contract Pattern**: Explicit I/O mapping (V2 pattern) with business key correlation

---

## CONTRACT TEMPLATE

Each subprocess contract includes:
1. **Process ID**: Unique Camunda process definition ID
2. **Duration**: Expected execution time (SLA)
3. **Input Variables**: Required and optional inputs with types
4. **Output Variables**: Guaranteed outputs with types
5. **Business Rules**: Conditions and constraints
6. **Error Codes**: Possible error events thrown
7. **Compensation**: Rollback actions if needed

---

## SUBPROCESS 1: LEAD QUALIFICATION

### Basic Information
- **Process ID**: `Process_Lead_Qualification_V3`
- **CallActivity ID in Main**: `CallActivity_QualificationPhase`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: MEDDIC scoring with 4-way decision gateway
- **Error Code**: `QUAL_ERROR`

### Input Variables
```yaml
Required:
  - leadSource: string
      enum: [website, partner, inbound, outbound, event]
      description: "Lead origin channel"

  - companyName: string
      maxLength: 255
      description: "Client company name"

  - companySize: integer
      minimum: 1
      description: "Number of lives (employees + dependents)"

  - contactName: string
      maxLength: 255
      description: "Primary contact person"

  - contactEmail: string
      format: email
      description: "Contact email address"

  - contactPhone: string
      format: phone (Brazilian)
      description: "Contact phone number"

  - industry: string
      maxLength: 100
      description: "Industry sector"

  - urgency: string
      enum: [low, medium, high, critical]
      description: "Deal urgency level"

Optional:
  - estimatedValue: long
      description: "Estimated deal value (BRL)"

  - scoreMEDDIC: integer
      range: [0, 10]
      description: "Previous MEDDIC score (if requalification)"
```

### Output Variables
```yaml
Always Returned:
  - scoreMEDDIC: integer
      range: [0, 10]
      description: "MEDDIC qualification score"
      calculation: "DMN decision table (6 dimensions)"

  - scoreHistorico: array
      itemType: object
      description: "Score history with timestamps"
      example: [{ data: "2025-12-08T10:00:00Z", score: 8, fase: "qualificacao" }]

  - isQualified: boolean
      description: "Overall qualification result"
      logic: "scoreMEDDIC >= 6"

  - qualificationLevel: string
      enum: [high, medium, low, disqualified]
      description: "Qualification tier"
      mapping: "high (>=8), medium (6-7), low (4-5), disqualified (<4)"

  - leadQualificationDuration: long
      unit: milliseconds
      description: "Time spent in qualification phase"

Conditional (if not qualified):
  - disqualificationReason: string
      description: "Why lead was disqualified"
      examples: ["MEDDIC score too low", "No economic buyer access", "Timeline too long"]

Conditional (if qualified):
  - nextActions: array
      itemType: string
      description: "Recommended next steps"
      examples: ["Schedule discovery meeting", "Request financial statements", "Identify champion"]
```

### Business Rules
```
Qualification Tiers:
  IF scoreMEDDIC >= 8 THEN
    qualificationLevel = 'high'
    isQualified = true
    Route to: Discovery (direct path)

  ELIF scoreMEDDIC >= 6 AND scoreMEDDIC < 8 THEN
    qualificationLevel = 'medium'
    isQualified = true
    Route to: Opportunity development subprocess → Discovery

  ELIF scoreMEDDIC >= 4 AND scoreMEDDIC < 6 THEN
    qualificationLevel = 'low'
    isQualified = false
    Route to: Nurturing campaign (not in main flow)

  ELSE (scoreMEDDIC < 4) THEN
    qualificationLevel = 'disqualified'
    isQualified = false
    Route to: Loss analysis → End process
```

### Error Handling
- **Error Code**: `QUAL_ERROR`
- **Thrown When**: MEDDIC calculation fails, external service unavailable
- **Recovery**: User task for manual qualification entry

### Compensation
- **Compensation Task**: `Task_CompensateQualification`
- **Actions**: Update CRM to "disqualified", cancel scheduled meetings, send notification to sales team
- **Duration**: PT5M (5 minutes)

---

## SUBPROCESS 2: CONSULTATIVE DISCOVERY

### Basic Information
- **Process ID**: `Process_Consultative_Discovery_V3`
- **CallActivity ID in Main**: `CallActivity_ConsultativeDiscovery`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Sequential deep discovery with viability gateway
- **Error Code**: `DISC_ERROR`

### Input Variables
```yaml
Required:
  - scoreMEDDIC: integer
      range: [6, 10]
      description: "Qualification score from previous phase"

  - companyName: string
  - companySize: integer
  - industry: string
  - contactName: string
  - contactEmail: string
  - contactPhone: string

Optional:
  - estimatedValue: long
      description: "Estimated opportunity value"
```

### Output Variables
```yaml
Always Returned:
  - opportunityId: string
      format: UUID or CRM ID
      description: "Unique opportunity identifier in CRM"

  - opportunityValue: long
      unit: BRL
      description: "Calculated annual contract value"
      formula: "companySize * PMPB * 12 - discount"

  - dealPriority: string
      enum: [low, medium, high, critical]
      description: "Opportunity priority based on value and timeline"

  - discoveryCompleted: boolean
      description: "Discovery meeting successfully completed"

Conditional (if discovery completed):
  - painPoints: array
      itemType: string
      description: "Identified business problems"
      minItems: 1

  - decisionMakers: array
      itemType: object
      description: "Stakeholder map with decision-making authority"
      schema: { name: string, role: string, authority: enum[economic_buyer, decision_maker, influencer, user] }

  - currentCoverage: object
      description: "Existing health insurance details"
      schema: { provider: string, planType: string, lives: integer, cost: long }

  - budget: long
      unit: BRL
      description: "Annual budget for health insurance"

  - timeline: string
      format: ISO 8601 duration
      description: "Expected implementation timeline"
      example: "P6M" (6 months)

  - competitorInfo: object
      description: "Competitive landscape analysis"
      schema: { currentProvider: string, switchingBarriers: array, competitiveAdvantage: string }

Conditional (if not viable):
  - disqualificationReason: string
      description: "Why opportunity is not viable"
      examples: ["Value <100K", "Timeline >12 months", "No budget authority"]
```

### Business Rules
```
Viability Check:
  IF opportunityValue >= 100000 AND timeline <= "P6M" THEN
    Viable = true
    Route to: Proposal Elaboration

  ELSE
    Viable = false
    Route to: Disqualified end event
    Note: Consider follow-up in 6 months

Priority Calculation:
  IF opportunityValue >= 1000000 AND timeline <= "P3M" THEN
    dealPriority = 'critical'

  ELIF opportunityValue >= 500000 AND timeline <= "P4M" THEN
    dealPriority = 'high'

  ELIF opportunityValue >= 200000 AND timeline <= "P6M" THEN
    dealPriority = 'medium'

  ELSE
    dealPriority = 'low'
```

### Error Handling
- **Error Code**: `DISC_ERROR`
- **Thrown When**: Meeting cancelled, no decision maker identified, CRM integration failure
- **Recovery**: Reschedule meeting, escalate to sales manager

### Compensation
- **Compensation Task**: `Task_CompensateDiscovery`
- **Actions**: Update CRM opportunity status to "lost - no discovery", cancel follow-up tasks, archive discovery notes
- **Duration**: PT10M (10 minutes)

---

## SUBPROCESS 3: PROPOSAL ELABORATION

### Basic Information
- **Process ID**: `Process_Proposal_Elaboration_V3`
- **CallActivity ID in Main**: `CallActivity_ProposalElaboration`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Iterative proposal with approval loop
- **Error Code**: `PROP_ERROR`

### Input Variables
```yaml
Required:
  - opportunityId: string
  - opportunityValue: long
  - companySize: integer
  - painPoints: array
  - budget: long
  - currentCoverage: object

Optional:
  - proposalVersion: integer
      default: 1
      description: "Proposal revision number"
```

### Output Variables
```yaml
Always Returned:
  - proposalId: string
      format: UUID
      description: "Unique proposal identifier"

  - proposalDocumentUrl: string
      format: URL
      description: "Proposal PDF download link"

  - planType: string
      enum: [ambulatorial, hospitalar, referencia]
      description: "Selected plan type"

  - coparticipation: boolean
      description: "Coparticipation included?"

  - networkType: string
      enum: [nacional, regional, local]
      description: "Provider network coverage"

  - discount: integer
      range: [0, 30]
      unit: percentage
      description: "Applied discount percentage"

  - totalValue: long
      unit: BRL
      description: "Final annual contract value after discount"

  - proposalStatus: string
      enum: [approved, rejected, revision]
      description: "Internal review outcome"

  - proposalVersion: integer
      description: "Proposal revision number (increments on revision)"

Conditional (if revision requested):
  - revisionNotes: string
      description: "Manager feedback for revision"
```

### Business Rules
```
Pricing Calculation (DMN Decision Table):
  Inputs: companySize, planType, coparticipation, networkType, discount
  Outputs: totalValue (BRL annual)

  Base Price Calculation:
    PMPB (Price Per Beneficiary Per Month) = f(planType, networkType, coparticipation)
    Annual Value = companySize * PMPB * 12 * (1 - discount/100)

Discount Constraints:
  IF discount <= 10 THEN
    Approval Required: No (auto-approved)

  ELIF discount > 10 AND discount <= 20 THEN
    Approval Required: Yes (manager level)

  ELIF discount > 20 AND discount <= 30 THEN
    Approval Required: Yes (director level)

  ELSE (discount > 30) THEN
    Rejected: Discount too high

Proposal Review Loop:
  IF internalReview == 'approved' THEN
    proposalStatus = 'approved'
    Route to: Commercial Approval

  ELIF internalReview == 'rejected' THEN
    proposalStatus = 'rejected'
    Route to: Rejected end event

  ELSE (internalReview == 'revision') THEN
    proposalStatus = 'revision'
    proposalVersion += 1
    Route back to: Product selection task
```

### Error Handling
- **Error Code**: `PROP_ERROR`
- **Thrown When**: Pricing calculation fails, document generation error, invalid plan configuration
- **Recovery**: Manual price entry, regenerate document, escalate to pricing team

### Compensation
- **Compensation Task**: `Task_CompensateProposal`
- **Actions**: Archive proposal document, update CRM opportunity status, cancel presentation meeting
- **Duration**: PT5M (5 minutes)

---

## SUBPROCESS 4: COMMERCIAL APPROVAL

### Basic Information
- **Process ID**: `Process_Commercial_Approval_V3`
- **CallActivity ID in Main**: `CallActivity_CommercialApproval`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: 4-tier approval matrix with escalation
- **Error Code**: `APPR_ERROR`

### Input Variables
```yaml
Required:
  - proposalId: string
  - totalValue: long
  - discount: integer
  - opportunityValue: long
  - companyName: string

Optional:
  - dealPriority: string
      enum: [low, medium, high, critical]
      default: medium
```

### Output Variables
```yaml
Always Returned:
  - proposalStatus: string
      enum: [approved, rejected, revision]
      description: "Final approval decision"

  - approvalLevel: string
      enum: [automatic, manager, director, clevel]
      description: "Approval tier reached"

  - approvedBy: string
      description: "User ID of approver"

  - approvalTimestamp: datetime
      format: ISO 8601
      description: "When approval was granted/rejected"

Conditional (if approved):
  - approvalComments: string
      description: "Approver notes"

Conditional (if rejected):
  - rejectionReason: string
      description: "Why proposal was rejected"
      examples: ["Margin too low", "High risk customer", "Market conditions unfavorable"]

Conditional (if revision):
  - revisionRequested: boolean
      value: true
  - revisionNotes: string
      description: "What needs to change for approval"
```

### Business Rules
```
4-Tier Approval Matrix:

Tier 1: Auto-Approval (<50K)
  IF totalValue < 50000 THEN
    approvalLevel = 'automatic'
    proposalStatus = 'approved'
    approvedBy = 'system'
    Duration: PT1M (instant)

Tier 2: Sales Manager (50K-200K)
  IF totalValue >= 50000 AND totalValue < 200000 THEN
    approvalLevel = 'manager'
    Assigned to: Candidate group 'sales-management'
    Decision deadline: PT48H (48 hours)
    Escalation: If no response, escalate to director

Tier 3: Commercial Director (200K-1M)
  IF totalValue >= 200000 AND totalValue < 1000000 THEN
    approvalLevel = 'director'
    Assigned to: Candidate group 'commercial-directors'
    Required fields: riskAssessment (string), strategicImportance (enum)
    Decision deadline: PT72H (72 hours)
    Escalation: If no response, escalate to C-level

Tier 4: C-Level (>=1M)
  IF totalValue >= 1000000 THEN
    approvalLevel = 'clevel'
    Assigned to: Candidate group 'executive-board'
    Required fields: riskAssessment, strategicImportance, competitiveAnalysis, boardRecommendation
    Decision deadline: PT120H (5 days)
    Escalation: If no response, automatic rejection

Enhanced Discount Rules (escalates approval tier):
  IF discount > 15 THEN
    Escalate approval tier by +1 level
    Example: 200K deal with 20% discount → Requires C-level (not director)

  IF discount > 20 THEN
    Always require C-level approval

  IF discount > 30 THEN
    Automatic rejection (no approval tier available)
```

### Error Handling
- **Error Code**: `APPR_ERROR`
- **Thrown When**: Approval timeout (no decision within SLA), invalid approval decision, approver unavailable
- **Recovery**: Escalate to next tier, assign backup approver, manual intervention

### Compensation
- **Compensation Task**: `Task_CompensateApproval`
- **Actions**: Reverse approval status, update CRM to "approval pending", notify sales team
- **Duration**: PT2M (2 minutes)

---

## SUBPROCESS 5: NEGOTIATION

### Basic Information
- **Process ID**: `Process_Negotiation_V3`
- **CallActivity ID in Main**: `CallActivity_NegotiationPhase`
- **Duration SLA**: PT15D (15 days)
- **Pattern**: Multi-round negotiation with escalation
- **Error Code**: `NEG_ERROR`

### Input Variables
```yaml
Required:
  - proposalId: string
  - totalValue: long
  - proposalDocumentUrl: string
  - companyName: string
  - contactName: string
  - contactEmail: string

Optional:
  - negotiationRounds: integer
      default: 0
      description: "Number of negotiation rounds so far"
```

### Output Variables
```yaml
Always Returned:
  - negotiationRounds: integer
      description: "Total negotiation rounds completed"

  - negotiationStatus: string
      enum: [accepted, negotiating, rejected]
      description: "Current negotiation state"

Conditional (if accepted):
  - finalDiscount: integer
      range: [0, 30]
      unit: percentage
      description: "Final agreed discount"

  - paymentTerms: string
      enum: [monthly, quarterly, annual]
      description: "Payment frequency"

  - contractDuration: integer
      range: [12, 60]
      unit: months
      description: "Contract duration"

  - slaTerms: object
      description: "Service level agreement terms"
      schema: { responseTime: string, resolutionTime: string, availability: number }

  - penalties: object
      description: "Penalty clauses"
      schema: { slaViolation: string, terminationFee: string }

  - additionalClauses: array
      itemType: string
      description: "Special terms negotiated"

Conditional (if rejected):
  - rejectionReason: string
      description: "Why negotiation failed"

Conditional (if negotiating):
  - needsAdjustment: boolean
      description: "Proposal revision required before continuing"
  - adjustmentRequests: array
      itemType: string
      description: "What client wants changed"
```

### Business Rules
```
Negotiation Flow:
  1. Present proposal (user task)
  2. Capture client response
     IF response == 'accepted' THEN
       negotiationStatus = 'accepted'
       Route to: Terms finalization

     ELIF response == 'negotiate' THEN
       negotiationStatus = 'negotiating'
       negotiationRounds += 1

       IF negotiationRounds > 3 THEN
         Route to: Escalation (executive involvement)

       ELSE
         Route to: Objection handling → Terms negotiation

     ELSE (response == 'rejected') THEN
       negotiationStatus = 'rejected'
       Route to: Loss analysis → End process

Discount Approval (if new discount > initial):
  IF newDiscount > 15 THEN
    Require executive approval
    Approval gateway: approved/rejected

    IF approved THEN
      finalDiscount = newDiscount
      Route to: Terms finalization

    ELSE
      Route back to: Terms negotiation (max discount capped at 15%)

Contract Terms Constraints:
  - Payment terms: Client choice (monthly/quarterly/annual)
  - Contract duration: 12-60 months (minimum 1 year)
  - SLA terms: Standard template (customizable with approval)
  - Penalties: Standard template (fixed percentages)
```

### Error Handling
- **Error Code**: `NEG_ERROR`
- **Thrown When**: Negotiation deadlock (>3 rounds with no progress), client unresponsive, executive rejection
- **Recovery**: Escalate to VP Sales, offer alternative proposal, schedule executive call

### Compensation
- **Compensation Task**: `Task_CompensateNegotiation`
- **Actions**: Revoke negotiation terms, update CRM to "negotiation failed", notify account manager
- **Duration**: PT5M (5 minutes)

---

## SUBPROCESS 6: CLOSING

### Basic Information
- **Process ID**: `Process_Closing_V3`
- **CallActivity ID in Main**: `CallActivity_ClosingPhase`
- **Duration SLA**: PT15D (15 days)
- **Pattern**: Sequential closing with handoff chain
- **Error Code**: `CLOS_ERROR`

### Input Variables
```yaml
Required:
  - proposalId: string
  - finalDiscount: integer
  - paymentTerms: string
  - contractDuration: integer
  - totalValue: long
  - companyName: string
  - contactName: string
  - contactEmail: string

Optional:
  - slaTerms: object
  - penalties: object
  - additionalClauses: array
```

### Output Variables
```yaml
Always Returned:
  - contractId: string
      format: UUID
      description: "Unique contract identifier"

  - contractUrl: string
      format: URL
      description: "Draft contract PDF download link"

  - contractSigned: boolean
      description: "All parties signed?"

  - dealClosed: boolean
      description: "Deal successfully closed?"

  - closureReason: string
      enum: [won, lost, abandoned]
      description: "Final outcome"

Conditional (if signed):
  - signatureRequestId: string
      description: "E-signature platform request ID"

  - allSigned: boolean
      value: true

  - signedContractUrl: string
      format: URL
      description: "Signed contract PDF download link"

  - cicloVendasDias: integer
      unit: days
      description: "Total sales cycle duration from lead to close"
      calculation: "endDate - processStartDate (in days)"

  - finalValue: long
      unit: BRL
      description: "Final signed contract value"

Conditional (if not signed):
  - signatureTimeout: boolean
      description: "14-day signature deadline exceeded?"
```

### Business Rules
```
Closing Flow:
  1. Generate contract (service task - document generation)
  2. Legal review (user task)
     IF legalApproval == false THEN
       Route back to: Contract revision
     ELSE
       Route to: Signature coordination

  3. Send for signature (service task - DocuSign/Clicksign)
     Signatories: min 2 (client representative + AUSTA representative)

  4. Wait for signatures (message event)
     Timeout: PT14D (14 days)

     IF timeout THEN
       Route to: Follow-up task → Gateway decision (continue waiting vs. abandon)

     ELIF all signed THEN
       Route to: Victory celebration

  5. Validate signatures (service task)
     Check: All required signatories signed
     Output: signedContractUrl

  6. Celebrate victory (user task - team recognition)
     Capture: Lessons learned, success factors, win story

  7. Update CRM to closed_won (service task)

  8. Integrate financial system (service task)
     Create: Customer master record, billing setup, revenue recognition

  9. Prepare onboarding (user task - 5-day deadline)
     Output: Onboarding checklist, kickoff date

  10. Handoff to operations (user task)
      Knowledge transfer to implementation team

  11. Initiate implementation (service task)
      Trigger: Implementation planning subprocess

Victory Metrics (calculated in celebration task):
  - cicloVendasDias = (today - processStartDate) in days
  - scoreMEDDICFinal = 10 (assumption: closed deal has perfect score)
  - contractValue = finalValue
  - discountApplied = finalDiscount
```

### Error Handling
- **Error Code**: `CLOS_ERROR`
- **Thrown When**: Legal rejection, signature platform failure, CRM integration error, financial system error
- **Recovery**: Manual contract revision, resend signature request, manual CRM update, escalate to finance team

### Compensation
- **Compensation Task**: `Task_CompensateClosing`
- **Actions**: Revoke contract (if not finalized), revert CRM status to "negotiation", cancel implementation planning, notify operations team
- **Duration**: PT10M (10 minutes)

---

## SUBPROCESS 7: IMPLEMENTATION PLANNING

### Basic Information
- **Process ID**: `Process_Implementation_Planning_V3`
- **CallActivity ID in Main**: `CallActivity_ImplementationPlanning`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Sequential planning with resource allocation
- **Error Code**: `PLAN_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - companyName: string
  - companySize: integer
  - planType: string
  - networkType: string
  - startDate: date
      format: ISO 8601
      description: "Contract start date"

Optional:
  - slaTerms: object
  - customRequirements: array
```

### Output Variables
```yaml
Always Returned:
  - projectId: string
      format: UUID
      description: "Unique project identifier in project management system"

  - implementationTeam: array
      itemType: object
      schema: { name: string, role: enum[pm, tech_lead, customer_success, support], email: string }
      description: "Implementation team members"
      minItems: 3

  - projectScope: string
      description: "Detailed scope definition"

  - projectTimeline: string
      format: URL or UUID
      description: "Gantt chart reference (link or project management tool ID)"

  - kickoffDate: date
      format: ISO 8601
      description: "Planned kickoff meeting date"
      constraint: "Within 14 days of planning completion"

  - implementationPlanCompleted: boolean
      value: true

Conditional (if risks identified):
  - risks: array
      itemType: object
      schema: { risk: string, likelihood: enum[low, medium, high], impact: enum[low, medium, high], mitigation: string }
      description: "Identified project risks"

Conditional (if resources allocated):
  - resources: array
      itemType: object
      schema: { resourceType: string, quantity: integer, allocation: string }
      description: "Allocated resources (people, tools, budget)"
```

### Business Rules
```
Planning Requirements:
  1. Form Implementation Team (user task)
     Required roles: Project Manager, Tech Lead, Customer Success Manager
     Optional roles: Support Specialist, Trainer, Data Analyst

     Team composition:
       IF companySize < 100 THEN
         minTeamSize = 3
       ELIF companySize >= 100 AND companySize < 500 THEN
         minTeamSize = 5
       ELSE (companySize >= 500) THEN
         minTeamSize = 7

  2. Define Project Scope (user task)
     Includes: System integrations, data migration, training plan, go-live date

  3. Create Project Timeline (service task)
     Auto-generate Gantt chart with milestones:
       - Kickoff: startDate + PT14D (14 days)
       - Integration: kickoffDate + PT7D
       - Migration: integrationComplete + PT3D
       - Training: migrationComplete + PT5D
       - UAT: trainingComplete + PT3D
       - Go-live: uatComplete + PT1D

     Total estimated duration: PT19D (19 days)

  4. Identify Risks (user task)
     Common risks: Integration failures, data quality issues, user resistance, timeline delays

  5. Allocate Resources (user task)
     Resources: Team members, project management tools, training materials, budget

  6. Schedule Kickoff Meeting (user task)
     Attendees: Implementation team + client stakeholders
     Deadline: Within 14 days of planning completion

  7. Update CRM (service task)
     Status: "implementation_planning_complete"
     Create: Project record in CRM

Kickoff Date Constraint:
  kickoffDate >= today + PT3D (minimum 3 days for preparation)
  kickoffDate <= today + PT14D (maximum 14 days)
```

### Error Handling
- **Error Code**: `PLAN_ERROR`
- **Thrown When**: Team formation failure (no available resources), timeline generation error, CRM update failure
- **Recovery**: Escalate to resource manager, manual timeline creation, manual CRM entry

### Compensation
- **Compensation Task**: `Task_CompensatePlanning`
- **Actions**: Cancel project, release allocated resources, update CRM to "planning cancelled", notify team members
- **Duration**: PT5M (5 minutes)

---

## SUBPROCESS 8: PROJECT EXECUTION

### Basic Information
- **Process ID**: `Process_Project_Execution_V3`
- **CallActivity ID in Main**: `CallActivity_ProjectExecution`
- **Duration SLA**: PT15D (15 days)
- **Pattern**: Sequential with parallelization opportunity
- **Error Code**: `EXEC_ERROR`

### Input Variables
```yaml
Required:
  - projectId: string
  - projectTimeline: string
  - implementationTeam: array
  - companyName: string
  - companySize: integer
  - planType: string
  - networkType: string

Optional:
  - customRequirements: array
  - integrationEndpoints: object
```

### Output Variables
```yaml
Always Returned:
  - kickoffCompleted: boolean
  - integrationCompleted: boolean
  - migrationCompleted: boolean
  - trainingCompleted: boolean
  - networksConfigured: boolean
  - uatCompleted: boolean
  - projectSignoff: boolean
  - implementationDuration: long
      unit: days
      description: "Actual implementation time"

Conditional (if completed):
  - goLiveDate: date
      format: ISO 8601
      description: "Actual go-live date"

  - lessonsLearned: string
      description: "Key learnings from implementation"

Conditional (if issues):
  - implementationIssues: array
      itemType: object
      schema: { issue: string, severity: enum[low, medium, high, critical], resolved: boolean }
      description: "Issues encountered during implementation"
```

### Business Rules
```
Execution Flow:
  1. Conduct Kickoff Meeting (user task)
     Agenda: Introductions, scope review, timeline walkthrough, Q&A
     Output: kickoffCompleted = true

  2. [V3 Enhancement: Parallel Gateway]

     Fork (3 concurrent paths):

     Path A (Sequential):
       2a. System Integration (service task)
           Integrations: CRM, ERP, ANS portal, digital services
           Duration: PT5D

       2b. Data Migration (service task)
           Migrate: Beneficiary data, contracts, billing info
           Duration: PT3D
           Dependency: System integration must complete first

     Path B (Parallel):
       2c. Train Client Staff (user task)
           Training: System usage, processes, support escalation
           Duration: PT5D
           Attendees: min 80% of users

     Path C (Parallel):
       2d. Configure Provider Networks (service task)
           Configuration: Provider credentials, service codes, pricing
           Duration: PT4D

     Join (synchronization point)

  3. User Acceptance Testing (user task)
     Assigned to: Client contact (external user)
     Test scenarios: 20+ test cases
     Acceptance criteria: 95% pass rate
     Duration: PT3D

     IF uatPassed == false THEN
       Route to: Issue resolution → Re-test
     ELSE
       Route to: Project signoff

  4. Project Signoff (user task)
     Assigned to: Project sponsor (client-side executive)
     Required: Formal signoff document
     Output: projectSignoff = true

Time Savings (V3 Parallel Execution):
  Sequential (V1/V2): 5 (integration) + 3 (migration) + 5 (training) + 4 (networks) = 17 days
  Parallel (V3): max(8 [A], 5 [B], 4 [C]) = 8 days
  Savings: 9 days (53% reduction)
```

### Error Handling
- **Error Code**: `EXEC_ERROR`
- **Thrown When**: Integration failure, data migration error, UAT failure, client refusal to sign off
- **Recovery**: Rollback integration, re-migrate data, fix issues and re-test, escalate to executive sponsor

### Compensation
- **Compensation Task**: `Task_CompensateExecution`
- **Actions**: Rollback integrations, revert data migration, cancel training sessions, update project status to "cancelled"
- **Duration**: PT30M (30 minutes)

---

## SUBPROCESS 9: BENEFICIARY ONBOARDING

### Basic Information
- **Process ID**: `Process_Beneficiary_Onboarding_V3`
- **CallActivity ID in Main**: `CallActivity_BeneficiaryOnboarding`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Sequential regulatory compliance flow
- **Error Code**: `ONBD_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - companyName: string
  - companySize: integer
  - startDate: date
  - planType: string

Optional:
  - beneficiaryDataFile: string
      format: URL or file path
      description: "Bulk beneficiary data (CSV/Excel)"
```

### Output Variables
```yaml
Always Returned:
  - beneficiaryList: array
      itemType: object
      schema: { cpf: string, name: string, birthDate: date, relationship: enum[titular, dependent], healthCard: string }
      description: "Registered beneficiaries"

  - ansRegistrationId: string
      description: "ANS submission reference number"

  - beneficiaryCount: integer
      description: "Total beneficiaries registered"

  - onboardingCompleted: boolean

Conditional (if completed):
  - healthCards: array
      itemType: string
      description: "Health card numbers"

  - credentials: array
      itemType: object
      schema: { cpf: string, username: string, temporaryPassword: string }
      description: "Digital platform access credentials"

  - welcomeKitSent: boolean

  - orientationScheduled: boolean
  - orientationDate: date
      format: ISO 8601
```

### Business Rules
```
Regulatory Compliance (ANS - Agência Nacional de Saúde Suplementar):
  1. Collect Beneficiary Data (service task)
     Source: Client HR system or manual upload
     Data validation:
       - CPF: Valid Brazilian tax ID (11 digits with checksum)
       - Birth date: Age 0-120 years
       - Relationship: titular or dependent
       - Health declaration: Required for new beneficiaries

  2. Validate Data (service task)
     Checks:
       - No duplicate CPF
       - CPF format valid
       - Birth date realistic
       - Relationship valid (1 titular per family group)

  3. Register with ANS (service task) [MANDATORY]
     Deadline: 72 hours from contract activation
     Submission format: Standard XML (ANS technical specification)
     Digital signature: ICP-Brasil certificate required
     Output: ansRegistrationId

     IF submission fails THEN
       Escalate to: Compliance officer (critical regulatory requirement)
       Retry: 3 attempts with 1-hour intervals

  4. Generate Health Cards (service task)
     Card format: Physical card + digital card
     Delivery: 5 business days
     Information: Beneficiary name, CPF, health card number, plan type, operator registration

  5. Send Credentials (send task)
     Channel: Email + SMS
     Content: Username, temporary password, portal URL, mobile app links

  6. Send Welcome Kit (send task)
     Channel: Email (digital welcome kit)
     Content: Plan details, provider network guide, coverage information, FAQ, support contacts

  7. Schedule Orientation Session (user task)
     Duration: 1 hour
     Attendees: Company HR + beneficiaries
     Deadline: Within 7 days of onboarding start

ANS Compliance Checkpoint:
  IF ansRegistrationId == null OR ansRegistrationId == "" THEN
    THROW Error_ANSRegistrationFailed
    Process status: BLOCKED (cannot proceed without ANS registration)
```

### Error Handling
- **Error Code**: `ONBD_ERROR`
- **Thrown When**: ANS submission failure, invalid beneficiary data, health card generation error
- **Recovery**: Manual ANS submission (compliance officer), data correction and re-validation, regenerate health cards

### Compensation
- **Compensation Task**: `Task_CompensateOnboarding`
- **Actions**: Cancel ANS registration (if possible), invalidate health cards, revoke digital credentials, notify client of cancellation
- **Duration**: PT15M (15 minutes)

---

## SUBPROCESS 10: DIGITAL SERVICES ACTIVATION

### Basic Information
- **Process ID**: `Process_Digital_Services_Activation_V3`
- **CallActivity ID in Main**: `CallActivity_DigitalServicesActivation`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Sequential digital enablement
- **Error Code**: `DIGI_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - beneficiaryList: array
  - companyName: string
  - planType: string

Optional:
  - digitalServicePreferences: object
      schema: { mobileApp: boolean, webPortal: boolean, telemedicine: boolean, chatSupport: boolean }
```

### Output Variables
```yaml
Always Returned:
  - userAccountsCreated: integer
      description: "Number of digital accounts created"

  - mobileAppConfigured: boolean
  - webPortalActivated: boolean
  - telemedicineSetup: boolean
  - chatSupportEnabled: boolean
  - digitalCredentialsSent: boolean
  - activationCompleted: boolean

Conditional (if completed):
  - appDownloadLinks: object
      schema: { ios: string, android: string }

  - portalUrl: string
      format: URL

  - telemedicineScheduleUrl: string
      format: URL

  - chatSupportUrl: string
      format: URL or WhatsApp number
```

### Business Rules
```
Digital Services Activation Flow:
  1. Create User Accounts (service task)
     Accounts: One per beneficiary (from beneficiaryList)
     Identity provider: OAuth2 (Keycloak/Auth0)
     Output: userAccountsCreated count

  2. Configure Mobile App (service task)
     Platforms: iOS + Android
     Configuration:
       - Branding: Company logo, color scheme
       - Features: Health card, provider search, telehealth, appointment scheduling
       - Push notifications: Enabled
     Output: appDownloadLinks

  3. Activate Web Portal (service task)
     URL: https://portal.austa.com.br/{companyName}
     Features: Full self-service (same as mobile app + admin functions)
     SSO: Integration with company Active Directory (optional)
     Output: portalUrl

  4. Setup Telemedicine (service task)
     Provider: Internal or third-party (e.g., Teladoc, Doctor On Demand)
     Availability: 24/7 (basic) or business hours (extended)
     Specialties: General practitioner, pediatrics, mental health
     Output: telemedicineScheduleUrl

  5. Enable Chat Support (service task)
     Channels: Web chat, WhatsApp Business API, Facebook Messenger
     Availability: Business hours (8AM-8PM) or extended
     Bot: AI chatbot for common questions + human escalation
     Output: chatSupportUrl

  6. Send Digital Credentials (send task)
     Channel: Email + SMS
     Content: Portal URL, username, temporary password, app download links, telemedicine info, support contacts
     Personalization: Each beneficiary receives their own credentials

  7. Train Users (user task)
     Role: Digital coach (customer success team)
     Format: Live webinar + recorded tutorials
     Duration: 1 hour
     Attendees: Company HR + key users
     Materials: User guides, FAQ, troubleshooting tips

Digital Service Tiers (based on planType):
  IF planType == 'ambulatorial' THEN
    Telemedicine: Basic (GP only)
    Chat Support: Business hours

  ELIF planType == 'hospitalar' THEN
    Telemedicine: Extended (GP + pediatrics)
    Chat Support: Extended hours

  ELSE (planType == 'referencia') THEN
    Telemedicine: Premium (GP + pediatrics + mental health + specialists)
    Chat Support: 24/7
```

### Error Handling
- **Error Code**: `DIGI_ERROR`
- **Thrown When**: Account creation failure, app configuration error, portal activation error, telemedicine provider unavailable
- **Recovery**: Retry account creation, manual app configuration, escalate to IT team, use backup telemedicine provider

### Compensation
- **Compensation Task**: `Task_CompensateDigitalServices`
- **Actions**: Disable user accounts, deactivate portal, cancel telemedicine access, disable chat support, revoke credentials
- **Duration**: PT10M (10 minutes)

---

## SUBPROCESS 11: POST-LAUNCH SETUP (NEW IN V3)

### Basic Information
- **Process ID**: `Process_Post_Launch_Setup_V3`
- **CallActivity ID in Main**: `CallActivity_PostLaunchSetup`
- **Duration SLA**: PT7D (7 days)
- **Pattern**: Parallel setup with monitoring preparation
- **Error Code**: `SETUP_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - projectId: string
  - beneficiaryCount: integer
  - planType: string

Optional:
  - customKpis: array
      itemType: string
      description: "Client-specific KPIs to track"
```

### Output Variables
```yaml
Always Returned:
  - monitoringDashboardSetup: boolean
  - kpiBaselineEstablished: boolean
  - escalationPathsDefined: boolean
  - supportTicketSystemConfigured: boolean
  - setupCompleted: boolean

Conditional (if completed):
  - dashboardUrl: string
      format: URL
      description: "Real-time monitoring dashboard URL"

  - kpiBaselines: object
      schema: { utilizationRate: number, npsScore: number, claimsRatio: number, ticketVolume: number }
      description: "Baseline KPI values (30-day average)"

  - escalationPaths: array
      itemType: object
      schema: { level: integer, role: string, responseTime: string }
      description: "3-level escalation matrix"

  - ticketSystemIntegrated: boolean
```

### Business Rules
```
Post-Launch Setup Flow (V3 Innovation):

  Purpose: Prepare monitoring infrastructure before go-live

  1. Setup Monitoring Dashboard (service task)
     Platform: Grafana or custom dashboard
     Data sources: CRM, ERP, digital services platform, ANS portal
     Metrics tracked:
       - Utilization rate (%)
       - NPS score (-100 to +100)
       - Claims ratio (actual vs. expected)
       - Support ticket volume
       - Digital service adoption (%)
       - Time-to-resolution (hours)
     Refresh frequency: Real-time
     Access: Account manager + customer success team + client admin
     Output: dashboardUrl

  2. Establish KPI Baselines (service task)
     Method: 30-day rolling average
     Initial values (industry benchmarks):
       - Utilization rate: 75% (target)
       - NPS score: 50 (target: 70)
       - Claims ratio: 80% (target: <85%)
       - Ticket volume: 0.5 tickets per beneficiary per month (target: <0.3)
     Output: kpiBaselines object

  3. Define Escalation Paths (user task)
     3-Level Escalation Matrix:
       Level 1: Support team (response: 4 hours)
       Level 2: Account manager (response: 24 hours)
       Level 3: Executive sponsor (response: 48 hours)

     Escalation triggers:
       - Critical issue (system down, data breach)
       - SLA violation
       - Client escalation request
       - Multiple unresolved tickets

     Output: escalationPaths array

  4. Configure Ticket System (service task)
     Integration: Client's existing system OR AUSTA's system
     Ticket types: Technical, billing, coverage, claims, general inquiry
     Auto-routing: Based on ticket type
     SLA tracking: Enabled (response time + resolution time)
     Output: ticketSystemIntegrated boolean

  5. Conduct Readiness Review (user task)
     Review checklist:
       - Dashboard accessible and populated
       - KPI baselines established
       - Escalation paths communicated
       - Ticket system tested
       - Monitoring team trained

     IF all checks pass THEN
       setupCompleted = true
     ELSE
       Route to: Issue resolution → Re-review

V3 Innovation Rationale:
  Problem: In V1/V2, monitoring starts AFTER go-live (reactive)
  Solution: V3 sets up monitoring BEFORE go-live (proactive)
  Benefit: Issues detected on Day 1 instead of Day 30
```

### Error Handling
- **Error Code**: `SETUP_ERROR`
- **Thrown When**: Dashboard setup failure, KPI baseline calculation error, ticket system integration failure
- **Recovery**: Manual dashboard setup, use industry benchmarks, escalate to IT support team

### Compensation
- **Compensation Task**: `Task_CompensateSetup`
- **Actions**: Deactivate dashboard, delete KPI baselines, remove escalation paths, disable ticket system integration
- **Duration**: PT5M (5 minutes)

---

## SUBPROCESS 12: POST-LAUNCH MONITORING

### Basic Information
- **Process ID**: `Process_Post_Launch_Monitoring_V3`
- **CallActivity ID in Main**: `CallActivity_PostLaunchMonitoring`
- **Duration SLA**: PT90D (90 days)
- **Pattern**: Timer-based check-ins with KPI tracking
- **Error Code**: `MONI_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - beneficiaryCount: integer
  - kpiBaselines: object
  - dashboardUrl: string

Optional:
  - monitoringFrequency: string
      enum: [weekly, biweekly, monthly]
      default: biweekly
```

### Output Variables
```yaml
Always Returned:
  - day7CheckCompleted: boolean
  - day30ReviewCompleted: boolean
  - day90AssessmentCompleted: boolean
  - monitoringCompleted: boolean

Conditional (if completed):
  - kpiAnalysis: object
      description: "DMN decision output with KPI trends"
      schema: { utilizationRate: number, npsScore: number, claimsRatio: number, ticketVolume: number, digitalAdoption: number }

  - accountHealth: integer
      range: [0, 100]
      description: "Composite account health score"
      calculation: "Weighted average of KPIs"

  - npsScore: integer
      range: [-100, 100]
      description: "Net Promoter Score (Day 90)"

  - issuesIdentified: array
      itemType: object
      schema: { issue: string, severity: enum[low, medium, high, critical], status: enum[open, resolved] }

  - improvementActions: array
      itemType: string
      description: "Recommended improvement actions"
```

### Business Rules
```
Monitoring Schedule (90-Day Cycle):

  Day 7 Check-In:
    1. Wait 7 days (timer event: PT7D)
    2. Day 7 check-in (user task)
       Assigned to: Customer success manager
       Focus: Initial adoption, early issues
       Questions:
         - Are beneficiaries accessing the portal/app?
         - Any registration issues?
         - Support tickets volume acceptable?
       Duration: 30 minutes
       Output: day7CheckCompleted = true

  Day 30 Review:
    3. Wait 23 more days (timer event: PT23D)
    4. Day 30 review (user task)
       Assigned to: Account manager
       Focus: Usage trends, satisfaction
       Questions:
         - Utilization rate on track?
         - Digital service adoption improving?
         - Any patterns in support tickets?
         - Client feedback?
       Duration: 1 hour
       Output: day30ReviewCompleted = true

  Day 90 Assessment:
    5. Wait 60 more days (timer event: PT60D)
    6. Day 90 assessment (user task)
       Assigned to: Account manager + customer success
       Focus: Comprehensive review, NPS collection
       Activities:
         - Analyze 90-day KPI trends
         - Collect NPS score (survey)
         - Identify improvement opportunities
         - Assess expansion readiness
       Duration: 2 hours
       Output: day90AssessmentCompleted = true, npsScore captured

  7. Analyze KPIs (business rule task - DMN)
     DMN decision table: decision_kpi_analysis
     Inputs: 90-day KPI data
     Outputs: kpiAnalysis object, accountHealth score, recommendations

  8. Account Health Assessment (user task)
     Final evaluation:
       Account Health Score = Weighted Average:
         - Utilization rate (30%): 0-100
         - NPS score (25%): -100 to +100 (normalized to 0-100)
         - Claims ratio (20%): Inverse scale (lower is better)
         - Ticket volume (15%): Inverse scale (lower is better)
         - Digital adoption (10%): 0-100

       IF accountHealth >= 80 THEN
         Status: Healthy → Ready for expansion
       ELIF accountHealth >= 60 AND accountHealth < 80 THEN
         Status: Stable → Monitor closely
       ELIF accountHealth >= 40 AND accountHealth < 60 THEN
         Status: At risk → Intervention required
       ELSE (accountHealth < 40) THEN
         Status: Critical → Escalate to executive

     Output: monitoringCompleted = true

KPIs Tracked:
  - Utilization Rate: % of beneficiaries using services
  - NPS Score: Net Promoter Score (-100 to +100)
  - Claims Ratio: Actual claims / Expected claims (%)
  - Ticket Volume: Tickets per beneficiary per month
  - Digital Adoption: % of beneficiaries using portal/app
  - Time-to-Resolution: Average hours to resolve tickets
```

### Error Handling
- **Error Code**: `MONI_ERROR`
- **Thrown When**: KPI data unavailable, DMN decision failure, NPS survey failure
- **Recovery**: Use cached KPI data, manual KPI analysis, reschedule NPS survey

### Compensation
- **Compensation Task**: `Task_CompensateMonitoring`
- **Actions**: Stop monitoring, archive monitoring data, update account status to "monitoring cancelled"
- **Duration**: PT2M (2 minutes)

---

## SUBPROCESS 13: CONTRACT EXPANSION

### Basic Information
- **Process ID**: `Process_Contract_Expansion_V3`
- **CallActivity ID in Main**: `CallActivity_ContractExpansion`
- **Duration SLA**: PT30D (30 days per cycle) - Continuous loop
- **Pattern**: Opportunity identification with recursive closing
- **Error Code**: `EXP_ERROR`

### Input Variables
```yaml
Required:
  - contractId: string
  - accountHealth: integer
  - npsScore: integer
  - beneficiaryCount: integer
  - currentValue: long
      unit: BRL
      description: "Current annual contract value"

Optional:
  - kpiAnalysis: object
  - companyGrowthIndicators: object
      schema: { headcountGrowth: number, newLocations: array, budgetIncrease: number }
```

### Output Variables
```yaml
Always Returned:
  - expansionScore: integer
      range: [0, 100]
      description: "Expansion opportunity score (DMN decision)"

  - expansionCompleted: boolean

Conditional (if score >= 70):
  - expansionOpportunities: array
      itemType: object
      schema: { type: enum[upsell, cross_sell, headcount_growth], description: string, estimatedValue: long }
      description: "Identified expansion opportunities"

  - clientAccepted: boolean
      description: "Client accepted expansion proposal?"

Conditional (if accepted):
  - newContractValue: long
      unit: BRL
      description: "New annual contract value after expansion"

  - expansionValue: long
      unit: BRL
      description: "Incremental revenue from expansion"
      calculation: "newContractValue - currentValue"
```

### Business Rules
```
Expansion Opportunity Identification:

  1. Identify Opportunities (business rule task - DMN)
     DMN decision table: decision_expansion_opportunities

     Inputs (weighted):
       - Account health score (30%)
       - Growth indicators (25%): Headcount growth, new locations, budget increase
       - Product utilization (20%): Services used, telemedicine adoption, portal engagement
       - Relationship strength (15%): Meeting frequency, satisfaction scores, executive alignment
       - Market conditions (10%): Industry growth, regulatory changes, competitor moves

     Outputs:
       - expansionScore: 0-100
       - expansionOpportunities: array

     Opportunity types:
       - Upsell: Upgrade plan (ambulatorial → hospitalar → referencia)
       - Cross-sell: Add services (dental, vision, life insurance, disability)
       - Headcount growth: Onboard new employees/locations
       - Network expansion: Add new geographic regions

  2. Expansion Potential Gateway
     IF expansionScore >= 70 THEN
       Route to: Prepare expansion proposal

     ELSE (expansionScore < 70) THEN
       Route to: Schedule next review (quarterly)
       Note: No expansion opportunity at this time

  3. Prepare Expansion Proposal (user task)
     Assigned to: Account manager
     Inputs: expansionOpportunities array
     Create: Expansion proposal document
     Content:
       - Current contract summary
       - Identified opportunities
       - Value proposition
       - Pricing
       - Timeline
     Duration: PT5D (5 days)

  4. Present to Client (user task)
     Meeting format: Executive presentation
     Attendees: Account manager + client executives
     Duration: 1 hour
     Output: Client response (accepted|negotiate|declined)

  5. Client Acceptance Gateway
     IF response == 'accepted' THEN
       Route to: Process expansion

     ELIF response == 'negotiate' THEN
       Route to: CallActivity to Negotiation_Closing subprocess (recursive)
       Note: Expansion is treated as a mini sales cycle

     ELSE (response == 'declined') THEN
       Route to: Schedule next review (quarterly)
       Note: Capture declination reason for future reference

  6. Process Expansion (CallActivity - RECURSIVE)
     Called subprocess: Process_Negotiation_Closing_V3

     Passes to negotiation:
       - proposalId: expansionProposal ID
       - totalValue: expansionValue
       - proposalDocumentUrl: expansion proposal URL
       - companyName: existing
       - contactName: existing

     Returns from negotiation:
       - negotiationStatus: accepted|rejected
       - finalDiscount: integer
       - paymentTerms: string
       - contractDuration: integer

     IF negotiationStatus == 'accepted' THEN
       Trigger: Closing subprocess for contract amendment
       Result: New contract value = currentValue + expansionValue

  7. Update Account Value (service task)
     Update CRM:
       - contractValue = newContractValue
       - lastExpansionDate = today
       - expansionHistory.append({ date: today, value: expansionValue, type: opportunityType })

     Output: expansionCompleted = true

  8. Schedule Next Review (user task)
     Frequency: Quarterly (PT90D)
     Purpose: Continuous expansion opportunity monitoring
     Calendar event: Created for account manager

Recursive Pattern:
  Contract Expansion → Negotiation → Closing → Contract Amendment

  Key insight: Expansion is a mini sales cycle (negotiation + closing)
  Benefit: Reuses existing negotiation and closing logic

Expansion Score Thresholds:
  90-100: Immediate expansion (hot opportunity)
  70-89: Strong expansion potential
  50-69: Moderate opportunity (nurture)
  <50: No expansion opportunity (focus on retention)
```

### Error Handling
- **Error Code**: `EXP_ERROR`
- **Thrown When**: Expansion score calculation fails, client unresponsive, negotiation deadlock, contract amendment error
- **Recovery**: Manual score calculation, follow-up with client, escalate to executive, manual contract update

### Compensation
- **Compensation Task**: `Task_CompensateExpansion`
- **Actions**: Revoke expansion terms, revert contract value, update CRM to "expansion cancelled", notify client of cancellation
- **Duration**: PT5M (5 minutes)

---

## SUMMARY: SUBPROCESS I/O DEPENDENCY GRAPH

```
Subprocess Flow with Variable Dependencies:

1. Qualification
   Inputs: Lead form data (7 variables)
   Outputs: scoreMEDDIC, isQualified

   ↓ (if qualified)

2. Discovery
   Inputs: scoreMEDDIC, company data
   Outputs: opportunityId, opportunityValue, painPoints, budget

   ↓

3. Proposal
   Inputs: opportunityId, opportunityValue, painPoints, budget
   Outputs: proposalId, totalValue, discount, proposalDocumentUrl

   ↓

4. Approval
   Inputs: proposalId, totalValue, discount
   Outputs: proposalStatus (approved/rejected/revision)

   ↓ (if approved)

5. Negotiation
   Inputs: proposalId, totalValue, proposalDocumentUrl
   Outputs: finalDiscount, paymentTerms, contractDuration

   ↓

6. Closing
   Inputs: proposalId, finalDiscount, paymentTerms, totalValue
   Outputs: contractId, contractSigned, finalValue

   ↓ (if signed)

7. Planning
   Inputs: contractId, companySize, planType, startDate
   Outputs: projectId, implementationTeam, projectTimeline

   ↓

8. Execution
   Inputs: projectId, projectTimeline, implementationTeam
   Outputs: integrationCompleted, uatCompleted, projectSignoff

   ↓ (parallel fork)

9. Onboarding (Parallel Path A)
   Inputs: contractId, companySize, startDate
   Outputs: beneficiaryList, ansRegistrationId, healthCards

10. Digital Services (Parallel Path B)
    Inputs: contractId, beneficiaryList
    Outputs: userAccountsCreated, appConfigured, portalActivated

11. Post-Launch Setup (Parallel Path C)
    Inputs: contractId, beneficiaryCount
    Outputs: dashboardUrl, kpiBaselines, escalationPaths

    ↓ (parallel join)

12. Monitoring
    Inputs: contractId, beneficiaryCount, kpiBaselines
    Outputs: accountHealth, npsScore, kpiAnalysis

    ↓

13. Expansion
    Inputs: contractId, accountHealth, npsScore, currentValue
    Outputs: expansionScore, newContractValue, expansionValue

    ↓ (if expansion accepted - recursive)
    Loops back to: Negotiation → Closing (contract amendment)
```

---

## VARIABLE NAMING STANDARDS (V3 Convention)

### Format
`{entity}_{attribute}_{phase?}`

### Entity Prefixes
- `lead_`: Lead-related data
- `opportunity_`: CRM opportunity data
- `proposal_`: Proposal documents and terms
- `contract_`: Contract details
- `project_`: Implementation project data
- `beneficiary_`: Beneficiary registration data
- `expansion_`: Expansion opportunity data

### Attribute Suffixes
- `_ID`: Unique identifier
- `_Value`: Monetary value (BRL)
- `_Status`: Current status (enum)
- `_Completed`: Boolean completion flag
- `_Score`: Numeric score (0-10 or 0-100)
- `_Url`: Web URL or document link
- `_Date`: ISO 8601 date/datetime

### Phase Suffixes (optional, for phase-specific variables)
- `_QUAL`: Qualification phase
- `_DISC`: Discovery phase
- `_PROP`: Proposal phase
- `_APPR`: Approval phase
- `_NEG`: Negotiation phase
- `_CLOS`: Closing phase
- `_PLAN`: Planning phase
- `_EXEC`: Execution phase
- `_ONBD`: Onboarding phase
- `_DIGI`: Digital services phase
- `_SETUP`: Post-launch setup phase
- `_MONI`: Monitoring phase
- `_EXP`: Expansion phase

### Examples
- `scoreMEDDIC`: Global MEDDIC score (no phase suffix - updated across phases)
- `opportunityId_CRM`: CRM system opportunity ID
- `proposalDocumentUrl_PROP`: Proposal PDF URL (phase-specific)
- `contractSigned_CLOS`: Contract signature status (phase-specific boolean)
- `accountHealth_MONI`: Account health score from monitoring (phase-specific)

---

## VALIDATION & TESTING

### Contract Validation Checklist

For each subprocess:
- [ ] All required input variables defined with types
- [ ] All output variables defined with types
- [ ] Business rules documented
- [ ] Error codes defined
- [ ] Compensation actions specified
- [ ] Dependencies on previous subprocess outputs identified
- [ ] Variable naming follows V3 convention
- [ ] Duration SLA specified

### Integration Testing Strategy

**Unit Test** (per subprocess):
1. Mock input variables
2. Execute subprocess in isolation
3. Verify output variables
4. Check error handling
5. Test compensation logic

**Integration Test** (end-to-end):
1. Execute main orchestrator with sample lead data
2. Verify variable propagation across subprocesses
3. Check gateway decisions
4. Validate SLA timer firing
5. Test error boundary triggering
6. Verify compensation chain (if error occurs)

### Sample Test Data

```yaml
Happy Path Test:
  Lead:
    - companyName: "Acme Corp"
    - companySize: 250
    - industry: "Technology"
    - urgency: "high"

  Expected Outputs:
    - scoreMEDDIC: 8 (high qualification)
    - opportunityValue: 750000 (BRL)
    - proposalStatus: "approved"
    - contractSigned: true
    - accountHealth: 85 (healthy)
    - expansionScore: 75 (expansion ready)

Error Path Test:
  Lead:
    - companyName: "Small Co"
    - companySize: 5
    - industry: "Retail"
    - urgency: "low"

  Expected Outputs:
    - scoreMEDDIC: 3 (disqualified)
    - isQualified: false
    - Process terminates at: Qualification end event
```

---

**Document Version**: 1.0.0
**Last Updated**: 2025-12-08
**Status**: APPROVED - Implementation Ready
**Next Review**: Weekly during implementation phase

---

*This contract specification will be updated as subprocesses are implemented and refined.*
