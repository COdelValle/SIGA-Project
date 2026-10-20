import http from 'k6/http';
import { check, sleep } from 'k6';

const baseUrl = (__ENV.SIGA_BASE_URL || '').replace(/\/$/, '');
const bearerToken = __ENV.SIGA_BEARER_TOKEN || '';
const writePath = __ENV.SIGA_WRITE_PATH || '';
const method = (__ENV.SIGA_WRITE_METHOD || 'POST').toUpperCase();
const bodyTemplate = __ENV.SIGA_WRITE_BODY || '';
const allowWrites = __ENV.SIGA_ALLOW_WRITES === 'true';
const expectedStatuses = parseStatuses(__ENV.SIGA_WRITE_EXPECTED_STATUSES || '200,201,204');
const p95Ms = Number(__ENV.SIGA_WRITE_P95_MS || '3000');
const runId = String(Date.now());

if (!baseUrl || !bearerToken || !writePath) {
  throw new Error('Define SIGA_BASE_URL, SIGA_BEARER_TOKEN y SIGA_WRITE_PATH.');
}
if (!allowWrites) {
  throw new Error(
    'Escrituras deshabilitadas. Usa solo un entorno descartable y define SIGA_ALLOW_WRITES=true.',
  );
}
if (!['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
  throw new Error('SIGA_WRITE_METHOD debe ser POST, PUT, PATCH o DELETE.');
}
if (!Number.isFinite(p95Ms) || p95Ms <= 0) {
  throw new Error('SIGA_WRITE_P95_MS debe ser un número positivo.');
}
if (bodyTemplate) {
  parseBody(bodyTemplate, 'validacion');
}

export const options = {
  stages: [
    { duration: '1m', target: 5 },
    { duration: '1m', target: 20 },
    { duration: '10m', target: 20 },
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: [`p(95)<${p95Ms}`],
    http_req_failed: ['rate<0.01'],
  },
};

export default function () {
  const correlationId = `${runId}-${__VU}-${__ITER}`;
  const body = bodyTemplate ? parseBody(bodyTemplate, correlationId) : null;
  const headers = {
    Authorization: `Bearer ${bearerToken}`,
    'X-Correlation-Id': correlationId,
  };
  if (body) {
    headers['Content-Type'] = 'application/json';
  }

  const response = http.request(
    method,
    `${baseUrl}${writePath}`,
    body ? JSON.stringify(body) : null,
    {
      headers,
      tags: { route: `${method} ${writePath}` },
    },
  );

  check(response, {
    [`${method} ${writePath} devuelve un status permitido`]: (result) =>
      expectedStatuses.includes(result.status),
  });

  sleep(1);
}

function parseStatuses(value) {
  const statuses = value
    .split(',')
    .map((status) => Number(status.trim()))
    .filter((status) => Number.isInteger(status) && status >= 100 && status <= 599);

  if (statuses.length === 0) {
    throw new Error('SIGA_WRITE_EXPECTED_STATUSES no contiene códigos HTTP válidos.');
  }
  return statuses;
}

function parseBody(template, correlationId) {
  const rendered = template
    .replace(/\{\{correlationId\}\}/g, correlationId)
    .replace(/\{\{runId\}\}/g, runId)
    .replace(/\{\{vu\}\}/g, String(__VU))
    .replace(/\{\{iteration\}\}/g, String(__ITER));

  try {
    return JSON.parse(rendered);
  } catch {
    throw new Error('SIGA_WRITE_BODY debe ser JSON válido después de sustituir los placeholders.');
  }
}
