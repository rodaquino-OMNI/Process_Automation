const { Client, Variables } = require('camunda-external-task-client-js');

describe('Qualification to Engagement Handoff Integration', () => {
  let client;
  let processInstanceId;

  beforeAll(() => {
    client = new Client({
      baseUrl: process.env.CAMUNDA_BASE_URL || 'http://localhost:8080/engine-rest',
      use: logger => console.log(logger)
    });
  });

  afterAll(async () => {
    if (client) {
      await client.stop();
    }
  });

  describe('Variable Passing', () => {
    it('should pass qualified lead data from Qualification to Engagement', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-001');
      variables.set('companyName', 'Integration Test Corp');
      variables.set('companySize', 200);
      variables.set('meddicScore', 8);
      variables.set('qualificationLevel', 'excellent');

      // Start process
      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-001',
        variables
      });

      processInstanceId = result.id;

      // Wait for qualification to complete
      await new Promise(resolve => setTimeout(resolve, 5000));

      // Verify variables are passed to engagement subprocess
      const processVariables = await client.getProcessVariables(processInstanceId);

      expect(processVariables.leadId).toBe('LEAD-INT-001');
      expect(processVariables.meddicScore).toBe(8);
      expect(processVariables.qualificationLevel).toBe('excellent');
      expect(processVariables.engagementStarted).toBe(true);
    });

    it('should pass stakeholder information to engagement', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-002');
      variables.set('decisionMakers', [
        { role: 'CEO', name: 'John Doe', email: 'john@example.com', influence: 'high' },
        { role: 'CFO', name: 'Jane Smith', email: 'jane@example.com', influence: 'high' }
      ]);
      variables.set('champion', {
        name: 'Bob Johnson',
        role: 'HR Director',
        email: 'bob@example.com'
      });

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-002',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.decisionMakers).toHaveLength(2);
      expect(processVariables.champion).toHaveProperty('role', 'HR Director');
      expect(processVariables.stakeholderMapComplete).toBe(true);
    });

    it('should handle missing optional variables gracefully', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-003');
      variables.set('companyName', 'Minimal Data Corp');
      variables.set('meddicScore', 6);
      // champion is optional

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-003',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.engagementStarted).toBe(true);
      expect(processVariables.champion).toBeUndefined();
    });
  });

  describe('Subprocess Transition', () => {
    it('should complete qualification subprocess before starting engagement', async () => {
      const executionLog = [];

      client.subscribe('qualification_complete', async ({ task, taskService }) => {
        executionLog.push({ step: 'qualification_complete', timestamp: Date.now() });
        await taskService.complete(task);
      });

      client.subscribe('engagement_start', async ({ task, taskService }) => {
        executionLog.push({ step: 'engagement_start', timestamp: Date.now() });
        await taskService.complete(task);
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-004');

      await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-004',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 10000));

      expect(executionLog).toHaveLength(2);
      expect(executionLog[0].step).toBe('qualification_complete');
      expect(executionLog[1].step).toBe('engagement_start');
      expect(executionLog[1].timestamp).toBeGreaterThan(executionLog[0].timestamp);
    });

    it('should not start engagement if qualification fails', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-005');
      variables.set('meddicScore', 2); // Below threshold
      variables.set('qualificationLevel', 'disqualified');

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-005',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.engagementStarted).toBeUndefined();
      expect(processVariables.processStatus).toBe('disqualified');
    });
  });

  describe('Data Transformation', () => {
    it('should transform qualification data for engagement use', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-006');
      variables.set('companySize', 250);
      variables.set('industry', 'Healthcare');
      variables.set('revenue', 5000000);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-006',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.companySizeSegment).toBe('medium');
      expect(processVariables.targetDecisionMakers).toContain('CEO');
      expect(processVariables.targetDecisionMakers).toContain('CFO');
    });
  });

  describe('Error Propagation', () => {
    it('should propagate qualification errors to parent process', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INVALID');

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-ERROR-001',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const incidents = await client.getIncidents(result.id);

      expect(incidents).toHaveLength(1);
      expect(incidents[0].incidentMessage).toContain('Lead not found');
    });

    it('should trigger boundary event on qualification timeout', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-INT-007');
      variables.set('simulateTimeout', true);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'AUSTA_B2B_Sales_V3',
        businessKey: 'INT-TEST-TIMEOUT',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 30000)); // Wait for timeout

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.timeoutOccurred).toBe(true);
      expect(processVariables.escalatedToManager).toBe(true);
    });
  });
});
