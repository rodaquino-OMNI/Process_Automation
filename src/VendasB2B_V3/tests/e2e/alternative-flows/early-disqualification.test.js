const { Client, Variables } = require('camunda-external-task-client-js');

describe('E2E Test - Early Disqualification Flow', () => {
  let client;

  beforeAll(() => {
    client = new Client({
      baseUrl: process.env.CAMUNDA_BASE_URL || 'http://localhost:8080/engine-rest'
    });
  });

  afterAll(async () => {
    if (client) {
      await client.stop();
    }
  });

  it('should disqualify lead with MEDDIC score < 4', async () => {
    const variables = new Variables();
    variables.set('leadId', 'LEAD-DQ-001');
    variables.set('companyName', 'Disqualified Corp');
    variables.set('companySize', 50);
    // Provide poor MEDDIC data
    variables.set('metrics', { defined: false, quantified: false, agreement: false });
    variables.set('economicBuyer', { identified: false, accessible: false, engaged: false });
    variables.set('decisionCriteria', { documented: false, aligned: false });
    variables.set('decisionProcess', { mapped: false, timeline: false });
    variables.set('identifyPain', { critical: false, quantified: false });
    variables.set('champion', { identified: false, influential: false, committed: false });

    const result = await client.startProcessInstance({
      processDefinitionKey: 'AUSTA_B2B_Sales_V3',
      businessKey: 'E2E-DQ-001',
      variables
    });

    await new Promise(resolve => setTimeout(resolve, 10000));

    const finalVars = await client.getProcessVariables(result.id);

    expect(finalVars.meddicScore).toBeLessThan(4);
    expect(finalVars.processStatus).toBe('disqualified');
    expect(finalVars.disqualificationReason).toBeDefined();
    expect(finalVars.engagementStarted).toBeUndefined();
    expect(finalVars.notificationSent).toBe(true);
  });

  it('should provide disqualification reason and next steps', async () => {
    const variables = new Variables();
    variables.set('leadId', 'LEAD-DQ-002');
    variables.set('companyName', 'Poor Fit Corp');
    variables.set('meddicScore', 2);

    const result = await client.startProcessInstance({
      processDefinitionKey', 'AUSTA_B2B_Sales_V3',
      businessKey: 'E2E-DQ-002',
      variables
    });

    await new Promise(resolve => setTimeout(resolve, 5000));

    const finalVars = await client.getProcessVariables(result.id);

    expect(finalVars.disqualificationReason).toContain('Low MEDDIC score');
    expect(finalVars.recommendedActions).toBeDefined();
    expect(finalVars.recommendedActions).toContain('Re-evaluate in 6 months');
  });

  it('should track disqualification for analytics', async () => {
    const variables = new Variables();
    variables.set('leadId', 'LEAD-DQ-003');
    variables.set('leadSource', 'Website');
    variables.set('meddicScore', 3);

    const result = await client.startProcessInstance({
      processDefinitionKey: 'AUSTA_B2B_Sales_V3',
      businessKey: 'E2E-DQ-003',
      variables
    });

    await new Promise(resolve => setTimeout(resolve, 5000));

    const finalVars = await client.getProcessVariables(result.id);

    expect(finalVars.analyticsTracked).toBe(true);
    expect(finalVars.disqualificationStage).toBe('qualification');
    expect(finalVars.leadSource).toBe('Website');
  });
});
