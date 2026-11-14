import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import type { Page } from '@playwright/test';

export type Rol = 'ADMIN' | 'DOCENTE' | 'APODERADO' | 'ESTUDIANTE';

export const pageResponse = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
  first: true,
  last: true,
  empty: true,
};

const rawConfig = JSON.parse(
  readFileSync(join(process.cwd(), 'e2e', 'config.e2e.json'), 'utf8'),
);

const scopes = [...new Set(rawConfig.msal.scopes.map((scope: string) => scope.split('/').pop()))];
const header = Buffer.from(JSON.stringify({ alg: 'none', typ: 'JWT' })).toString('base64url');
const payload = Buffer.from(
  JSON.stringify({
    roles: ['ADMIN', 'DOCENTE', 'ESTUDIANTE', 'APODERADO'],
    scp: scopes.join(' '),
    oid: 'e2e',
  }),
).toString('base64url');

export function e2eConfig(useMocks = false): string {
  return JSON.stringify({
    ...rawConfig,
    useMocks,
    mockAccessToken: `${header}.${payload}.e2e`,
  });
}

export async function mockAuth(page: Page, rol: Rol, useMocks = false): Promise<void> {
  await page.route('**/config.json', (route) =>
    route.fulfill({ contentType: 'application/json', body: e2eConfig(useMocks) }),
  );

  await page.route('**/api/**', (route) => {
    const path = new URL(route.request().url()).pathname;
    if (path === '/api/me') {
      return route.fulfill({
        json: {
          id: 'e2e',
          email: `e2e-${rol.toLowerCase()}@siga.cl`,
          displayName: `E2E ${rol}`,
          roles: [rol],
        },
      });
    }
    if (path.includes('/notificaciones/me/no-leidas/count')) {
      return route.fulfill({ json: { noLeidas: 0 } });
    }
    if (path.includes('/notificaciones/me')) {
      return route.fulfill({ json: pageResponse });
    }
    return route.fulfill({ json: pageResponse });
  });
}
