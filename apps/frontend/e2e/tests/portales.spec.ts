import { expect, test } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { mockAuth, type Rol } from '../support/e2e-app';

const portales: Array<{ rol: Rol; ruta: string; portal: string }> = [
  { rol: 'ESTUDIANTE', ruta: '/estudiante/inicio', portal: 'Portal Estudiante' },
  { rol: 'APODERADO', ruta: '/apoderado/inicio', portal: 'Portal Apoderado' },
  { rol: 'DOCENTE', ruta: '/docente/inicio', portal: 'Portal Docente' },
];

for (const { rol, ruta, portal } of portales) {
  test(`${portal} carga su dashboard con autenticacion simulada`, async ({ page }) => {
    await allure.epic(portal);
    await allure.feature('Inicio');
    await allure.story(`Dashboard de ${rol.toLowerCase()}`);
    await allure.owner('qa');
    await allure.tags('e2e', 'smoke');

    await mockAuth(page, rol, true);
    await page.goto('/');

    await expect(page).toHaveURL(new RegExp(`${ruta.replaceAll('/', '\\/')}$`));
    await expect(page.getByRole('heading', { name: /Bienvenido/ })).toBeVisible();
    await expect(page.getByText(portal, { exact: true }).first()).toBeVisible();
  });
}
