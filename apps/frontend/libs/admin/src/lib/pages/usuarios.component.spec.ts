import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { APP_CONFIG, AppConfig, MeService } from '@siga/core';
import { CredencialTemporal, UsuarioAdmin, UsuarioDetalle } from '@siga/mocks';
import { Observable, Subject, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../state/admin.service';
import { AdminUsuariosComponent } from './usuarios.component';

const MI_OID = 'oid-admin-propio';

const usuarios: UsuarioAdmin[] = [
  { id: MI_OID, nombre: 'Admin Propio', email: 'admin@test.com', rol: 'ADMIN', estado: 'ACTIVO' },
  { id: 'oid-otro', nombre: 'Otro Docente', email: 'docente@test.com', rol: 'DOCENTE', estado: 'ACTIVO' },
  { id: 'oid-apoderado', nombre: 'Apoderado Inactivo', email: 'apoderado@test.com', rol: 'APODERADO', estado: 'INACTIVO' },
];

const detalle: UsuarioDetalle = {
  id: 'oid-otro',
  fullName: 'Otro Docente',
  email: 'docente@test.com',
  rol: 'DOCENTE',
  estado: 'ACTIVO',
  rut: '12345678-9',
  fechaNacimiento: '1985-03-04',
  detalle: 'Docente de Matemática',
  etiquetas: ['Contrato indefinido'],
};

const credencial: CredencialTemporal = {
  processId: null,
  email: 'docente@test.com',
  userId: 'oid-otro',
  temporaryPassword: 'Temp-1234',
  expiresAt: '2026-12-01T00:00:00',
};

const config: AppConfig = {
  bffBaseUrl: '',
  useMocks: false,
  msal: { clientId: '', authority: '', redirectUri: '', postLogoutRedirectUri: '', scopes: [] },
};

interface Opciones {
  usarMocks?: boolean;
  usuarios?: () => Observable<UsuarioAdmin[] | null>;
  resetPassword?: () => Observable<CredencialTemporal>;
  eliminarUsuario?: () => Observable<void>;
}

function configurar(opciones: Opciones = {}) {
  const adminService = {
    getUsuarios: vi.fn(opciones.usuarios ?? (() => of(usuarios))),
    invalidar: vi.fn(),
    eliminarUsuario: vi.fn(opciones.eliminarUsuario ?? (() => of(undefined))),
    resetPassword: vi.fn(opciones.resetPassword ?? (() => of(credencial))),
    getUsuarioDetalle: vi.fn(() => of(detalle)),
    getClases: vi.fn(() => of([])),
    buscarEstudiantes: vi.fn(() => of([])),
  };
  const meService = {
    getMe: () => of({ id: MI_OID, email: 'admin@test.com', displayName: 'Admin', roles: ['ADMIN'] }),
  };

  TestBed.configureTestingModule({
    imports: [AdminUsuariosComponent],
    providers: [
      { provide: APP_CONFIG, useValue: { ...config, useMocks: opciones.usarMocks ?? false } },
      { provide: AdminService, useValue: adminService },
      { provide: MeService, useValue: meService },
    ],
  });

  const fixture = TestBed.createComponent(AdminUsuariosComponent);
  fixture.detectChanges();
  return { fixture, adminService };
}

async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  for (let intento = 0; intento < 3; intento += 1) {
    TestBed.tick();
    await fixture.whenStable();
    await Promise.resolve();
  }
  fixture.detectChanges();
}

function texto(fixture: ComponentFixture<unknown>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

function filas(fixture: ComponentFixture<unknown>): HTMLTableRowElement[] {
  return Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr'),
  ) as HTMLTableRowElement[];
}

function filaCon(fixture: ComponentFixture<unknown>, contenido: string): HTMLTableRowElement {
  return filas(fixture).find((fila) => fila.textContent?.includes(contenido)) as HTMLTableRowElement;
}

function botonesDe(fila: HTMLTableRowElement): HTMLButtonElement[] {
  return Array.from(fila.querySelectorAll('button')) as HTMLButtonElement[];
}

