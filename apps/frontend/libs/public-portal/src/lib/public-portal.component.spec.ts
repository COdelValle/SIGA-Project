import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService } from '@siga/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PUBLIC_ROUTES, PublicPortalComponent } from './public-portal.component';
import { SinAccesoComponent } from './sin-acceso.component';

function configurar<T>(componente: new () => T, autenticado = false) {
  const auth = {
    login: vi.fn(),
    logout: vi.fn(),
    isAuthenticated: vi.fn(() => autenticado),
  };

  TestBed.configureTestingModule({
    imports: [componente],
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });

  const fixture = TestBed.createComponent(componente);
  fixture.detectChanges();
  return { fixture, auth };
}

function texto(fixture: { nativeElement: HTMLElement }): string {
  return fixture.nativeElement.textContent ?? '';
}

describe('PublicPortalComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra los accesos y navega al login al ingresar', () => {
    const { fixture, auth } = configurar(PublicPortalComponent);

    expect(texto(fixture)).toContain('Portal Academico SIGA');
    expect(texto(fixture)).toContain('Estudiantes');
    expect(texto(fixture)).toContain('Apoderados');
    expect(texto(fixture)).toContain('Docentes');
    expect(texto(fixture)).toContain('SIGA - Gestion academica');

    const ingresar = (Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[]).find((boton) =>
      boton.textContent?.includes('Ingresar al portal'),
    ) as HTMLButtonElement;
    ingresar.click();

    expect(auth.login).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('expone la ruta pública raíz', () => {
    expect(PUBLIC_ROUTES.length).toBe(1);
    expect(PUBLIC_ROUTES[0].path).toBe('');
    expect(PUBLIC_ROUTES[0].component).toBe(PublicPortalComponent);
  });
});

describe('SinAccesoComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('cierra la sesión de Microsoft si aún está activa', () => {
    const { fixture, auth } = configurar(SinAccesoComponent, true);

    expect(texto(fixture)).toContain('Cuenta no habilitada');
    expect(auth.isAuthenticated).toHaveBeenCalled();
    expect(auth.logout).toHaveBeenCalledWith('/sin-acceso');
    fixture.destroy();
  });

  it('no cierra sesión cuando no hay autenticación activa', () => {
    const { fixture, auth } = configurar(SinAccesoComponent, false);

    expect(auth.logout).not.toHaveBeenCalled();
    fixture.destroy();
  });
});
