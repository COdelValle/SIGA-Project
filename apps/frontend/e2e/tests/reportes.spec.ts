import { expect, test } from '@playwright/test';
import * as allure from 'allure-js-commons';
import { mockAuth } from '../support/e2e-app';

for (const { rol, home, portal } of [
  { rol: 'DOCENTE' as const, home: '/docente/inicio', portal: 'Portal Docente' },
  { rol: 'ADMIN' as const, home: '/admin/inicio', portal: 'Portal Administración' },
]) {
  test(`${portal} muestra el dashboard de reportes con polling`, async ({ page }) => {
    await allure.epic(portal);
    await allure.feature('Reportes');
    await allure.story('Dashboard de asistencia y rendimiento');
    await allure.owner('qa');
    await allure.tags('e2e', 'reportes');

    await mockAuth(page, rol);
    await page.goto('/');
    await expect(page).toHaveURL(new RegExp(`${home.replaceAll('/', '\\/')}$`));

    await page.getByRole('link', { name: 'Reportes' }).click();
    await expect(page).toHaveURL(/\/reportes$/);
    await expect(page.getByRole('heading', { name: 'Reportes académicos' })).toBeVisible();
    await expect(page.getByText('8vo Básico A · Matemática').first()).toBeVisible();
    await expect(page.getByRole('cell', { name: 'Ana Pérez' }).first()).toBeVisible();
    await expect(page.getByText('Actualización automática cada 15 s')).toBeVisible();
    await expect(page.getByText(/Última actualización:/)).toBeVisible();
  });
}
