import { Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import {
  AsistenciaDTO,
  AsistenciaService,
  PerfilEstudianteDTO,
  PerfilEstudianteService,
  diaActual,
} from '@siga/academico';
import { APP_CONFIG, Me, MeService } from '@siga/core';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { EstudianteAsistenciaHistorialComponent } from './asistencia-historial.component';
import { EstudianteAsistenciasComponent } from './asistencias.component';
import { EstudianteHorariosComponent } from './horarios.component';
import { EstudianteInicioComponent } from './inicio.component';
import { EstudianteNotasComponent } from './notas.component';
import { EstudianteProgresoComponent } from './progreso.component';

const ME: Me = {
  id: 'oid-camila',
  email: 'camila.soto@siga.cl',
  displayName: 'Camila Soto',
  roles: ['ESTUDIANTE'],
};

function crearPerfil(): PerfilEstudianteDTO {
  return {
    id: 1,
    idUsuario: 'oid-camila',
    rut: '12345678-9',
    firstName: 'Camila',
    middleName: null,
    firstSurname: 'Soto',
    secondSurname: 'Hernández',
    birthDate: '2012-04-01',
    allergies: [],
    state: 'ACTIVO',
    idClase: 3,
    clase: { id: 3, nivel: '8vo Básico', letra: 'A', anioAcademico: 2026 },
    asignaturas: [
      {
        id: 10,
        idAsignatura: 1,
        name: 'Matemática',
        description: '',
        area: 'Ciencias',
        caracter: 'OBLIGATORIA',
        calificable: true,
        idDocente: 2,
        docente: 'ANA PÉREZ',
        horarios: [
          {
            id: 1,
            dia: 'Lunes',
            horarioEntrada: '08:00',
            horarioSalida: '08:45',
            ubicacion: 'Sala 3',
          },
        ],
        evaluaciones: [
          { id: 1, nombre: 'PRUEBA 1', tipo: 'SUMATIVA', ponderacion: 100, nota: 6.5 },
        ],
      },
      {
        id: 11,
        idAsignatura: 2,
        name: 'Orientación',
        description: '',
        area: 'Formación',
        caracter: 'OBLIGATORIA',
        calificable: false,
        idDocente: 3,
        docente: null,
        horarios: [],
        evaluaciones: [],
      },
    ],
  };
}

const ASISTENCIAS: AsistenciaDTO[] = [
  {
    id: 1,
    idEstudiante: 1,
    idCursoAsignatura: 10,
    fecha: '2026-03-02',
    estado: 'PRESENTE',
    justificacion: 'NO_APLICA',
    observacion: null,
  },
  {
    id: 2,
    idEstudiante: 1,
    idCursoAsignatura: 10,
    fecha: '2026-03-03',
    estado: 'AUSENTE',
    justificacion: 'SI',
    observacion: 'Certificado médico',
  },
  {
    id: 3,
    idEstudiante: 1,
    idCursoAsignatura: 10,
    fecha: '2026-03-04',
    estado: 'PRESENTE',
    justificacion: 'NO_APLICA',
    observacion: null,
  },
];

type Parametros = Record<string, string>;

function mapa(valores: Parametros) {
  return {
    get: (clave: string) => valores[clave] ?? null,
    keys: Object.keys(valores),
  };
}

function crearRoute(params: Parametros = {}, query: Parametros = {}): Partial<ActivatedRoute> {
  const paramMap = mapa(params);
  const queryParamMap = mapa(query);
  return {
    snapshot: { paramMap, queryParamMap } as unknown as ActivatedRoute['snapshot'],
    paramMap: of(paramMap) as unknown as ActivatedRoute['paramMap'],
    queryParamMap: of(queryParamMap) as unknown as ActivatedRoute['queryParamMap'],
  };
}

function crearConfig(useMocks: boolean) {
  return {
    bffBaseUrl: '/api',
    useMocks,
    msal: {
      clientId: '',
      authority: '',
      redirectUri: '',
      postLogoutRedirectUri: '',
      scopes: [],
    },
  };
}

interface Opciones {
  useMocks?: boolean;
  params?: Parametros;
  query?: Parametros;
  perfil?: () => Observable<PerfilEstudianteDTO | null>;
  asistencias?: () => Observable<AsistenciaDTO[] | null>;
}

function configurar<T>(componente: Type<T>, opciones: Opciones = {}) {
  const perfilService = {
    getPerfilMe: vi.fn(opciones.perfil ?? (() => of<PerfilEstudianteDTO | null>(crearPerfil()))),
    invalidar: vi.fn(),
  };
  const asistenciaService = {
    getAsistenciasMe: vi.fn(
      opciones.asistencias ?? (() => of<AsistenciaDTO[] | null>(ASISTENCIAS)),
    ),
    invalidar: vi.fn(),
  };

  TestBed.configureTestingModule({
    imports: [componente],
    providers: [
      provideRouter([]),
      { provide: APP_CONFIG, useValue: crearConfig(opciones.useMocks ?? true) },
      { provide: MeService, useValue: { getMe: () => of(ME) } },
      { provide: PerfilEstudianteService, useValue: perfilService },
      { provide: AsistenciaService, useValue: asistenciaService },
      { provide: ActivatedRoute, useValue: crearRoute(opciones.params, opciones.query) },
    ],
  });

  const fixture = TestBed.createComponent(componente);
  fixture.detectChanges();
  return { fixture, perfilService, asistenciaService };
}

async function esperar(fixture: ComponentFixture<unknown>): Promise<void> {
  for (let intento = 0; intento < 4; intento += 1) {
    TestBed.tick();
    await fixture.whenStable();
    await Promise.resolve();
  }
  fixture.detectChanges();
}

function texto(fixture: ComponentFixture<unknown>): string {
  return (fixture.nativeElement as HTMLElement).textContent ?? '';
}

function boton(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.textContent?.includes(etiqueta)) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

describe('paginas del portal estudiante', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('inicio en modo demo muestra el nombre, los resumenes y cambia de dia', async () => {
    const { fixture } = configurar(EstudianteInicioComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Bienvenido(a)');
    expect(texto(fixture)).toContain('Camila Soto');
    expect(texto(fixture)).toContain('Ver horarios');
    expect(texto(fixture)).toContain('Ver asistencias');
    expect(texto(fixture)).toContain('Ver notas');

    const enlaces = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('a'),
    ) as HTMLAnchorElement[];
    expect(enlaces.some((enlace) => enlace.getAttribute('href')?.includes('/estudiante/horarios'))).toBe(
      true,
    );

    boton(fixture, 'Martes').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Tecnología');
    fixture.destroy();
  });

  it('inicio en modo real usa el perfil y las asistencias del BFF', async () => {
    const { fixture } = configurar(EstudianteInicioComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Camila Soto');
    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('67%');
    expect(texto(fixture)).toContain('6,5');
    fixture.destroy();
  });

  it('inicio muestra el estado de error y reintenta invalidando el cache', async () => {
    const { fixture, perfilService } = configurar(EstudianteInicioComponent, {
      useMocks: false,
      perfil: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(perfilService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('notas en modo demo cambia de semestre y muestra la leyenda de calculo', async () => {
    const { fixture } = configurar(EstudianteNotasComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8° Básico A');
    expect(texto(fixture)).toContain('Promedio del semestre');
    expect(texto(fixture)).toContain('Cálculo: promedio ponderado');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Semestre 1');
    fixture.destroy();
  });

  it('notas en modo real muestra las notas del perfil y el semestre vacio', async () => {
    const { fixture } = configurar(EstudianteNotasComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8vo Básico A');
    expect(texto(fixture)).toContain('6,5');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Sin notas registradas.');
    fixture.destroy();
  });

  it('notas muestra error si fallan las asistencias y permite reintentar', async () => {
    const { fixture, asistenciaService } = configurar(EstudianteNotasComponent, {
      useMocks: false,
      asistencias: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(asistenciaService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('asistencias en modo real muestra la tabla con el porcentaje', async () => {
    const { fixture } = configurar(EstudianteAsistenciasComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Asistencia');
    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('67%');
    expect(texto(fixture)).toContain('Ver Historial');
    fixture.destroy();
  });

  it('asistencias con perfil sin asignaturas muestra el estado vacio', async () => {
    const { fixture } = configurar(EstudianteAsistenciasComponent, {
      useMocks: false,
      perfil: () => of<PerfilEstudianteDTO | null>({ ...crearPerfil(), asignaturas: [] }),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Sin datos de asistencia.');
    fixture.destroy();
  });

  it('asistencias muestra error y reintenta al pulsar el boton', async () => {
    const { fixture, perfilService } = configurar(EstudianteAsistenciasComponent, {
      useMocks: false,
      perfil: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(perfilService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('horarios parte con el dia del query param y navega al cambiar de dia', async () => {
    const { fixture } = configurar(EstudianteHorariosComponent, {
      useMocks: true,
      query: { dia: 'Martes' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Día: Martes');
    expect(texto(fixture)).toContain('Tecnología');

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    boton(fixture, 'Jueves').click();
    await esperar(fixture);

    expect(navegar).toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: { dia: 'Jueves' },
      queryParamsHandling: 'merge',
    });
    expect(texto(fixture)).toContain('Día: Jueves');
    expect(texto(fixture)).toContain('Artes Visuales');
    fixture.destroy();
  });

  it('horarios normaliza un dia invalido y usa el dia actual', async () => {
    const { fixture } = configurar(EstudianteHorariosComponent, {
      useMocks: false,
      query: { dia: 'Domingo' },
      perfil: () => of<PerfilEstudianteDTO | null>({ ...crearPerfil(), asignaturas: [] }),
    });
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Día: Domingo');
    expect(texto(fixture)).toContain(`Día: ${diaActual()}`);
    expect(texto(fixture)).toContain('Sin bloques para este día.');
    fixture.destroy();
  });

  it('horarios en modo real muestra los bloques del perfil para el dia elegido', async () => {
    const { fixture } = configurar(EstudianteHorariosComponent, {
      useMocks: false,
      query: { dia: 'Lunes' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Día: Lunes');
    expect(texto(fixture)).toContain('08:00 - 08:45');
    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('Ana Pérez');
    expect(texto(fixture)).toContain('Sala 3');
    fixture.destroy();
  });

  it('horarios muestra error y reintenta', async () => {
    const { fixture, perfilService } = configurar(EstudianteHorariosComponent, {
      useMocks: false,
      perfil: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(perfilService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('progreso en modo demo cambia de periodo con el selector', async () => {
    const { fixture } = configurar(EstudianteProgresoComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Progreso Académico');
    expect(texto(fixture)).toContain('2026 · 8° Básico A');
    expect(texto(fixture)).toContain('En curso');
    expect(texto(fixture)).toContain('Cálculo: promedio ponderado por la ponderación');

    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'siga-select button',
    ) as HTMLButtonElement;
    selector.click();
    fixture.detectChanges();

    const opcion = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('li button'),
    ).find((item) => item.textContent?.includes('2025')) as HTMLButtonElement;
    opcion.click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('2025 · 8° Básico A');
    expect(texto(fixture)).toContain('Finalizado');
    fixture.destroy();
  });

  it('progreso en modo real muestra un periodo y permite volver al semestre 1', async () => {
    const { fixture } = configurar(EstudianteProgresoComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8vo Básico A');
    expect(texto(fixture)).toContain('6,5');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Sin notas registradas.');
    expect(texto(fixture)).toContain('0,0');
    fixture.destroy();
  });

  it('progreso muestra error y reintenta', async () => {
    const { fixture, asistenciaService } = configurar(EstudianteProgresoComponent, {
      useMocks: false,
      asistencias: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(asistenciaService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('asistencia-historial en modo demo pagina los registros del query param', async () => {
    const { fixture } = configurar(EstudianteAsistenciaHistorialComponent, {
      useMocks: true,
      params: { id: '1' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Historia, Geografía y Ciencias Sociales');
    const filas = () =>
      (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length;
    expect(filas()).toBe(10);
    expect(texto(fixture)).toContain('Mostrando 1–10 de 60');

    boton(fixture, 'Siguiente').click();
    fixture.detectChanges();

    expect(filas()).toBe(10);
    expect(texto(fixture)).toContain('Mostrando 11–20 de 60');
    fixture.destroy();
  });

  it('asistencia-historial muestra el detalle del perfil real', async () => {
    const { fixture } = configurar(EstudianteAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '10' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('02/03/2026');
    expect(texto(fixture)).toContain('Presente');
    expect(texto(fixture)).toContain('Inasistencia');
    expect(texto(fixture)).toContain('Sí');
    fixture.destroy();
  });

  it('asistencia-historial con id desconocido muestra el estado vacio', async () => {
    const { fixture } = configurar(EstudianteAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '999' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Asignatura');
    expect(texto(fixture)).toContain('Sin registros de asistencia.');
    fixture.destroy();
  });

  it('asistencia-historial muestra error y reintenta', async () => {
    const { fixture, perfilService } = configurar(EstudianteAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '10' },
      perfil: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(perfilService.invalidar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });
});
