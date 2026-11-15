import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { MsalService } from '@azure/msal-angular';
import { AsistenciaService, PerfilEstudianteService } from '@siga/academico';
import { AuthErrorService, Me, MeService } from '@siga/core';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { App } from './app';

@Component({ selector: 'siga-destino-prueba', template: '<p>destino</p>' })
class DestinoComponent {}

interface Opciones {
  cuentas?: { username: string }[];
  me?: Me | (() => Observable<Me>);
  errorPrevio?: { code?: string; message: string; detail?: string };
  conRuta?: boolean;
}

function configurar(opciones: Opciones = {}) {
  const perfilService = { invalidar: vi.fn() };
  const asistenciaService = { invalidar: vi.fn() };
  const meService = {
    getMe: vi.fn(() =>
      typeof opciones.me === 'function'
        ? opciones.me()
        : of(
            opciones.me ?? {
              id: 'oid',
              email: 'user@siga.cl',
              displayName: 'Usuario SIGA',
              roles: ['ADMIN'],
            },
          ),
    ),
    clear: vi.fn(),
  };

  TestBed.configureTestingModule({
    imports: [App],
    providers: [
      provideRouter(opciones.conRuta ? [{ path: 'destino', component: DestinoComponent }] : []),
      {
        provide: MsalService,
        useValue: { instance: { getAllAccounts: () => opciones.cuentas ?? [] } },
      },
      { provide: MeService, useValue: meService },
      { provide: PerfilEstudianteService, useValue: perfilService },
      { provide: AsistenciaService, useValue: asistenciaService },
    ],
  });

  const authError = TestBed.inject(AuthErrorService);
  const router = TestBed.inject(Router);
  if (opciones.errorPrevio) {
    authError.set(opciones.errorPrevio);
  }
  const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

  const fixture = TestBed.createComponent(App);
  fixture.detectChanges();

  return { fixture, router, navegar, authError, perfilService, asistenciaService, meService };
}

describe('App', () => {
  beforeEach(() => vi.restoreAllMocks());
  afterEach(() => TestBed.resetTestingModule());

  it('sin sesión activa no consulta el perfil ni navega', () => {
    const { fixture, navegar, meService } = configurar({ cuentas: [] });

    expect(meService.getMe).not.toHaveBeenCalled();
    expect(navegar).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('con sesión y rol navega al portal correspondiente', () => {
    const { fixture, navegar, meService } = configurar({
      cuentas: [{ username: 'admin@siga.cl' }],
      me: { id: 'oid', email: 'admin@siga.cl', displayName: 'Admin', roles: ['ADMIN'] },
    });

    expect(meService.getMe).toHaveBeenCalledWith(true);
    expect(navegar).toHaveBeenCalledWith(['/admin']);
    fixture.destroy();
  });

  it('autenticado sin rol limpia el perfil y redirige a sin acceso', () => {
    const { fixture, navegar, meService } = configurar({
      cuentas: [{ username: 'nuevo@siga.cl' }],
      me: { id: '', email: '', displayName: '', roles: [] },
    });

    expect(meService.clear).toHaveBeenCalledTimes(1);
    expect(navegar).toHaveBeenCalledWith(['/sin-acceso']);
    fixture.destroy();
  });

  it('si el retorno de Microsoft falló redirige al error sin consultar el perfil', () => {
    const { fixture, navegar, meService } = configurar({
      cuentas: [{ username: 'admin@siga.cl' }],
      errorPrevio: { code: 'invalid_grant', message: 'El código expiró' },
    });

    expect(meService.getMe).not.toHaveBeenCalled();
    expect(navegar).toHaveBeenCalledWith(['/error-acceso']);
    fixture.destroy();
  });

  it('guarda el error normalizado si no se puede resolver el perfil', () => {
    const consola = vi.spyOn(console, 'error').mockImplementation(() => undefined);
    const { fixture, navegar, authError } = configurar({
      cuentas: [{ username: 'admin@siga.cl' }],
      me: () =>
        throwError(() => ({ errorCode: 'token_invalido', errorMessage: 'Token inválido' })),
    });

    expect(consola).toHaveBeenCalled();
    expect(authError.error()).toEqual({
      code: 'token_invalido',
      message: 'Token inválido',
      detail: undefined,
    });
    expect(navegar).toHaveBeenCalledWith(['/error-acceso']);
    fixture.destroy();
  });

  it('invalida los caches de perfil y asistencia al terminar una navegación', async () => {
    const { fixture, router, perfilService, asistenciaService } = configurar({
      cuentas: [],
      conRuta: true,
    });
    const perfilAntes = perfilService.invalidar.mock.calls.length;
    const asistenciaAntes = asistenciaService.invalidar.mock.calls.length;

    await router.navigateByUrl('/destino');
    await fixture.whenStable();
    fixture.detectChanges();

    expect(perfilService.invalidar.mock.calls.length).toBeGreaterThan(perfilAntes);
    expect(asistenciaService.invalidar.mock.calls.length).toBeGreaterThan(asistenciaAntes);
    fixture.destroy();
  });
});
