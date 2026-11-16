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
    if (path.endsWith('/reportes/filtros')) {
      return route.fulfill({
        json: {
          cursos: [{ id: 8, nombre: 'Matemática', curso: '8vo Básico A', idClase: 4 }],
        },
      });
    }
    if (path.endsWith('/reportes/filtros/estudiantes')) {
      return route.fulfill({
        json: [{ id: 41, nombre: 'Ana Pérez', rut: '11111111-1' }],
      });
    }
    if (path.endsWith('/reportes/asistencia')) {
      return route.fulfill({
        json: [
          {
            idEstudiante: 41,
            idCursoAsignatura: 8,
            fecha: '2026-11-09',
            estado: 'AUSENTE',
            porcentajeInasistencia: 45.0,
            totalRegistros: 20,
            totalInasistencias: 9,
            nivelRiesgo: 'CRITICO',
            actualizadoEn: '2026-11-09T22:00:00',
          },
        ],
      });
    }
    if (path.endsWith('/reportes/rendimiento')) {
      return route.fulfill({
        json: [
          {
            idEstudiante: 41,
            idEvaluacion: 12,
            idCursoAsignatura: 8,
            puntaje: 3.5,
            promedio: 3.5,
            evaluacionesInsuficientes: 2,
            nivelRiesgo: 'CRITICO',
            actualizadoEn: '2026-11-09T22:00:00',
          },
        ],
      });
    }
    if (path.includes('/notificaciones/me/no-leidas/count')) {
      return route.fulfill({ json: { noLeidas: 0 } });
    }
    if (path.includes('/notificaciones/me')) {
      return route.fulfill({ json: pageResponse });
    }
    if (
      path.endsWith('/docentes/cursos') ||
      path.endsWith('/docentes/horario') ||
      path.endsWith('/admin/usuarios') ||
      path.endsWith('/admin/asignaturas') ||
      path.endsWith('/admin/malla') ||
      path.endsWith('/admin/clases') ||
      path.endsWith('/admin/estudiantes')
    ) {
      return route.fulfill({ json: [] });
    }
    return route.fulfill({ json: pageResponse });
  });
}
