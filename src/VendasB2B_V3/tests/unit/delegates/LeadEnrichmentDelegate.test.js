const nock = require('nock');
const LeadEnrichmentDelegate = require('../../../src/delegates/LeadEnrichmentDelegate');

describe('LeadEnrichmentDelegate', () => {
  let delegate;
  let mockExecution;

  beforeEach(() => {
    delegate = new LeadEnrichmentDelegate();
    mockExecution = global.testUtils.createMockDelegate();
    mockExecution.getVariable = jest.fn();
    mockExecution.setVariable = jest.fn();
  });

  afterEach(() => {
    nock.cleanAll();
  });

  describe('CRM Data Fetch', () => {
    it('should successfully fetch lead data from CRM', async () => {
      const leadId = 'LEAD-001';
      const crmData = {
        companyName: 'TechCorp Inc',
        companySize: 250,
        industry: 'Technology',
        revenue: 5000000,
        employees: 250
      };

      mockExecution.getVariable.mockReturnValueOnce(leadId);

      nock('http://localhost:3001')
        .get(`/api/leads/${leadId}`)
        .reply(200, crmData);

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('crmData', crmData);
      expect(mockExecution.setVariable).toHaveBeenCalledWith('enrichmentStatus', 'success');
    });

    it('should handle CRM API timeout gracefully', async () => {
      const leadId = 'LEAD-002';
      mockExecution.getVariable.mockReturnValueOnce(leadId);

      nock('http://localhost:3001')
        .get(`/api/leads/${leadId}`)
        .delay(35000) // Simulate timeout
        .reply(200, {});

      await expect(delegate.execute(mockExecution)).rejects.toThrow('CRM API timeout');
    });

    it('should retry on transient failures', async () => {
      const leadId = 'LEAD-003';
      mockExecution.getVariable.mockReturnValueOnce(leadId);

      nock('http://localhost:3001')
        .get(`/api/leads/${leadId}`)
        .times(2)
        .reply(500, { error: 'Internal Server Error' })
        .get(`/api/leads/${leadId}`)
        .reply(200, { companyName: 'Success Corp' });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'enrichmentStatus',
        'success'
      );
    });

    it('should handle 404 lead not found', async () => {
      const leadId = 'LEAD-INVALID';
      mockExecution.getVariable.mockReturnValueOnce(leadId);

      nock('http://localhost:3001')
        .get(`/api/leads/${leadId}`)
        .reply(404, { error: 'Lead not found' });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('enrichmentStatus', 'not_found');
      expect(mockExecution.setVariable).toHaveBeenCalledWith('enrichmentError', 'Lead not found in CRM');
    });

    it('should handle malformed CRM response', async () => {
      const leadId = 'LEAD-004';
      mockExecution.getVariable.mockReturnValueOnce(leadId);

      nock('http://localhost:3001')
        .get(`/api/leads/${leadId}`)
        .reply(200, 'Invalid JSON');

      await expect(delegate.execute(mockExecution)).rejects.toThrow('Invalid CRM response format');
    });
  });

  describe('Data Enrichment Logic', () => {
    it('should enrich lead with company size classification', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          leadId: 'LEAD-005',
          companySize: 150
        };
        return vars[key];
      });

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-005')
        .reply(200, { companySize: 150 });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('companySizeSegment', 'medium');
    });

    it('should calculate fit score based on industry', async () => {
      mockExecution.getVariable.mockReturnValueOnce('LEAD-006');

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-006')
        .reply(200, {
          industry: 'Healthcare',
          companySize: 500,
          revenue: 10000000
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        expect.stringMatching(/fitScore/),
        expect.any(Number)
      );
    });

    it('should extract decision maker information', async () => {
      mockExecution.getVariable.mockReturnValueOnce('LEAD-007');

      const crmData = {
        contacts: [
          { role: 'CEO', name: 'John Doe', email: 'john@example.com' },
          { role: 'CFO', name: 'Jane Smith', email: 'jane@example.com' }
        ]
      };

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-007')
        .reply(200, crmData);

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'decisionMakers',
        expect.arrayContaining([
          expect.objectContaining({ role: 'CEO' }),
          expect.objectContaining({ role: 'CFO' })
        ])
      );
    });
  });

  describe('Error Handling', () => {
    it('should handle network errors', async () => {
      mockExecution.getVariable.mockReturnValueOnce('LEAD-008');

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-008')
        .replyWithError('Network error');

      await expect(delegate.execute(mockExecution)).rejects.toThrow();
      expect(mockExecution.setVariable).toHaveBeenCalledWith('enrichmentStatus', 'error');
    });

    it('should handle missing leadId parameter', async () => {
      mockExecution.getVariable.mockReturnValueOnce(null);

      await expect(delegate.execute(mockExecution)).rejects.toThrow('leadId is required');
    });

    it('should handle invalid leadId format', async () => {
      mockExecution.getVariable.mockReturnValueOnce('INVALID');

      await expect(delegate.execute(mockExecution)).rejects.toThrow('Invalid leadId format');
    });

    it('should log enrichment failures for monitoring', async () => {
      const consoleSpy = jest.spyOn(console, 'error');
      mockExecution.getVariable.mockReturnValueOnce('LEAD-009');

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-009')
        .reply(500);

      await expect(delegate.execute(mockExecution)).rejects.toThrow();
      expect(consoleSpy).toHaveBeenCalled();
    });
  });

  describe('Performance', () => {
    it('should complete enrichment within 3 seconds', async () => {
      mockExecution.getVariable.mockReturnValueOnce('LEAD-010');

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-010')
        .reply(200, { companyName: 'Fast Corp' });

      const start = Date.now();
      await delegate.execute(mockExecution);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(3000);
    });

    it('should handle concurrent enrichment requests', async () => {
      const promises = [];

      for (let i = 0; i < 10; i++) {
        const mockExec = global.testUtils.createMockDelegate();
        mockExec.getVariable = jest.fn().mockReturnValue(`LEAD-${i}`);
        mockExec.setVariable = jest.fn();

        nock('http://localhost:3001')
          .get(`/api/leads/LEAD-${i}`)
          .reply(200, { companyName: `Company ${i}` });

        promises.push(delegate.execute(mockExec));
      }

      await expect(Promise.all(promises)).resolves.toBeDefined();
    });
  });

  describe('Cache Behavior', () => {
    it('should use cached data when available', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'leadId') return 'LEAD-011';
        if (key === 'crmDataCache') return { companyName: 'Cached Corp' };
        return null;
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('crmData', { companyName: 'Cached Corp' });
    });

    it('should refresh cache after expiration', async () => {
      const expiredCache = {
        data: { companyName: 'Old Corp' },
        timestamp: Date.now() - 3600000 // 1 hour old
      };

      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'leadId') return 'LEAD-012';
        if (key === 'crmDataCache') return expiredCache;
        return null;
      });

      nock('http://localhost:3001')
        .get('/api/leads/LEAD-012')
        .reply(200, { companyName: 'New Corp' });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'crmData',
        expect.objectContaining({ companyName: 'New Corp' })
      );
    });
  });
});
