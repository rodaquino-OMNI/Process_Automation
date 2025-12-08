// Global test setup
const { configure } = require('@testing-library/react');
const dotenv = require('dotenv');

// Load test environment variables
dotenv.config({ path: '.env.test' });

// Configure testing library
configure({ testIdAttribute: 'data-testid' });

// Set test timeout
jest.setTimeout(30000);

// Mock console methods to reduce test noise
global.console = {
  ...console,
  error: jest.fn(),
  warn: jest.fn(),
  log: jest.fn()
};

// Mock process.env
process.env.NODE_ENV = 'test';
process.env.CAMUNDA_BASE_URL = 'http://localhost:8080';
process.env.CRM_API_URL = 'http://localhost:3001';
process.env.ANS_API_URL = 'http://localhost:3002';

// Global test utilities
global.testUtils = {
  createMockProcess: (variables = {}) => ({
    id: 'test-process-id',
    businessKey: 'test-business-key',
    variables: {
      leadId: 'LEAD-001',
      companyName: 'Test Company',
      companySize: 500,
      cnpj: '12.345.678/0001-90',
      ...variables
    }
  }),

  createMockDelegate: () => ({
    variables: new Map(),
    set: jest.fn((key, value) => {
      this.variables.set(key, value);
    }),
    get: jest.fn((key) => this.variables.get(key)),
    getVariable: jest.fn(),
    setVariable: jest.fn()
  }),

  wait: (ms) => new Promise(resolve => setTimeout(resolve, ms)),

  mockApiResponse: (data, status = 200) => ({
    ok: status >= 200 && status < 300,
    status,
    json: async () => data,
    text: async () => JSON.stringify(data)
  })
};

// Cleanup after each test
afterEach(() => {
  jest.clearAllMocks();
});
