const MEDDICScoringDelegate = require('../../../src/delegates/MEDDICScoringDelegate');

describe('MEDDICScoringDelegate', () => {
  let delegate;
  let mockExecution;

  beforeEach(() => {
    delegate = new MEDDICScoringDelegate();
    mockExecution = global.testUtils.createMockDelegate();
    mockExecution.getVariable = jest.fn();
    mockExecution.setVariable = jest.fn();
  });

  describe('MEDDIC Score Calculation', () => {
    it('should calculate perfect MEDDIC score (10/10)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: true }, // 2 points
          economicBuyer: { identified: true, accessible: true, engaged: true }, // 2 points
          decisionCriteria: { documented: true, aligned: true }, // 2 points
          decisionProcess: { mapped: true, timeline: true }, // 2 points
          identifyPain: { critical: true, quantified: true }, // 1 point
          champion: { identified: true, influential: true, committed: true } // 1 point
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('meddicScore', 10);
      expect(mockExecution.setVariable).toHaveBeenCalledWith('qualificationLevel', 'excellent');
    });

    it('should calculate minimum passing score (4/10)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: false, agreement: false },
          economicBuyer: { identified: true, accessible: false, engaged: false },
          decisionCriteria: { documented: true, aligned: false },
          decisionProcess: { mapped: true, timeline: false },
          identifyPain: { critical: false, quantified: false },
          champion: { identified: false, influential: false, committed: false }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith('meddicScore', 4);
      expect(mockExecution.setVariable).toHaveBeenCalledWith('qualificationLevel', 'marginal');
    });

    it('should calculate failing score (<4)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: false, quantified: false, agreement: false },
          economicBuyer: { identified: true, accessible: false, engaged: false },
          decisionCriteria: { documented: false, aligned: false },
          decisionProcess: { mapped: false, timeline: false },
          identifyPain: { critical: false, quantified: false },
          champion: { identified: false, influential: false, committed: false }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      const scoreCall = mockExecution.setVariable.mock.calls.find(
        call => call[0] === 'meddicScore'
      );
      expect(scoreCall[1]).toBeLessThan(4);
      expect(mockExecution.setVariable).toHaveBeenCalledWith('qualificationLevel', 'disqualified');
    });
  });

  describe('Individual Component Scoring', () => {
    it('should score Metrics component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'metrics') {
          return { defined: true, quantified: true, agreement: true };
        }
        return null;
      });

      const score = await delegate.scoreMetrics(mockExecution);
      expect(score).toBe(2);
    });

    it('should score Economic Buyer component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'economicBuyer') {
          return { identified: true, accessible: true, engaged: true };
        }
        return null;
      });

      const score = await delegate.scoreEconomicBuyer(mockExecution);
      expect(score).toBe(2);
    });

    it('should score Decision Criteria component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'decisionCriteria') {
          return { documented: true, aligned: true };
        }
        return null;
      });

      const score = await delegate.scoreDecisionCriteria(mockExecution);
      expect(score).toBe(2);
    });

    it('should score Decision Process component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'decisionProcess') {
          return { mapped: true, timeline: true };
        }
        return null;
      });

      const score = await delegate.scoreDecisionProcess(mockExecution);
      expect(score).toBe(2);
    });

    it('should score Identify Pain component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'identifyPain') {
          return { critical: true, quantified: true };
        }
        return null;
      });

      const score = await delegate.scoreIdentifyPain(mockExecution);
      expect(score).toBe(1);
    });

    it('should score Champion component correctly', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'champion') {
          return { identified: true, influential: true, committed: true };
        }
        return null;
      });

      const score = await delegate.scoreChampion(mockExecution);
      expect(score).toBe(1);
    });
  });

  describe('DMN Decision Table Integration', () => {
    it('should call DMN decision table for qualification', async () => {
      const dmnSpy = jest.spyOn(delegate, 'evaluateDMN');
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: true },
          economicBuyer: { identified: true, accessible: true, engaged: true },
          decisionCriteria: { documented: true, aligned: true },
          decisionProcess: { mapped: true, timeline: true },
          identifyPain: { critical: true, quantified: true },
          champion: { identified: true, influential: true, committed: true }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(dmnSpy).toHaveBeenCalledWith(
        'decision_meddic_qualification',
        expect.any(Object)
      );
    });

    it('should handle DMN evaluation errors gracefully', async () => {
      jest.spyOn(delegate, 'evaluateDMN').mockRejectedValue(
        new Error('DMN evaluation failed')
      );

      mockExecution.getVariable.mockImplementation((key) => {
        return { defined: true };
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'DMN evaluation failed'
      );
    });
  });

  describe('Qualification Recommendations', () => {
    it('should recommend proceeding for high scores (8+)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: true },
          economicBuyer: { identified: true, accessible: true, engaged: true },
          decisionCriteria: { documented: true, aligned: true },
          decisionProcess: { mapped: true, timeline: true },
          identifyPain: { critical: true, quantified: true },
          champion: { identified: true, influential: false, committed: false }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'recommendation',
        'proceed'
      );
    });

    it('should recommend coaching for medium scores (4-7)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: false },
          economicBuyer: { identified: true, accessible: true, engaged: false },
          decisionCriteria: { documented: true, aligned: false },
          decisionProcess: { mapped: false, timeline: false },
          identifyPain: { critical: true, quantified: false },
          champion: { identified: false, influential: false, committed: false }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'recommendation',
        'coach_and_improve'
      );
    });

    it('should recommend disqualification for low scores (<4)', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: false, quantified: false, agreement: false },
          economicBuyer: { identified: false, accessible: false, engaged: false },
          decisionCriteria: { documented: false, aligned: false },
          decisionProcess: { mapped: false, timeline: false },
          identifyPain: { critical: false, quantified: false },
          champion: { identified: false, influential: false, committed: false }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'recommendation',
        'disqualify'
      );
    });
  });

  describe('Gap Analysis', () => {
    it('should identify missing MEDDIC components', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: false, agreement: false },
          economicBuyer: { identified: false, accessible: false, engaged: false },
          decisionCriteria: { documented: true, aligned: true },
          decisionProcess: { mapped: true, timeline: true },
          identifyPain: { critical: true, quantified: true },
          champion: { identified: true, influential: true, committed: true }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'gaps',
        expect.arrayContaining(['economicBuyer', 'metrics_agreement'])
      );
    });

    it('should provide action items for identified gaps', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: true },
          economicBuyer: { identified: false, accessible: false, engaged: false },
          decisionCriteria: { documented: true, aligned: true },
          decisionProcess: { mapped: true, timeline: true },
          identifyPain: { critical: true, quantified: true },
          champion: { identified: true, influential: true, committed: true }
        };
        return vars[key];
      });

      await delegate.execute(mockExecution);

      expect(mockExecution.setVariable).toHaveBeenCalledWith(
        'actionItems',
        expect.arrayContaining([
          expect.objectContaining({
            component: 'economicBuyer',
            action: expect.any(String)
          })
        ])
      );
    });
  });

  describe('Performance', () => {
    it('should complete scoring within 100ms', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        const vars = {
          metrics: { defined: true, quantified: true, agreement: true },
          economicBuyer: { identified: true, accessible: true, engaged: true },
          decisionCriteria: { documented: true, aligned: true },
          decisionProcess: { mapped: true, timeline: true },
          identifyPain: { critical: true, quantified: true },
          champion: { identified: true, influential: true, committed: true }
        };
        return vars[key];
      });

      const start = Date.now();
      await delegate.execute(mockExecution);
      const duration = Date.now() - start;

      expect(duration).toBeLessThan(100);
    });
  });

  describe('Input Validation', () => {
    it('should handle missing MEDDIC components gracefully', async () => {
      mockExecution.getVariable.mockReturnValue(null);

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'Missing required MEDDIC components'
      );
    });

    it('should validate component structure', async () => {
      mockExecution.getVariable.mockImplementation((key) => {
        if (key === 'metrics') return 'invalid_structure';
        return null;
      });

      await expect(delegate.execute(mockExecution)).rejects.toThrow(
        'Invalid MEDDIC component structure'
      );
    });
  });
});
