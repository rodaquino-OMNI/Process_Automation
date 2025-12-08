const ROICalculatorDelegate = require('../../../src/delegates/ROICalculatorDelegate');

describe('ROICalculatorDelegate', () => {
  let delegate;
  let mockExecution;

  beforeEach(() => {
    delegate = new ROICalculatorDelegate();
    mockExecution = global.testUtils.createMockDelegate();
    mockExecution.getVariable = jest.fn();
    mockExecution.setVariable = jest.fn();
  });

  describe('Basic ROI Calculation', () => {
    it('should calculate ROI with standard parameters', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'roi',
        expect.any(Number)
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'roiPercentage',
        expect.any(Number)
      );
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'paybackPeriodMonths',
        expect.any(Number)
      );
    });

    it('should calculate positive ROI correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 200,
          currentPremium: 600,
          proposedPremium: 500,
          implementationCost: 100000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const roiCall = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'roiPercentage'
      );
      expect(roiCall[1]).toBeGreaterThan(0);
    });

    it('should calculate negative ROI when costs exceed savings', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 50,
          currentPremium: 500,
          proposedPremium: 510, // Higher premium
          implementationCost: 200000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const roiCall = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'roiPercentage'
      );
      expect(roiCall[1]).toBeLessThan(0);
    });
  });

  describe('Cost Components', () => {
    it('should include implementation costs in calculation', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 150,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 75000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'totalImplementationCost',
        75000
      );
    });

    it('should calculate training costs based on company size', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 300,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'trainingCost',
        expect.any(Number)
      );
    });

    it('should include migration costs for existing plans', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 50000,
          contractDuration: 12,
          hasExistingPlan: true
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'migrationCost',
        expect.any(Number)
      );
    });
  });

  describe('Savings Calculation', () => {
    it('should calculate monthly savings accurately', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 600,
          proposedPremium: 500,
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const monthlySavings = (600 - 500) * 100; // 10,000
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'monthlySavings',
        monthlySavings
      );
    });

    it('should calculate annual savings', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 600,
          proposedPremium: 500,
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const annualSavings = (600 - 500) * 100 * 12; // 120,000
      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'annualSavings',
        annualSavings
      );
    });

    it('should calculate lifetime value', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 200,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 100000,
          contractDuration: 36 // 3 years
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'lifetimeValue',
        expect.any(Number)
      );
    });
  });

  describe('Payback Period', () => {
    it('should calculate payback period in months', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 600,
          proposedPremium: 500,
          implementationCost: 60000, // 6 months payback
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'paybackPeriodMonths',
        6
      );
    });

    it('should handle infinite payback period when no savings', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 500, // No savings
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'paybackPeriodMonths',
        Infinity
      );
    });
  });

  describe('Edge Cases', () => {
    it('should handle zero implementation cost', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 0,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'paybackPeriodMonths',
        0
      );
    });

    it('should handle very large company sizes', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 10000,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 500000,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'roi',
        expect.any(Number)
      );
    });

    it('should handle decimal values in calculations', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 150,
          currentPremium: 567.89,
          proposedPremium: 489.12,
          implementationCost: 75432.50,
          contractDuration: 12
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const roiCall = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'roi'
      );
      expect(roiCall[1]).toBeCloseTo(expect.any(Number), 2);
    });
  });

  describe('Input Validation', () => {
    it('should throw error for negative company size', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: -100,
          currentPremium: 500,
          proposedPremium: 450
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'Company size must be positive'
      );
    });

    it('should throw error for missing required parameters', async () => {
      mockExecution.getVariable.mockReturnValue(undefined);

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'Missing required parameters'
      );
    });

    it('should throw error for invalid contract duration', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 50000,
          contractDuration: 0
        };
        return vars[key];
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'Contract duration must be positive'
      );
    });
  });

  describe('Performance', () => {
    it('should complete calculation within 100ms', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          companySize: 100,
          currentPremium: 500,
          proposedPremium: 450,
          implementationCost: 50000,
          contractDuration: 12
        };
        return vars[key];
      });

      const start = Date.now();
      await delegate.execute(mockExecution);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(100);
    });

    it('should handle batch calculations efficiently', async () => {
      const calculations = [];

      for (let i = 0; i < 100; i++) {
        const mockExec = global.testUtils.createMockDelegate();
        mockExec.getVariable = jest.fn().mockImplementation((key) => {
          const vars = {
            companySize: 100 + i,
            currentPremium: 500,
            proposedPremium: 450,
            implementationCost: 50000,
            contractDuration: 12
          };
          return vars[key];
        });
        mockExec.setVariable = jest.fn();
        calculations.push(delegate.execute(mockExec));
      }

      const start = Date.now();
      await Promise.all(calculations);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(1000); // Should complete 100 calculations in <1s
    });
  });
});
