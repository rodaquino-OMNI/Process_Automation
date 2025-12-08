const nock = require('nock');
const ANSRegistrationDelegate = require('../../../src/delegates/ANSRegistrationDelegate');

describe('ANSRegistrationDelegate', () => {
  let delegate;
  let mockExecution;

  beforeEach(() => {
    delegate = new ANSRegistrationDelegate();
    mockExecution = global.testUtils.createMockDelegate();
    mockExecution.getVariable = jest.fn();
    mockExecution.setVariable = jest.fn();
  });

  afterEach(() => {
    nock.cleanAll();
  });

  describe('ANS Registration Format Validation', () => {
    it('should validate correct ANS registration format', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-001',
          ansRegistrationNumber: '123456',
          companyData: {
            cnpj: '12.345.678/0001-90',
            razaoSocial: 'Test Company LTDA',
            address: 'Rua Test, 123'
          },
          beneficiaries: [
            { cpf: '123.456.789-00', name: 'John Doe', birthDate: '1990-01-01' }
          ]
        };
        return vars[key];
      });

      await delegate.validateRegistrationFormat(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'formatValidation',
        'valid'
      );
    });

    it('should reject invalid CNPJ format', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyData: {
            cnpj: 'INVALID',
            razaoSocial: 'Test Company LTDA'
          }
        };
        return vars[key];
      });

      await expect(delegate.validateRegistrationFormat(mockExecution))
        .rejects.toThrow('Invalid CNPJ format');
    });

    it('should reject invalid CPF format for beneficiaries', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyData: {
            cnpj: '12.345.678/0001-90',
            razaoSocial: 'Test Company LTDA'
          },
          beneficiaries: [
            { cpf: 'INVALID', name: 'John Doe' }
          ]
        };
        return vars[key];
      });

      await expect(delegate.validateRegistrationFormat(mockExecution))
        .rejects.toThrow('Invalid CPF format for beneficiary');
    });

    it('should validate required ANS fields', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyData: {
            cnpj: '12.345.678/0001-90'
            // Missing razaoSocial
          }
        };
        return vars[key];
      });

      await expect(delegate.validateRegistrationFormat(mockExecution))
        .rejects.toThrow('Missing required ANS field: razaoSocial');
    });
  });

  describe('ANS API Integration', () => {
    it('should successfully submit registration to ANS', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-001',
          companyData: {
            cnpj: '12.345.678/0001-90',
            razaoSocial: 'Test Company LTDA',
            address: 'Rua Test, 123'
          },
          beneficiaries: [
            { cpf: '123.456.789-00', name: 'John Doe', birthDate: '1990-01-01' }
          ]
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(200, {
          success: true,
          registrationNumber: 'ANS-2025-123456',
          protocol: 'PROT-001'
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'ansRegistrationNumber',
        'ANS-2025-123456'
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'ansStatus',
        'registered'
      );
    });

    it('should handle ANS API timeout (30s limit)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-002',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .delay(35000) // Exceeds 30s timeout
        .reply(200, {});

      await expect(delegate.execute(mockExecution)).rejects.toThrow('ANS API timeout');
      expect(mockExecution.setVariable).toHaveBeenCalledWith('ansStatus', 'timeout');
    });

    it('should retry on ANS temporary failures', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-003',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .times(2)
        .reply(503, { error: 'Service temporarily unavailable' })
        .post('/api/ans/register')
        .reply(200, {
          success: true,
          registrationNumber: 'ANS-2025-123457'
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'ansStatus',
        'registered'
      );
    });

    it('should handle ANS rejection with reason', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-004',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(400, {
          success: false,
          reason: 'Duplicate registration',
          code: 'ANS_ERR_DUPLICATE'
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('ansStatus', 'rejected');
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'ansRejectionReason',
        'Duplicate registration'
      );
    });
  });

  describe('Regulatory Compliance', () => {
    it('should validate ANS RN 469/2021 compliance', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          planType: 'PME',
          coverage: ['hospitalar', 'ambulatorial', 'obstetrico'],
          coparticipation: true
        };
        return vars[key];
      });

      await delegate.validateCompliance(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'complianceStatus',
        'compliant'
      );
    });

    it('should check beneficiary age limits', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          beneficiaries: [
            { birthDate: '2023-01-01', relationship: 'dependent' } // Too young
          ]
        };
        return vars[key];
      });

      await expect(delegate.validateCompliance(mockExecution))
        .rejects.toThrow('Beneficiary age validation failed');
    });

    it('should validate waiting period rules', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          planType: 'INDIVIDUAL',
          waitingPeriods: {
            urgency: 24, // hours
            hospitalization: 180, // days
            obstetric: 300 // days
          }
        };
        return vars[key];
      });

      await delegate.validateCompliance(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'waitingPeriodsValid',
        true
      );
    });

    it('should validate coverage according to ANS rol', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          coverage: ['invalid_procedure']
        };
        return vars[key];
      });

      await expect(delegate.validateCompliance(mockExecution))
        .rejects.toThrow('Invalid coverage procedure not in ANS rol');
    });
  });

  describe('Beneficiary Registration', () => {
    it('should register multiple beneficiaries in batch', async () => {
      const beneficiaries = Array.from({ length: 50 }, (_, i) => ({
        cpf: `000.000.${String(i).padStart(3, '0')}-00`,
        name: `Beneficiary ${i}`,
        birthDate: '1990-01-01'
      }));

      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-005',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(200, {
          success: true,
          registeredCount: 50
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'beneficiariesRegistered',
        50
      );
    });

    it('should handle partial beneficiary registration failures', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-006',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: [
            { cpf: '123.456.789-00', name: 'Valid' },
            { cpf: 'INVALID', name: 'Invalid' }
          ]
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(207, { // Multi-status
          success: true,
          registeredCount: 1,
          failedCount: 1,
          failures: [{ cpf: 'INVALID', reason: 'Invalid CPF' }]
        });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'beneficiariesRegistered',
        1
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'beneficiariesFailed',
        1
      );
    });
  });

  describe('Error Recovery', () => {
    it('should store registration data for manual retry', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-007',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(500, { error: 'Internal server error' });

      await expect(delegate.execute(mockExecution)).rejects.toThrow();
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'registrationDataBackup',
        expect.any(Object)
      );
    });

    it('should implement exponential backoff for retries', async () => {
      const retrySpy = jest.spyOn(delegate, 'retryWithBackoff');

      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-008',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .times(3)
        .reply(503)
        .post('/api/ans/register')
        .reply(200, { success: true });

      await delegate.execute(mockExecution);

      expect(retrySpy).toHaveBeenCalled();
    });
  });

  describe('Performance', () => {
    it('should complete registration within 10 seconds', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractId: 'CONTRACT-009',
          companyData: { cnpj: '12.345.678/0001-90', razaoSocial: 'Test' },
          beneficiaries: []
        };
        return vars[key];
      });

      nock('http://localhost:3002')
        .post('/api/ans/register')
        .reply(200, { success: true });

      const start = Date.now();
      await delegate.execute(mockExecution);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(10000);
    });
  });
});
