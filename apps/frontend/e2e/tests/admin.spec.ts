import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import AxeBuilder from '@axe-core/playwright';
import { expect, test } from '@playwright/test';
import * as allure from 'allure-js-commons';

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
const e2eConfig = JSON.stringify({
  ...rawConfig,
  mockAccessToken: `${header}.${payload}.e2e`,
});

const pageResponse = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  size: 20,
  number: 0,
  first: true,
  last: true,
  empty: true,
};

test.beforeEach(async ({ page }) => {
  await page.route('**/config.json', (route) =>
    route.fulfill({ contentType: 'application/json', body: e2eConfig }),
  );

  await page.route('**/api/bff/v1/notificaciones/me**', (route) =>
    route.fulfill({ json: pageResponse }),
  );

  await page.route('**/api/bff/v1/notificaciones/me/no-leidas/count', (route) =>
    route.fulfill({ json: { noLeidas: 0 } }),
  );

  await page.route('**/api/bff/v1/admin/usuarios', (route) =>
    route.fulfill({
      json: [
        {
          id: 'e2e',
          nombre: 'Administrador E2E',
          email: 'e2e@siga.cl',
          rol: 'ADMIN',
          estado: 'ACTIVO',
        },
      ],
    }),
  );

  await page.route('**/api/me', (route) =>
    route.fulfill({
      json: {
        id: 'e2e',
        email: 'e2e@siga.cl',
        displayName: 'E2E SIGA',
        roles: ['ADMIN'],
      },
    }),
  );
});

test('la autenticacion simulada entra al portal admin con JWT de prueba', async ({ page }) => {
  await allure.epic('Portal Administracion');
  await allure.feature('Autenticacion simulada');
  await allure.story('Acceso por rol ADMIN');
  await allure.owner('qa');
  await allure.tags('e2e', 'smoke');

  let authorization: string | undefined;

  await page.route('**/api/me', async (route) => {
    authorization = route.request().headers()['authorization'];
    await route.fulfill({
      json: {
        id: 'e2e',
        email: 'e2e@siga.cl',
        displayName: 'E2E SIGA',
        roles: ['ADMIN'],
      },
    });
  });

  await page.goto('/');
  await expect(page).toHaveURL(/\/admin\/inicio$/);
  await expect(page.getByRole('heading', { name: /Bienvenido/ })).toBeVisible();
  expect(authorization).toContain('Bearer eyJ');
});

test('el portal admin no tiene violaciones de accesibilidad criticas', async ({ page }) => {
  await allure.epic('Portal Administracion');
  await allure.feature('Accesibilidad');
  await allure.story('Sin violaciones criticas');
  await allure.owner('qa');
  await allure.tags('e2e', 'a11y');

  await page.goto('/');
  await expect(page).toHaveURL(/\/admin\/inicio$/);

  const results = await new AxeBuilder({ page }).analyze();
  await test.info().attach('axe-admin', {
    body: JSON.stringify(results, null, 2),
    contentType: 'application/json',
  });

  const criticas = results.violations.filter(
    (violation) => violation.impact === 'critical' || violation.impact === 'serious',
  );
  expect(criticas).toEqual([]);
});
