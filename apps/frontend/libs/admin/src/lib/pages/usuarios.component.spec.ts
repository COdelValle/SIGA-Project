import { TestBed } from '@angular/core/testing';
import { APP_CONFIG, AppConfig, MeService } from '@siga/core';
import { UsuarioAdmin } from '@siga/mocks';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../state/admin.service';
import { AdminUsuariosComponent } from './usuarios.component';

const MI_OID = 'oid-admin-propio';

const usuarios: UsuarioAdmin[] = [
  { id: MI_OID, nombre: 'Admin Propio', email: 'admin@test.com', rol: 'ADMIN', estado: 'ACTIVO' },
  { id: 'oid-otro', nombre: 'Otro Docente', email: 'docente@test.com', rol: 'DOCENTE', estado: 'ACTIVO' },
];

const config: AppConfig = {
  bffBaseUrl: '',
  useMocks: false,
  msal: { clientId: '', authority: '', redirectUri: '', postLogoutRedirectUri: '', scopes: [] },
};

function configurar() {
  const adminService = {
    getUsuarios: vi.fn(() => of(usuarios)),
    invalidar: vi.fn(),
    eliminarUsuario: vi.fn(() => of(undefined)),
    resetPassword: vi.fn(() =>
      of({ usuario: 'docente@test.com', password: 'temporal-123', expiraEn: '2026-10-12T00:00:00' }),
    ),
  };
  const meService = {
    getMe: () => of({ id: MI_OID, email: 'admin@test.com', displayName: 'Admin', roles: ['ADMIN'] }),
  };

  TestBed.configureTestingModule({
    imports: [AdminUsuariosComponent],
    providers: [
      { provide: APP_CONFIG, useValue: config },
      { provide: AdminService, useValue: adminService },
      { provide: MeService, useValue: meService },
    ],
  });

  const fixture = TestBed.createComponent(AdminUsuariosComponent);
  fixture.detectChanges();
  return { fixture, adminService };
}

function filaCon(
  fixture: Awaited<ReturnType<typeof configurar>>['fixture'],
  texto: string,
): HTMLTableRowElement {
  const filas = Array.from(fixture.nativeElement.querySelectorAll('tbody tr')) as HTMLTableRowElement[];
  return filas.find((fila) => fila.textContent?.includes(texto)) as HTMLTableRowElement;
}

function botonesDe(fila: HTMLTableRowElement): HTMLButtonElement[] {
  return Array.from(fila.querySelectorAll('button')) as HTMLButtonElement[];
}

describe('AdminUsuariosComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('oculta Eliminar en la fila del usuario autenticado y lo mantiene en las demás', async () => {
    const { fixture } = configurar();
    await fixture.whenStable();
    fixture.detectChanges();

    const filaPropia = filaCon(fixture, 'admin@test.com');
    const filaAjena = filaCon(fixture, 'docente@test.com');

    expect(botonesDe(filaPropia).some((boton) => boton.textContent?.includes('Ver'))).toBe(true);
    expect(botonesDe(filaPropia).some((boton) => boton.textContent?.includes('Eliminar'))).toBe(false);
    expect(botonesDe(filaAjena).some((boton) => boton.textContent?.includes('Eliminar'))).toBe(true);
    fixture.destroy();
  });
});
