import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AuthErrorService, AuthService } from '@siga/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AuthErrorComponent } from './auth-error.component';

function configurar(error?: { code?: string; message: string; detail?: string }) {
  const auth = { login: vi.fn() };

  TestBed.configureTestingModule({
    imports: [AuthErrorComponent],
    providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
  });

  const authError = TestBed.inject(AuthErrorService);
  if (error) {
    authError.set(error);
  }
  const fixture = TestBed.createComponent(AuthErrorComponent);
  fixture.detectChanges();
  return { fixture, auth, authError };
}

function texto(fixture: Awaited<ReturnType<typeof configurar>>['fixture']): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

describe('AuthErrorComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra el mensaje general sin detalle de error', () => {
    const { fixture } = configurar();

    expect(texto(fixture)).toContain('No pudimos iniciar tu sesion');
    expect(texto(fixture)).toContain('Intenta nuevamente');
    expect(texto(fixture)).not.toContain('invalid_grant');
    fixture.destroy();
  });

  it('muestra el código, mensaje y detalle del error almacenado', () => {
    const { fixture } = configurar({
      code: 'invalid_grant',
      message: 'El código de autorización expiró',
      detail: 'correlation-id-123',
    });

    expect(texto(fixture)).toContain('invalid_grant');
    expect(texto(fixture)).toContain('El código de autorización expiró');
    expect(texto(fixture)).toContain('correlation-id-123');

    const enlaces = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a'),
    ) as HTMLAnchorElement[];
    expect(enlaces[0].getAttribute('href')).toBe('/');
    fixture.destroy();
  });

  it('al reintentar limpia el error e inicia sesión nuevamente', () => {
    const { fixture, auth, authError } = configurar({
      code: 'invalid_grant',
      message: 'El código expiró',
    });

    const reintentar = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ).find((boton) => boton.textContent?.includes('Reintentar')) as HTMLButtonElement;
    reintentar.click();
    fixture.detectChanges();

    expect(authError.error()).toBeNull();
    expect(auth.login).toHaveBeenCalledTimes(1);
    expect(texto(fixture)).not.toContain('invalid_grant');
    fixture.destroy();
  });
});
