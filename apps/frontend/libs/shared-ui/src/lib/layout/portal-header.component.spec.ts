import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthService, Me, MeService, NotificationService, ThemeService } from '@siga/core';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PortalHeaderComponent } from './portal-header.component';

function configurar(me: Me, portal = '') {
  const auth = { logout: vi.fn() };
  const notificationService = {
    getMine: vi.fn(() => of(null)),
    getUnreadCount: vi.fn(() => of({ noLeidas: 0 })),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    clearRead: vi.fn(),
  };

  TestBed.configureTestingModule({
    imports: [PortalHeaderComponent],
    providers: [
      provideRouter([]),
      { provide: MeService, useValue: { getMe: () => of(me) } },
      { provide: AuthService, useValue: auth },
      { provide: NotificationService, useValue: notificationService },
    ],
  });

  const fixture = TestBed.createComponent(PortalHeaderComponent);
  fixture.componentRef.setInput('portal', portal);
  fixture.detectChanges();
  return { fixture, auth };
}

function texto(fixture: ReturnType<typeof configurar>['fixture']): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

function botonTema(fixture: ReturnType<typeof configurar>['fixture']): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.getAttribute('aria-label')?.startsWith('Cambiar a tema')) as
    | HTMLButtonElement
    | undefined;
  if (!encontrado) {
    throw new Error('No se encontro el boton de tema');
  }
  return encontrado;
}

describe('PortalHeaderComponent', () => {
  afterEach(() => {
    localStorage.removeItem('siga-theme');
    document.documentElement.removeAttribute('data-theme');
    TestBed.resetTestingModule();
  });

  it('muestra el portal, el nombre, el correo y las iniciales del usuario', () => {
    const { fixture } = configurar(
      {
        id: 'oid-1',
        email: 'camila.soto@siga.cl',
        displayName: 'Camila Antonieta Soto',
        roles: ['ESTUDIANTE'],
      },
      'Portal Estudiante',
    );

    expect(texto(fixture)).toContain('SIGA Virtual');
    expect(texto(fixture)).toContain('Portal Estudiante');
    expect(texto(fixture)).toContain('Camila Antonieta Soto');
    expect(texto(fixture)).toContain('camila.soto@siga.cl');
    expect(texto(fixture)).toContain('CA');

    const avatar = (fixture.nativeElement as HTMLElement).querySelector(
      'span[aria-label="Usuario Camila Antonieta Soto"]',
    );
    expect(avatar).not.toBeNull();
    fixture.destroy();
  });

  it('usa el correo como nombre visible cuando no hay displayName', () => {
    const { fixture } = configurar({
      id: 'oid-2',
      email: 'apoderado@siga.cl',
      displayName: '',
      roles: ['APODERADO'],
    });

    expect(texto(fixture)).toContain('apoderado@siga.cl');
    const avatar = (fixture.nativeElement as HTMLElement).querySelector(
      'span[aria-label="Usuario apoderado@siga.cl"]',
    );
    expect(avatar?.textContent?.trim()).toBe('A');
    fixture.destroy();
  });

  it('sin datos del usuario muestra el marcador "?" en el avatar', () => {
    const { fixture } = configurar({ id: '', email: '', displayName: '', roles: [] });

    const avatar = (fixture.nativeElement as HTMLElement).querySelector(
      'span[aria-label="Usuario "]',
    );
    expect(avatar?.textContent?.trim()).toBe('?');
    fixture.destroy();
  });

  it('alterna el tema entre oscuro y claro y actualiza el aria-label', () => {
    const { fixture } = configurar({
      id: 'oid-1',
      email: 'camila@siga.cl',
      displayName: 'Camila Soto',
      roles: ['ESTUDIANTE'],
    });

    const tema = TestBed.inject(ThemeService);
    tema.set('dark');
    fixture.detectChanges();
    TestBed.tick();

    const boton = botonTema(fixture);
    expect(boton.getAttribute('aria-label')).toBe('Cambiar a tema claro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');

    boton.click();
    fixture.detectChanges();
    TestBed.tick();

    expect(boton.getAttribute('aria-label')).toBe('Cambiar a tema oscuro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');

    boton.click();
    fixture.detectChanges();
    TestBed.tick();

    expect(boton.getAttribute('aria-label')).toBe('Cambiar a tema claro');
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
    fixture.destroy();
  });

  it('cerrar sesion delega en el servicio de autenticacion', () => {
    const { fixture, auth } = configurar({
      id: 'oid-1',
      email: 'camila@siga.cl',
      displayName: 'Camila Soto',
      roles: ['ESTUDIANTE'],
    });

    const cerrar = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((item) => item.textContent?.includes('Cerrar Sesion')) as HTMLButtonElement;
    cerrar.click();

    expect(auth.logout).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });
});