function botonDe(fila: HTMLTableRowElement, etiqueta: string): HTMLButtonElement {
  const encontrado = botonesDe(fila).find((boton) => boton.textContent?.includes(etiqueta));
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

function boton(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.textContent?.trim() === etiqueta) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

function selector(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('siga-select'),
  ).find(
    (item) => item.querySelector('button')?.getAttribute('aria-label') === etiqueta,
  ) as HTMLElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el selector ${etiqueta}`);
  }
  return encontrado;
}

function elegirEnSelector(
  fixture: ComponentFixture<unknown>,
  etiqueta: string,
  opcion: string,
): void {
  (selector(fixture, etiqueta).querySelector('button') as HTMLButtonElement).click();
  fixture.detectChanges();
  const botonOpcion = Array.from(
    selector(fixture, etiqueta).querySelectorAll('li button'),
  ).find((item) => item.textContent?.includes(opcion)) as HTMLButtonElement;
  botonOpcion.click();
}

describe('AdminUsuariosComponent', () => {
  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('oculta Eliminar en la fila del usuario autenticado y lo mantiene en las demás', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    const filaPropia = filaCon(fixture, 'admin@test.com');
    const filaAjena = filaCon(fixture, 'docente@test.com');

    expect(botonesDe(filaPropia).some((boton) => boton.textContent?.includes('Ver'))).toBe(true);
    expect(botonesDe(filaPropia).some((boton) => boton.textContent?.includes('Eliminar'))).toBe(false);
    expect(botonesDe(filaPropia).some((boton) => boton.textContent?.includes('Restablecer'))).toBe(false);
    expect(botonesDe(filaAjena).some((boton) => boton.textContent?.includes('Eliminar'))).toBe(true);
    fixture.destroy();
  });

  it('en modo demo usa los usuarios mock y oculta las acciones de escritura', async () => {
    const { fixture } = configurar({ usarMocks: true, usuarios: () => of([]) });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Administrador SIGA');
    expect(texto(fixture)).not.toContain('Acciones');
    expect(
      Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('button')).some(
        (item) => item.textContent?.includes('Nuevo usuario'),
      ),
    ).toBe(false);
    fixture.destroy();
  });

  it('si falla la carga muestra el error e invalida el cache al reintentar', async () => {
    const { fixture, adminService } = configurar({
      usuarios: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(adminService.invalidar).toHaveBeenCalledTimes(1);
    expect(adminService.getUsuarios).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('filtra por nombre o correo y muestra el estado sin resultados', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    const buscador = (fixture.nativeElement as HTMLElement).querySelector(
      'input[type="search"]',
    ) as HTMLInputElement;

    buscador.value = 'docente';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(filas(fixture).length).toBe(1);
    expect(texto(fixture)).toContain('Otro Docente');

    buscador.value = 'no-existe';
    buscador.dispatchEvent(new Event('input'));
    fixture.detectChanges();

    expect(filas(fixture).length).toBe(1);
    expect(texto(fixture)).toContain('Sin resultados.');
    fixture.destroy();
  });

  it('filtra por rol y por estado desde los selectores', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    elegirEnSelector(fixture, 'Filtrar por rol', 'DOCENTE');
    await esperar(fixture);
    expect(filas(fixture).length).toBe(1);
    expect(texto(fixture)).toContain('Otro Docente');

    elegirEnSelector(fixture, 'Filtrar por estado', 'INACTIVO');
    await esperar(fixture);
    expect(texto(fixture)).toContain('Sin resultados.');

    elegirEnSelector(fixture, 'Filtrar por rol', 'Todos los roles');
    await esperar(fixture);
    expect(filas(fixture).length).toBe(1);
    expect(texto(fixture)).toContain('Apoderado Inactivo');
    fixture.destroy();
  });

  it('pagina los resultados de diez en diez', async () => {
    const muchos = Array.from({ length: 12 }, (_, indice) => ({
      id: `oid-${indice}`,
      nombre: `Usuario ${indice}`,
      email: `usuario${indice}@test.com`,
      rol: 'DOCENTE' as const,
      estado: 'ACTIVO' as const,
    }));
    const { fixture } = configurar({ usuarios: () => of(muchos) });
    await esperar(fixture);

    expect(filas(fixture).length).toBe(10);
    expect(texto(fixture)).toContain('Mostrando 1–10 de 12');

    boton(fixture, 'Siguiente').click();
    await esperar(fixture);

    expect(filas(fixture).length).toBe(2);
    expect(texto(fixture)).toContain('Mostrando 11–12 de 12');
    fixture.destroy();
  });

  it('muestra y oculta el formulario de registro', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    boton(fixture, 'Nuevo usuario').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('Registrar usuario');
    expect(boton(fixture, 'Ocultar registro')).toBeTruthy();

    boton(fixture, 'Ocultar registro').click();
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Registrar usuario');
    fixture.destroy();
  });

  it('restablece la contraseña y muestra la credencial temporal', async () => {
    const { fixture, adminService } = configurar();
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Restablecer').click();
    await esperar(fixture);

    expect(adminService.resetPassword).toHaveBeenCalledWith('oid-otro');
    expect(texto(fixture)).toContain('Credencial temporal');
    expect(texto(fixture)).toContain('docente@test.com');
    expect(texto(fixture)).toContain('Temp-1234');

    boton(fixture, 'Entendido').click();
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain('Credencial temporal');
    fixture.destroy();
  });

  it('muestra el estado de generación mientras restablece la contraseña', async () => {
    const pendiente = new Subject<CredencialTemporal>();
    const { fixture } = configurar({ resetPassword: () => pendiente.asObservable() });
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Restablecer').click();
    fixture.detectChanges();

    const fila = filaCon(fixture, 'docente@test.com');
    expect(texto(fixture)).toContain('Generando…');
    expect(botonDe(fila, 'Generando…').disabled).toBe(true);

    pendiente.next(credencial);
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).toContain('Credencial temporal');
    fixture.destroy();
  });

  it('muestra el detalle de los errores de restablecimiento', async () => {
    const conCampos = new HttpErrorResponse({
      error: { errors: { email: 'El correo no está registrado' } },
      status: 400,
      statusText: 'Bad Request',
    });
    const { fixture } = configurar({ resetPassword: () => throwError(() => conCampos) });
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Restablecer').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('El correo no está registrado');
    fixture.destroy();
  });

  it('muestra el mensaje genérico cuando el error no trae detalle', async () => {
    const { fixture } = configurar({
      resetPassword: () => throwError(() => new HttpErrorResponse({ status: 500 })),
    });
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Restablecer').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo completar la operación');
    fixture.destroy();
  });

  it('elimina un usuario confirmado y refresca la lista', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture, adminService } = configurar();
    await esperar(fixture);
    expect(adminService.getUsuarios).toHaveBeenCalledTimes(1);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Eliminar').click();
    await esperar(fixture);

    expect(confirmar).toHaveBeenCalled();
    expect(adminService.eliminarUsuario).toHaveBeenCalledWith('oid-otro');
    expect(texto(fixture)).toContain('Usuario docente@test.com eliminado.');
    expect(adminService.invalidar).toHaveBeenCalledTimes(1);
    expect(adminService.getUsuarios).toHaveBeenCalledTimes(2);
    fixture.destroy();
  });

  it('no elimina el usuario si se cancela la confirmación', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { fixture, adminService } = configurar();
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Eliminar').click();
    await esperar(fixture);

    expect(adminService.eliminarUsuario).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('muestra el estado de eliminación mientras espera al BFF', async () => {
    const pendiente = new Subject<void>();
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture } = configurar({ eliminarUsuario: () => pendiente.asObservable() });
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Eliminar').click();
    fixture.detectChanges();

    const fila = filaCon(fixture, 'docente@test.com');
    expect(texto(fixture)).toContain('Eliminando…');
    expect(botonDe(fila, 'Eliminando…').disabled).toBe(true);

    pendiente.next();
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).toContain('Usuario docente@test.com eliminado.');
    confirmar.mockRestore();
    fixture.destroy();
  });

  it('muestra el error del BFF al eliminar un usuario', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const error = new HttpErrorResponse({
      error: { message: 'No se puede eliminar al último administrador' },
      status: 409,
      statusText: 'Conflict',
    });
    const { fixture } = configurar({ eliminarUsuario: () => throwError(() => error) });
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Eliminar').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se puede eliminar al último administrador');
    fixture.destroy();
  });

  it('abre el detalle del usuario seleccionado', async () => {
    const { fixture, adminService } = configurar();
    await esperar(fixture);

    botonDe(filaCon(fixture, 'docente@test.com'), 'Ver').click();
    await esperar(fixture);

    expect(adminService.getUsuarioDetalle).toHaveBeenCalledWith('oid-otro');
    expect(texto(fixture)).toContain('Detalle del usuario');
    expect(texto(fixture)).toContain('Docente de Matemática');
    fixture.destroy();
  });
});
