const { Client, Variables } = require('camunda-external-task-client-js');

describe('Parallel Stakeholder Engagement Integration', () => {
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

  describe('Parallel Execution', () => {
    it('should execute 4 stakeholder paths in parallel', async () => {
      const executionLog = [];

      const stakeholderRoles = ['CEO', 'CFO', 'COO', 'CHRO'];

      stakeholderRoles.forEach(role => {
        client.subscribe(`engage_${role.toLowerCase()}`, async ({ task, taskService }) => {
          executionLog.push({
            role,
            startTime: Date.now(),
            threadId: task.id
          });

          await new Promise(resolve => setTimeout(resolve, 2000)); // Simulate work

          executionLog[executionLog.length - 1].endTime = Date.now();

          await taskService.complete(task, new Variables().set(`${role}_engaged`, true));
        });
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-001');
      variables.set('stakeholders', stakeholderRoles.map(role => ({ role, name: `${role} Name` })));

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-001',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      expect(executionLog).toHaveLength(4);

      // Verify parallel execution (all started within 1 second of each other)
      const startTimes = executionLog.map(log => log.startTime);
      const maxStartDiff = Math.max(...startTimes) - Math.min(...startTimes);
      expect(maxStartDiff).toBeLessThan(1000);

      // Verify all completed
      expect(executionLog.every(log => log.endTime)).toBe(true);
    });

    it('should synchronize all parallel paths before proceeding', async () => {
      let synchronizationPoint = null;

      client.subscribe('engagement_complete', async ({ task, taskService }) => {
        synchronizationPoint = Date.now();
        await taskService.complete(task);
      });

      const stakeholderCompletionTimes = [];

      ['CEO', 'CFO', 'COO', 'CHRO'].forEach((role, index) => {
        client.subscribe(`engage_${role.toLowerCase()}`, async ({ task, taskService }) => {
          // Simulate different completion times
          await new Promise(resolve => setTimeout(resolve, (index + 1) * 1000));

          stakeholderCompletionTimes.push(Date.now());

          await taskService.complete(task, new Variables().set(`${role}_engaged`, true));
        });
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-002');
      variables.set('stakeholders', ['CEO', 'CFO', 'COO', 'CHRO'].map(role => ({ role })));

      await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-002',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 10000));

      // Verify synchronization occurred after all paths completed
      const lastCompletionTime = Math.max(...stakeholderCompletionTimes);
      expect(synchronizationPoint).toBeGreaterThanOrEqual(lastCompletionTime);
    });
  });

  describe('Individual Stakeholder Engagement', () => {
    it('should track engagement status for each stakeholder', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-003');
      variables.set('stakeholders', [
        { role: 'CEO', name: 'John Doe', email: 'john@example.com' },
        { role: 'CFO', name: 'Jane Smith', email: 'jane@example.com' }
      ]);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-003',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.CEO_engaged).toBe(true);
      expect(processVariables.CFO_engaged).toBe(true);
      expect(processVariables.CEO_meetings).toBeGreaterThan(0);
      expect(processVariables.CFO_meetings).toBeGreaterThan(0);
    });

    it('should handle stakeholder engagement rejection', async () => {
      client.subscribe('engage_ceo', async ({ task, taskService }) => {
        await taskService.complete(task, new Variables()
          .set('CEO_engaged', false)
          .set('CEO_rejection_reason', 'Not interested')
        );
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-004');
      variables.set('stakeholders', [{ role: 'CEO', name: 'John Doe' }]);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-004',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.CEO_engaged).toBe(false);
      expect(processVariables.CEO_rejection_reason).toBe('Not interested');
      expect(processVariables.alternativePathTriggered).toBe(true);
    });
  });

  describe('Meeting Scheduling', () => {
    it('should schedule meetings with each stakeholder', async () => {
      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-005');
      variables.set('stakeholders', [
        { role: 'CEO', availability: ['2025-01-15', '2025-01-16'] },
        { role: 'CFO', availability: ['2025-01-15', '2025-01-17'] }
      ]);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-005',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.CEO_meeting_scheduled).toBe(true);
      expect(processVariables.CFO_meeting_scheduled).toBe(true);
      expect(processVariables.CEO_meeting_date).toBeDefined();
      expect(processVariables.CFO_meeting_date).toBeDefined();
    });
  });

  describe('Error Handling in Parallel Paths', () => {
    it('should continue with other paths if one stakeholder path fails', async () => {
      client.subscribe('engage_ceo', async ({ task, taskService }) => {
        throw new Error('CEO engagement failed');
      });

      client.subscribe('engage_cfo', async ({ task, taskService }) => {
        await taskService.complete(task, new Variables().set('CFO_engaged', true));
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-006');
      variables.set('stakeholders', [
        { role: 'CEO' },
        { role: 'CFO' }
      ]);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-006',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.CFO_engaged).toBe(true);
      expect(processVariables.CEO_error).toBeDefined();
    });

    it('should escalate if critical stakeholder engagement fails', async () => {
      client.subscribe('engage_ceo', async ({ task, taskService }) => {
        throw new Error('CEO engagement critical failure');
      });

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-007');
      variables.set('stakeholders', [{ role: 'CEO', critical: true }]);

      const result = await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-TEST-007',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 5000));

      const processVariables = await client.getProcessVariables(result.id);

      expect(processVariables.escalated).toBe(true);
      expect(processVariables.escalationReason).toContain('Critical stakeholder');
    });
  });

  describe('Performance', () => {
    it('should complete 4 parallel engagements faster than sequential', async () => {
      const parallelStart = Date.now();

      const variables = new Variables();
      variables.set('leadId', 'LEAD-PARALLEL-008');
      variables.set('stakeholders', ['CEO', 'CFO', 'COO', 'CHRO'].map(role => ({ role })));
      variables.set('executionMode', 'parallel');

      await client.startProcessInstance({
        processDefinitionKey: 'engagement_subprocess',
        businessKey: 'PARALLEL-PERF-TEST',
        variables
      });

      await new Promise(resolve => setTimeout(resolve, 10000));

      const parallelDuration = Date.now() - parallelStart;

      // Parallel should complete in ~3s (max path time), not 8s (sum of all paths)
      expect(parallelDuration).toBeLessThan(8000);
    });
  });
});
