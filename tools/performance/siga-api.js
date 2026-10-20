import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.SIGA_BASE_URL || '').replace(/\/$/, '');
const bearerToken = __ENV.SIGA_BEARER_TOKEN || '';

if (!baseUrl || !bearerToken) {
  throw new Error('Define SIGA_BASE_URL y SIGA_BEARER_TOKEN fuera del archivo/script.');
}

export const options = {
  stages: [
    { duration: '1m', target: 5 },
    { duration: '1m', target: 20 },
    { duration: '10m', target: 20 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const response = http.get(`${baseUrl}/api/me`, {
    headers: { Authorization: `Bearer ${bearerToken}` },
    tags: { route: 'GET /api/me' },
  });

  check(response, {
    'GET /api/me returns 200': (result) => result.status === 200,
    'GET /api/me returns JSON': (result) =>
      String(result.headers['Content-Type'] || '').includes('application/json'),
  });

  sleep(1);
}
