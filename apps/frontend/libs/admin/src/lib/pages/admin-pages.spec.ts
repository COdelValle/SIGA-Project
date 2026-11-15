import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { APP_CONFIG, AppConfig, MeService } from '@siga/core';
import { AsignaturaAdmin, MallaFila, UsuarioAdmin } from '@siga/mocks';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../state/admin.service';
import { AdminAsignaturasComponent } from './asignaturas.component';
import { AdminInicioComponent } from './inicio.component';
import { AdminMallaComponent } from './malla.component';

function crearConfig(useMocks: boolean): AppConfig {
  return {
    bffBaseUrl: '/api',
    useMocks,
    msal: { clientId: '', authority: '', redirectUri: '', postLogoutRedirectUri: '', scopes: [] },
  };
}

interface Opciones {
  useMocks?: boolean;
  usuarios?: () => Observable<UsuarioAdmin[] | null>;
  asignaturas?: () => Observable<AsignaturaAdmin[] | null>;
  malla?: () => Observable<MallaFila[] | null>;
  me?: { id: string; email: string; displayName: string; roles: string[] } | null;
}

function configurar<T>(componente: Type<T>, opciones: Opciones = {}) {
  const adminService = {
    getUsuarios: vi.fn(opciones.usuarios ?? (() => of<UsuarioAdmin[] | null>([]))),
    getAsignaturas: vi.fn(opciones.asignaturas ?? (() => of<AsignaturaAdmin[] | null>([]))),
    getMalla: vi.fn(opciones.malla ?? (() => of<MallaFila[] | null>([]))),
    invalidar: vi.fn(),
  };
  const meService = {
    getMe: () =>
      of(
        opciones.me === null
          ? null
          : (opciones.me ?? {
              id: 'oid-admin',
              email: 'admin@siga.cl',
              displayName: 'Administradora SIGA',
              roles: ['ADMIN'],
            }),
      ),
  };

  TestBed.configureTestingModule({
    imports: [componente],
    providers: [
      provideRouter([]),
      { provide: APP_CONFIG, useValue: crearConfig(opciones.useMocks ?? true) },
      { provide: AdminService, useValue: adminService },
      { provide: MeService, useValue: meService },
    ],
  });

  const fixture = TestBed.createComponent(componente);
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

function boton(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.textContent?.trim() === etiqueta) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

const mallaPersonalizada: MallaFila[] = [
  {
    id: 1,
    nivel: '1ro Básico',
    idAsignatura: 1,
    nombre: 'Lenguaje',
    area: 'Lenguaje',
    caracter: 'OBLIGATORIA',
    plan: 'COMUN',
    horasSemanales: 6,
    calificable: true,
    activa: true,
  },
  {
    id: 2,
    nivel: '1ro Básico',
    idAsignatura: 2,
    nombre: 'Música',
    area: 'Artes',
    caracter: 'ELECTIVA',
    plan: 'COMUN',
    horasSemanales: null,
    calificable: false,
    activa: true,
  },
];

const asignaturasPersonalizadas: AsignaturaAdmin[] = [
  {
    id: 1,
    nombre: 'Lenguaje',
    area: 'Lenguaje',
    calificable: true,
    niveles: ['1ro Básico'],
    activa: true,
  },
  {
    id: 2,
    nombre: 'Orientación',
    area: 'Formación',
    calificable: false,
    niveles: [],
    activa: false,
  },
];

describe('páginas de administración', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('malla en modo demo muestra las filas del nivel y cambia de nivel', async () => {
    const { fixture } = configurar(AdminMallaComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Malla curricular');
    expect(texto(fixture)).toContain('Obligatoria');
    expect(filas(fixture).length).toBeGreaterThan(0);

    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'select',
    ) as HTMLSelectElement;
    selector.value = '1ro Medio';
    selector.dispatchEvent(new Event('change'));
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Electiva');
    fixture.destroy();
  });

  it('malla en modo real formatea las horas y el carácter de cada fila', async () => {
    const { fixture } = configurar(AdminMallaComponent, {
      useMocks: false,
      malla: () => of(mallaPersonalizada),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Lenguaje');
    expect(texto(fixture)).toContain('6');
    expect(texto(fixture)).toContain('Calificable');
    expect(texto(fixture)).toContain('Electiva');
    expect(texto(fixture)).toContain('Sin calificación');
    expect(filas(fixture).length).toBe(2);
    fixture.destroy();
  });

  it('malla muestra el estado vacío para un nivel sin asignaturas', async () => {
    const { fixture } = configurar(AdminMallaComponent, {
      useMocks: false,
      malla: () => of([]),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Sin asignaturas para este nivel.');
    fixture.destroy();
  });

  it('malla muestra el error de carga e invalida el cache al reintentar', async () => {
    const { fixture, adminService } = configurar(AdminMallaComponent, {
      useMocks: false,
      malla: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(adminService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('asignaturas en modo demo lista el catálogo y pagina los resultados', async () => {
    const { fixture } = configurar(AdminAsignaturasComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Asignaturas');
    expect(texto(fixture)).toContain('Lenguaje y Comunicación');
    expect(texto(fixture)).toContain('1ro Básico – 6to Básico');
    expect(texto(fixture)).toContain('Mostrando 1–10 de 22');

    boton(fixture, 'Siguiente').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Mostrando 11–20 de 22');
    expect(filas(fixture).length).toBe(10);
    fixture.destroy();
  });

  it('asignaturas en modo real muestra un nivel único y el guion sin niveles', async () => {
    const { fixture } = configurar(AdminAsignaturasComponent, {
      useMocks: false,
      asignaturas: () => of(asignaturasPersonalizadas),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Lenguaje');
    expect(texto(fixture)).toContain('Sí');
    expect(texto(fixture)).toContain('Activa');
    expect(texto(fixture)).toContain('Orientación');
    expect(texto(fixture)).toContain('Sin calificación');
    expect(texto(fixture)).toContain('Inactiva');
    expect(texto(fixture)).toContain('—');
    fixture.destroy();
  });

  it('asignaturas muestra el estado vacío y el error con reintento', async () => {
    const { fixture, adminService } = configurar(AdminAsignaturasComponent, {
      useMocks: false,
      asignaturas: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(adminService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('inicio en modo demo saluda y resume los usuarios por rol', async () => {
    const { fixture } = configurar(AdminInicioComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Bienvenido(a),');
    expect(texto(fixture)).toContain('Administradora SIGA');
    expect(texto(fixture)).toContain('ADMIN');
    expect(texto(fixture)).toContain('DOCENTE');
    expect(texto(fixture)).toContain('Ver usuarios');

    const enlaces = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a'),
    ) as HTMLAnchorElement[];
    expect(enlaces.some((enlace) => enlace.getAttribute('href')?.includes('/admin/usuarios'))).toBe(
      true,
    );
    fixture.destroy();
  });

  it('inicio en modo real cuenta los usuarios recibidos del BFF', async () => {
    const usuarios: UsuarioAdmin[] = [
      { id: '1', nombre: 'Admin', email: 'admin@test.com', rol: 'ADMIN', estado: 'ACTIVO' },
      { id: '2', nombre: 'Docente', email: 'docente@test.com', rol: 'DOCENTE', estado: 'ACTIVO' },
      { id: '3', nombre: 'Docente 2', email: 'docente2@test.com', rol: 'DOCENTE', estado: 'INACTIVO' },
    ];
    const { fixture } = configurar(AdminInicioComponent, {
      useMocks: false,
      usuarios: () => of(usuarios),
    });
    await esperar(fixture);

    const articulos = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('article'),
    ) as HTMLElement[];
    expect(articulos.length).toBe(4);
    const docente = articulos.find((articulo) => articulo.textContent?.includes('DOCENTE'));
    expect(docente?.textContent).toContain('2');
    fixture.destroy();
  });

  it('inicio maneja el error de carga y el perfil sin nombre', async () => {
    const { fixture, adminService } = configurar(AdminInicioComponent, {
      useMocks: false,
      me: null,
      usuarios: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Bienvenido(a),');
    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(adminService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });
});
