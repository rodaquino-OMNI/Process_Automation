const { Client, Variables } = require('camunda-external-task-client-js');

describe('Full Lifecycle E2E Test - Happy Path', () => {
  let client;
  let processInstanceId;
  const testData = {
    leadId: 'LEAD-E2E-HP-001',
    companyName: 'E2E Test Corporation',
    cnpj: '12.345.678/0001-90',
    companySize: 300,
    industry: 'Technology',
    revenue: 10000000
  };

  beforeAll(() => {
    client = new Client({
      baseUrl: process.env.CAMUNDA_BASE_URL || 'http://localhost:8080/engine-rest',
      asyncResponseTimeout: 120000 // 2 minutes
    });
  });

  afterAll(async () => {
    if (client) {
      await client.stop();
    }
  });

  it('should complete full sales lifecycle from lead to post-launch (90 days)', async () => {
    const milestones = [];

    // 1. LEAD QUALIFICATION PHASE (Days 1-3)
    console.log('Starting Lead Qualification Phase...');
    const variables = new Variables();
    Object.entries(testData).forEach(([key, value]) => {
      variables.set(key, value);
    });

    const result = await client.startProcessInstance({
      processDefinitionKey: 'AUSTA_B2B_Sales_V3',
      businessKey: `E2E-HP-${Date.now()}`,
      variables
    });

    processInstanceId = result.id;
    milestones.push({ phase: 'Process Started', timestamp: Date.now() });

    // Wait for CRM enrichment
    await waitForVariable(client, processInstanceId, 'crmDataEnriched', 5000);
    milestones.push({ phase: 'CRM Data Enriched', timestamp: Date.now() });

    // Wait for MEDDIC scoring
    await waitForVariable(client, processInstanceId, 'meddicScore', 5000);
    const vars1 = await client.getProcessVariables(processInstanceId);
    expect(vars1.meddicScore).toBeGreaterThanOrEqual(4);
    milestones.push({ phase: 'MEDDIC Scored', timestamp: Date.now() });

    console.log('Qualification complete. Score:', vars1.meddicScore);

    // 2. STAKEHOLDER ENGAGEMENT PHASE (Days 4-20)
    console.log('Starting Stakeholder Engagement Phase...');

    await waitForVariable(client, processInstanceId, 'engagementStarted', 10000);
    milestones.push({ phase: 'Engagement Started', timestamp: Date.now() });

    // Simulate stakeholder meetings
    const stakeholders = ['CEO', 'CFO', 'COO', 'CHRO'];
    for (const stakeholder of stakeholders) {
      await completeUserTask(client, processInstanceId, `engage_${stakeholder.toLowerCase()}`, {
        [`${stakeholder}_engaged`]: true,
        [`${stakeholder}_interest_level`]: 'high'
      });
    }

    milestones.push({ phase: 'All Stakeholders Engaged', timestamp: Date.now() });
    console.log('Stakeholder engagement complete.');

    // 3. VALUE DEMONSTRATION PHASE (Days 21-35)
    console.log('Starting Value Demonstration Phase...');

    await waitForVariable(client, processInstanceId, 'valueDemoScheduled', 10000);

    // Complete value demonstration
    await completeUserTask(client, processInstanceId, 'conduct_value_demo', {
      demoCompleted: true,
      clientFeedback: 'Very positive',
      roiPresented: true,
      pilotRequested: false // Direct to proposal
    });

    milestones.push({ phase: 'Value Demo Completed', timestamp: Date.now() });
    console.log('Value demonstration complete.');

    // 4. PROPOSAL & NEGOTIATION PHASE (Days 36-55)
    console.log('Starting Proposal & Negotiation Phase...');

    await waitForVariable(client, processInstanceId, 'proposalGenerated', 15000);
    milestones.push({ phase: 'Proposal Generated', timestamp: Date.now() });

    // Complete negotiation
    await completeUserTask(client, processInstanceId, 'negotiate_terms', {
      negotiationComplete: true,
      finalDiscount: 5,
      termsAgreed: true
    });

    milestones.push({ phase: 'Negotiation Completed', timestamp: Date.now() });
    console.log('Negotiation complete.');

    // 5. CONTRACT & CLOSING PHASE (Days 56-65)
    console.log('Starting Contract & Closing Phase...');

    await waitForVariable(client, processInstanceId, 'contractGenerated', 10000);
    milestones.push({ phase: 'Contract Generated', timestamp: Date.now() });

    // Sign contract
    await waitForVariable(client, processInstanceId, 'contractSentForSignature', 5000);

    await completeUserTask(client, processInstanceId, 'contract_signature', {
      contractSigned: true,
      signatureDate: new Date().toISOString()
    });

    milestones.push({ phase: 'Contract Signed', timestamp: Date.now() });
    console.log('Contract signed.');

    // 6. ANS REGISTRATION PHASE (Days 66-70)
    console.log('Starting ANS Registration Phase...');

    await waitForVariable(client, processInstanceId, 'ansRegistrationSubmitted', 10000);
    milestones.push({ phase: 'ANS Registration Submitted', timestamp: Date.now() });

    await waitForVariable(client, processInstanceId, 'ansRegistrationApproved', 10000);
    milestones.push({ phase: 'ANS Registration Approved', timestamp: Date.now() });
    console.log('ANS registration approved.');

    // 7. IMPLEMENTATION PHASE (Days 71-85)
    console.log('Starting Implementation Phase...');

    await waitForVariable(client, processInstanceId, 'implementationStarted', 5000);
    milestones.push({ phase: 'Implementation Started', timestamp: Date.now() });

    // Complete implementation tasks
    await completeUserTask(client, processInstanceId, 'onboard_beneficiaries', {
      beneficiariesOnboarded: 300,
      onboardingComplete: true
    });

    await completeUserTask(client, processInstanceId, 'activate_digital_services', {
      digitalServicesActive: true,
      cardsIssued: true
    });

    milestones.push({ phase: 'Implementation Completed', timestamp: Date.now() });
    console.log('Implementation complete.');

    // 8. POST-LAUNCH MONITORING PHASE (Days 86-90)
    console.log('Starting Post-Launch Monitoring Phase...');

    await waitForVariable(client, processInstanceId, 'goLiveDate', 5000);
    milestones.push({ phase: 'Go Live', timestamp: Date.now() });

    // Wait for initial monitoring
    await new Promise(resolve => setTimeout(resolve, 5000));

    const finalVars = await client.getProcessVariables(processInstanceId);

    milestones.push({ phase: 'Process Completed', timestamp: Date.now() });

    // ASSERTIONS
    expect(finalVars.processStatus).toBe('completed');
    expect(finalVars.contractSigned).toBe(true);
    expect(finalVars.ansRegistrationApproved).toBe(true);
    expect(finalVars.goLiveDate).toBeDefined();

    // Log milestones
    console.log('\n=== E2E Test Milestones ===');
    milestones.forEach((milestone, index) => {
      const duration = index > 0
        ? `+${((milestone.timestamp - milestones[index-1].timestamp) / 1000).toFixed(2)}s`
        : 'Start';
      console.log(`${milestone.phase}: ${duration}`);
    });

    const totalDuration = (milestones[milestones.length - 1].timestamp - milestones[0].timestamp) / 1000;
    console.log(`\nTotal E2E Duration: ${totalDuration.toFixed(2)} seconds`);

    // Verify all phases completed
    expect(milestones.map(m => m.phase)).toEqual(
      expect.arrayContaining([
        'Process Started',
        'CRM Data Enriched',
        'MEDDIC Scored',
        'Engagement Started',
        'All Stakeholders Engaged',
        'Value Demo Completed',
        'Proposal Generated',
        'Negotiation Completed',
        'Contract Generated',
        'Contract Signed',
        'ANS Registration Submitted',
        'ANS Registration Approved',
        'Implementation Started',
        'Implementation Completed',
        'Go Live',
        'Process Completed'
      ])
    );
  }, 300000); // 5 minute timeout for full E2E

  // Helper functions
  async function waitForVariable(client, processInstanceId, variableName, timeout = 30000) {
    const startTime = Date.now();
    while (Date.now() - startTime < timeout) {
      const vars = await client.getProcessVariables(processInstanceId);
      if (vars[variableName] !== undefined && vars[variableName] !== null) {
        return vars[variableName];
      }
      await new Promise(resolve => setTimeout(resolve, 1000));
    }
    throw new Error(`Timeout waiting for variable: ${variableName}`);
  }

  async function completeUserTask(client, processInstanceId, taskKey, variables) {
    const tasks = await client.getTaskList({ processInstanceId });
    const task = tasks.find(t => t.taskDefinitionKey === taskKey);

    if (!task) {
      throw new Error(`Task not found: ${taskKey}`);
    }

    await client.completeTask(task.id, new Variables(variables));
  }
});
