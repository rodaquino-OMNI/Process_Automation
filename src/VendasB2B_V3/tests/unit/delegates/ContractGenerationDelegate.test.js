const nock = require('nock');
const ContractGenerationDelegate = require('../../../src/delegates/ContractGenerationDelegate');

describe('ContractGenerationDelegate', () => {
  let delegate;
  let mockExecution;

  beforeEach(() => {
    delegate = new ContractGenerationDelegate();
    mockExecution = global.testUtils.createMockDelegate();
    mockExecution.getVariable = jest.fn();
    mockExecution.setVariable = jest.fn();
  });

  afterEach(() => {
    nock.cleanAll();
  });

  describe('Contract Template Generation', () => {
    it('should generate contract from template', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'TechCorp Inc',
          cnpj: '12.345.678/0001-90',
          planType: 'PME',
          beneficiaryCount: 100,
          monthlyPremium: 45000,
          startDate: '2025-01-01',
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'contractGenerated',
        true
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'contractDocumentUrl',
        expect.stringContaining('.pdf')
      );
    });

    it('should include all required contract clauses', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          planType: 'PME',
          beneficiaryCount: 50
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const contractData = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'contractData'
      )[1];

      expect(contractData.clauses).toEqual(
        expect.arrayContaining([
          expect.objectContaining({ type: 'payment_terms' }),
          expect.objectContaining({ type: 'coverage_scope' }),
          expect.objectContaining({ type: 'cancellation_policy' }),
          expect.objectContaining({ type: 'price_adjustment' }),
          expect.objectContaining({ type: 'dispute_resolution' })
        ])
      );
    });

    it('should apply correct template for plan type', async () => {
      const planTypes = ['PME', 'ENTERPRISE', 'INDIVIDUAL'];

      for (const planType of planTypes) {
        const mockExec = global.testUtils.createMockDelegate();
        mockExec.getVariable = jest.fn().mockImplementation((key) => {
          if (key === 'planType') return planType;
          return 'default';
        });
        mockExec.setVariable = jest.fn();

        await delegate.execute(mockExec);

        expect(mockExec.setVariable).toHaveBeenCalledWith(
          'templateUsed',
          `contract_template_${planType.toLowerCase()}`
        );
      }
    });
  });

  describe('E-Signature Integration (DocuSign)', () => {
    it('should send contract for e-signature successfully', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          clientEmail: 'client@example.com',
          clientName: 'John Doe'
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .reply(201, {
          envelopeId: 'ENV-12345',
          status: 'sent',
          uri: '/envelopes/ENV-12345'
        });

      await delegate.sendForSignature(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'envelopeId',
        'ENV-12345'
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'signatureStatus',
        'sent'
      );
    });

    it('should retry on DocuSign API failures', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          clientEmail: 'client@example.com'
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .times(2)
        .reply(500, { error: 'Internal Server Error' })
        .post('/v2/accounts/test/envelopes')
        .reply(201, { envelopeId: 'ENV-12346', status: 'sent' });

      await delegate.sendForSignature(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'signatureStatus',
        'sent'
      );
    });

    it('should handle DocuSign authentication errors', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          clientEmail: 'client@example.com'
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .reply(401, { error: 'Invalid authentication token' });

      await expect(delegate.sendForSignature(mockExecution))
        .rejects.toThrow('DocuSign authentication failed');
    });

    it('should set appropriate signing order for multiple signers', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          signers: [
            { email: 'client@example.com', name: 'Client', role: 'client', order: 1 },
            { email: 'sales@austa.com', name: 'Sales Rep', role: 'austa', order: 2 }
          ]
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .reply(201, { envelopeId: 'ENV-12347', status: 'sent' });

      await delegate.sendForSignature(mockExecution);

      const docusignRequest = nock.recorder.play()[0];
      expect(JSON.parse(docusignRequest.body).recipients.signers).toHaveLength(2);
    });
  });

  describe('Contract Validation', () => {
    it('should validate all required fields are present', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          // Missing cnpj
          planType: 'PME'
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution))
        .rejects.toThrow('Missing required field: cnpj');
    });

    it('should validate contract value within limits', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          monthlyPremium: 1000000000 // Too high
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution))
        .rejects.toThrow('Contract value exceeds maximum limit');
    });

    it('should validate contract duration', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          contractDuration: 120 // 10 years - too long
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution))
        .rejects.toThrow('Contract duration exceeds maximum');
    });

    it('should validate CNPJ format', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          cnpj: 'INVALID'
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution))
        .rejects.toThrow('Invalid CNPJ format');
    });
  });

  describe('PDF Generation', () => {
    it('should generate PDF with correct formatting', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          cnpj: '12.345.678/0001-90',
          planType: 'PME'
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const pdfUrl = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'contractDocumentUrl'
      )[1];

      expect(pdfUrl).toMatch(/\.pdf$/);
    });

    it('should include company logo in PDF', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          companyLogoUrl: 'https://example.com/logo.png'
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const contractData = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'contractData'
      )[1];

      expect(contractData.includesLogo).toBe(true);
    });

    it('should handle PDF generation errors gracefully', async () => {
      const pdfSpy = jest.spyOn(delegate, 'generatePDF');
      pdfSpy.mockRejectedValue(new Error('PDF generation failed'));

      mockExecution.getVariable.mockImplementation((key) => {
        return 'default';
      });

      await expect(delegate.execute(mockExecution))
        .rejects.toThrow('PDF generation failed');
    });
  });

  describe('Contract Numbering', () => {
    it('should generate unique contract numbers', async () => {
      const contractNumbers = new Set();

      for (let i = 0; i < 100; i++) {
        const mockExec = global.testUtils.createMockDelegate();
        mockExec.getVariable = jest.fn().mockReturnValue('default');
        mockExec.setVariable = jest.fn();

        await delegate.execute(mockExec);

        const contractNumber = mockExec.setVariable.mock.calls.find(
          call => call[0] === 'contractNumber'
        )[1];

        contractNumbers.add(contractNumber);
      }

      expect(contractNumbers.size).toBe(100);
    });

    it('should follow contract number format YYYY-NNNNNN', async () => {
      mockExecution.getVariable.mockReturnValue('default');

      await delegate.execute(mockExecution);

      const contractNumber = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'contractNumber'
      )[1];

      expect(contractNumber).toMatch(/^2025-\d{6}$/);
    });
  });

  describe('Retry Logic', () => {
    it('should implement exponential backoff for retries', async () => {
      const timestamps = [];

      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          clientEmail: 'client@example.com'
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .times(3)
        .reply(function() {
          timestamps.push(Date.now());
          return [503, { error: 'Service unavailable' }];
        })
        .post('/v2/accounts/test/envelopes')
        .reply(201, { envelopeId: 'ENV-12348' });

      await delegate.sendForSignature(mockExecution);

      // Verify exponential backoff
      for (let i = 1; i < timestamps.length; i++) {
        const delay = timestamps[i] - timestamps[i-1];
        expect(delay).toBeGreaterThan(1000 * Math.pow(2, i-1));
      }
    });

    it('should fail after max retry attempts', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          contractDocumentUrl: 'https://example.com/contract.pdf',
          clientEmail: 'client@example.com'
        };
        return vars[key];
      });

      nock('https://api.docusign.com')
        .post('/v2/accounts/test/envelopes')
        .times(5) // Max retries
        .reply(500, { error: 'Internal Server Error' });

      await expect(delegate.sendForSignature(mockExecution))
        .rejects.toThrow('Max retry attempts exceeded');
    });
  });

  describe('Performance', () => {
    it('should generate contract within 5 seconds', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companyName: 'Test Company',
          cnpj: '12.345.678/0001-90',
          planType: 'PME'
        };
        return vars[key];
      });

      const start = Date.now();
      await delegate.execute(mockExecution);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(5000);
    });

    it('should handle concurrent contract generation', async () => {
      const promises = [];

      for (let i = 0; i < 10; i++) {
        const mockExec = global.testUtils.createMockDelegate();
        mockExec.getVariable = jest.fn().mockImplementation((key) => {
          if (key === 'companyName') return `Company ${i}`;
          return 'default';
        });
        mockExec.setVariable = jest.fn();

        promises.push(delegate.execute(mockExec));
      }

      await expect(Promise.all(promises)).resolves.toBeDefined();
    });
  });

  describe('Error Logging', () => {
    it('should log contract generation failures', async () => {
      const consoleSpy = jest.spyOn(console, 'error');

      mockExecution.getVariable.mockImplementation(() => {
        throw new Error('Database connection failed');
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow();
      expect(consoleSpy).toHaveBeenCalled();
    });

    it('should store error details in process variables', async () => {
      mockExecution.getVariable.mockImplementation(() => {
        throw new Error('Template not found');
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow();
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'contractGenerationError',
        expect.stringContaining('Template not found')
      );
    });
  });
});
