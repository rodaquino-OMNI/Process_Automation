import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const processStartTrend = new Trend('process_start_duration');
const taskCompletionTrend = new Trend('task_completion_duration');

// Test configuration
export const options = {
  stages: [
    { duration: '2m', target: 20 },   // Ramp up to 20 users
    { duration: '5m', target: 50 },   // Ramp up to 50 users
    { duration: '5m', target: 100 },  // Ramp up to 100 users
    { duration: '5m', target: 100 },  // Stay at 100 users
    { duration: '2m', target: 0 },    // Ramp down to 0 users
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'], // 95% of requests under 2s
    http_req_failed: ['rate<0.05'],     // Error rate under 5%
    errors: ['rate<0.05'],
    process_start_duration: ['p(95)<3000'], // Process start under 3s
    task_completion_duration: ['p(95)<1000'] // Task completion under 1s
  },
};

const BASE_URL = __ENV.CAMUNDA_BASE_URL || 'http://localhost:8080/engine-rest';

// Test scenarios
export default function () {
  // Scenario 1: Start new process instance (30% of load)
  if (Math.random() < 0.3) {
    startProcessInstance();
  }

  // Scenario 2: Complete external tasks (50% of load)
  else if (Math.random() < 0.8) {
    completeExternalTask();
  }

  // Scenario 3: Query process status (20% of load)
  else {
    queryProcessStatus();
  }

  sleep(1);
}

function startProcessInstance() {
  const payload = JSON.stringify({
    businessKey: `LOAD-TEST-${Date.now()}-${Math.random()}`,
    variables: {
      leadId: { value: `LEAD-${Math.floor(Math.random() * 10000)}`, type: 'String' },
      companyName: { value: `Company ${Math.floor(Math.random() * 1000)}`, type: 'String' },
      companySize: { value: Math.floor(Math.random() * 1000) + 50, type: 'Long' },
      cnpj: { value: '12.345.678/0001-90', type: 'String' }
    }
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
    },
  };

  const startTime = Date.now();
  const response = http.post(
    `${BASE_URL}/process-definition/key/AUSTA_B2B_Sales_V3/start`,
    payload,
    params
  );

  const duration = Date.now() - startTime;
  processStartTrend.add(duration);

  const success = check(response, {
    'process started successfully': (r) => r.status === 200,
    'process instance ID returned': (r) => r.json('id') !== undefined,
  });

  errorRate.add(!success);
}

function completeExternalTask() {
  // 1. Fetch and lock external task
  const fetchPayload = JSON.stringify({
    workerId: `worker-${__VU}`,
    maxTasks: 1,
    topics: [
      { topicName: 'lead-enrichment', lockDuration: 10000 },
      { topicName: 'roi-calculation', lockDuration: 10000 },
      { topicName: 'meddic-scoring', lockDuration: 10000 },
    ]
  });

  const fetchResponse = http.post(
    `${BASE_URL}/external-task/fetchAndLock`,
    fetchPayload,
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  if (fetchResponse.status === 200 && fetchResponse.json().length > 0) {
    const task = fetchResponse.json()[0];

    // 2. Complete the task
    const completePayload = JSON.stringify({
      workerId: `worker-${__VU}`,
      variables: {
        taskCompleted: { value: true, type: 'Boolean' },
        completionTime: { value: new Date().toISOString(), type: 'String' }
      }
    });

    const startTime = Date.now();
    const completeResponse = http.post(
      `${BASE_URL}/external-task/${task.id}/complete`,
      completePayload,
      {
        headers: { 'Content-Type': 'application/json' },
      }
    );

    const duration = Date.now() - startTime;
    taskCompletionTrend.add(duration);

    const success = check(completeResponse, {
      'task completed successfully': (r) => r.status === 204,
    });

    errorRate.add(!success);
  }
}

function queryProcessStatus() {
  const response = http.get(`${BASE_URL}/process-instance?active=true&maxResults=10`);

  const success = check(response, {
    'query successful': (r) => r.status === 200,
    'returned process instances': (r) => Array.isArray(r.json()),
  });

  errorRate.add(!success);
}

// Stress test scenario
export function stressTest() {
  const response = http.post(
    `${BASE_URL}/process-definition/key/AUSTA_B2B_Sales_V3/start`,
    JSON.stringify({
      businessKey: `STRESS-TEST-${Date.now()}`,
      variables: {
        leadId: { value: `LEAD-STRESS-${Math.random()}`, type: 'String' },
        companySize: { value: 500, type: 'Long' }
      }
    }),
    {
      headers: { 'Content-Type': 'application/json' },
    }
  );

  check(response, {
    'stress test instance created': (r) => r.status === 200,
  });
}

export const stressTestOptions = {
  executor: 'constant-arrival-rate',
  rate: 200, // 200 iterations per second
  timeUnit: '1s',
  duration: '2m',
  preAllocatedVUs: 500,
  maxVUs: 1000,
};
