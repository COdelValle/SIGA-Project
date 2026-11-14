import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { AsistenciaDTO, DiaSemana } from '@siga/academico';
import { APP_CONFIG, EstadoRemoto } from '@siga/core';
import { CursoDocente } from '@siga/mocks';
import { Observable, Subject, of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AsistenciaService } from '@siga/academico';
import { DocenteAcademicoService } from '../state/docente-academico.service';
import { DocenteRegistrarAsistenciasComponent } from './registrar-asistencias.component';

const cursoA: CursoDocente = {
  id: 3,
  nombre: '8vo A',
  nivel: 8,
  seccion: 'A',
  asignatura: 'Ciencias Naturales',
  docenteId: 1,
  sala: 'S1',
  diasClase: ['Lunes'],
  alumnos: [
    { id: 1, nombres: 'Ana', firstSurname: 'Pérez', secondSurname: 'Soto' },
    { id: 2, nombres: 'Bruno', firstSurname: 'Díaz', secondSurname: 'Rojas' },
  ],
};

const cursoB: CursoDocente = {
  id: 4,
  nombre: '7mo B',
  nivel: 7,
  seccion: 'B',
  asignatura: 'Física',
  docenteId: 1,
  sala: 'S2',
  diasClase: ['Martes'],
  alumnos: [{ id: 5, nombres: 'Carla', firstSurname: 'Muñoz', secondSurname: 'Vega' }],
};

const registroAna: AsistenciaDTO = {
  id: 11,
  idEstudiante: 1,
  idCursoAsignatura: 3,
  fecha: '2026-11-16',
  estado: 'PRESENTE',
  justificacion: 'SI',
  observacion: null,
};

function crearRespuesta(request: {
  idEstudiante: number;
  idCursoAsignatura: number;
  fecha: string;
  estado: AsistenciaDTO['estado'];
}): AsistenciaDTO {
  return {
    id: 99,
    idEstudiante: request.idEstudiante,
    idCursoAsignatura: request.idCursoAsignatura,
    fecha: request.fecha,
    estado: request.estado,
    justificacion: request.estado === 'PRESENTE' ? 'NO_APLICA' : 'PENDIENTE',
    observacion: null,
  };
}

function crearConfig() {
  return {
    bffBaseUrl: '/api',
    useMocks: false,
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
  cursos?: CursoDocente[];
  estadoHorario?: EstadoRemoto;
  asistencias?: () => Observable<AsistenciaDTO[]>;
  registrar?: (request: {
    idEstudiante: number;
    idCursoAsignatura: number;
    fecha: string;
    estado: AsistenciaDTO['estado'];
  }) => Observable<AsistenciaDTO>;
  actualizar?: (id: number, request: unknown) => Observable<AsistenciaDTO>;
}

function configurar(opciones: Opciones = {}) {
  const academico = {
    cursosConClaseHoy: signal<CursoDocente[]>(opciones.cursos ?? [cursoA, cursoB]),
    estadoHorario: signal<EstadoRemoto>(opciones.estadoHorario ?? 'listo'),
    diaHoy: signal<DiaSemana | null>('Lunes'),
    fechaHoy: '2026-11-16',
    hoyLegible: () => '16/11/2026',
    semestreActual: signal<1 | 2>(2),
    recargarDatos: vi.fn(),
  };
  const asistenciaService = {
    getAsistenciasAsignatura: vi.fn(() => opciones.asistencias?.() ?? of<AsistenciaDTO[]>([])),
    registrar: vi.fn(
      (request: Parameters<NonNullable<Opciones['registrar']>>[0]) =>
        opciones.registrar?.(request) ?? of(crearRespuesta(request)),
    ),
    actualizar: vi.fn((id: number, request: unknown) => {
      if (opciones.actualizar) {
        return opciones.actualizar(id, request);
      }
      const datos = request as {
        justificacion: AsistenciaDTO['justificacion'];
        observacion: string | null;
        estado?: AsistenciaDTO['estado'] | null;
      };
      return of<AsistenciaDTO>({
        id,
        idEstudiante: 1,
        idCursoAsignatura: 3,
        fecha: '2026-11-16',
        estado: datos.estado ?? 'PRESENTE',
        justificacion: datos.justificacion,
        observacion: datos.observacion,
      });
    }),
    invalidar: vi.fn(),
  };

  TestBed.configureTestingModule({
    imports: [DocenteRegistrarAsistenciasComponent],
    providers: [
      { provide: APP_CONFIG, useValue: crearConfig() },
      { provide: DocenteAcademicoService, useValue: academico },
      { provide: AsistenciaService, useValue: asistenciaService },
    ],
  });

  const fixture = TestBed.createComponent(DocenteRegistrarAsistenciasComponent);
  fixture.detectChanges();
  return { fixture, academico, asistenciaService };
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

function fila(fixture: ComponentFixture<unknown>, indice: number): HTMLElement {
  return (fixture.nativeElement as HTMLElement).querySelectorAll('li')[indice] as HTMLElement;
}

function botonDe(filaAlumno: HTMLElement, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(filaAlumno.querySelectorAll('button')).find((item) =>
    item.textContent?.includes(etiqueta),
  ) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
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

describe('DocenteRegistrarAsistenciasComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('carga la fecha, el curso y los alumnos con sus registros', async () => {
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([registroAna]),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Hoy es Lunes 16/11/2026 · Semestre 2');
    expect(texto(fixture)).toContain('8vo A · Ciencias Naturales');
    expect(texto(fixture)).toContain('Ana Pérez Soto');
    expect(texto(fixture)).toContain('Bruno Díaz Rojas');
    expect(texto(fixture)).toContain('Presente · Justificada');
    expect(texto(fixture)).toContain('Sin marcar');
    expect(asistenciaService.getAsistenciasAsignatura).toHaveBeenCalledWith(3, '2026-11-16');
    fixture.destroy();
  });

  it('registra la asistencia del alumno sin marcar y actualiza la fila', async () => {
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([registroAna]),
    });
    await esperar(fixture);

    botonDe(fila(fixture, 1), 'Ausente').click();
    await esperar(fixture);

    expect(asistenciaService.registrar).toHaveBeenCalledWith({
      idEstudiante: 2,
      idCursoAsignatura: 3,
      fecha: '2026-11-16',
      estado: 'AUSENTE',
    });
    expect(texto(fixture)).toContain('Ausente · Pendiente');
    fixture.destroy();
  });

  it('actualiza la asistencia existente y no repite el mismo estado', async () => {
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([registroAna]),
    });
    await esperar(fixture);

    botonDe(fila(fixture, 0), 'Presente').click();
    await esperar(fixture);
    expect(asistenciaService.actualizar).not.toHaveBeenCalled();
    expect(asistenciaService.registrar).not.toHaveBeenCalled();

    botonDe(fila(fixture, 0), 'Ausente').click();
    await esperar(fixture);

    expect(asistenciaService.actualizar).toHaveBeenCalledWith(11, {
      justificacion: 'PENDIENTE',
      observacion: null,
      estado: 'AUSENTE',
    });
    expect(texto(fixture)).toContain('Ausente · Pendiente');
    fixture.destroy();
  });

  it('muestra el aviso de guardado y bloquea el selector mientras espera', async () => {
    const pendiente = new Subject<AsistenciaDTO>();
    const { fixture } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([]),
      registrar: () => pendiente.asObservable(),
    });
    await esperar(fixture);

    botonDe(fila(fixture, 0), 'Presente').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Guardando…');
    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'siga-select button',
    ) as HTMLButtonElement;
    expect(selector.disabled).toBe(true);

    pendiente.next({
      id: 77,
      idEstudiante: 1,
      idCursoAsignatura: 3,
      fecha: '2026-11-16',
      estado: 'PRESENTE',
      justificacion: 'NO_APLICA',
      observacion: null,
    });
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Guardando…');
    expect(texto(fixture)).toContain('Presente · No aplica');
    fixture.destroy();
  });

  it('muestra el mensaje de error del BFF y permite reintentar la marcacion', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo guardar la asistencia' },
      status: 500,
      statusText: 'Server Error',
    });
    let intento = 0;
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([]),
      registrar: (request) => {
        intento += 1;
        return intento === 1 ? throwError(() => error) : of(crearRespuesta(request));
      },
    });
    await esperar(fixture);

    botonDe(fila(fixture, 0), 'Presente').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo guardar la asistencia');
    expect(texto(fixture)).toContain('Reintentar');

    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(asistenciaService.registrar).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).not.toContain('No se pudo guardar la asistencia');
    expect(texto(fixture)).toContain('Presente · No aplica');
    fixture.destroy();
  });

  it('ante un conflicto 409 recarga las asistencias sin mostrar error', async () => {
    const conflicto = new HttpErrorResponse({ status: 409, statusText: 'Conflict' });
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([]),
      registrar: () => throwError(() => conflicto),
    });
    await esperar(fixture);

    botonDe(fila(fixture, 0), 'Ausente').click();
    await esperar(fixture);

    expect(asistenciaService.getAsistenciasAsignatura).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).not.toContain('No se pudo guardar');
    expect(texto(fixture)).not.toContain('Reintentar');
    fixture.destroy();
  });

  it('muestra el estado de carga mientras llegan las asistencias', async () => {
    const pendiente = new Subject<AsistenciaDTO[]>();
    const { fixture } = configurar({ asistencias: () => pendiente.asObservable() });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Cargando asistencias…');

    pendiente.next([registroAna]);
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Cargando asistencias…');
    expect(texto(fixture)).toContain('Presente · Justificada');
    fixture.destroy();
  });

  it('muestra error al cargar asistencias y recarga con el boton', async () => {
    let intento = 0;
    const { fixture, asistenciaService } = configurar({
      asistencias: () => {
        intento += 1;
        return intento === 1
          ? throwError(() => new Error('boom'))
          : of<AsistenciaDTO[]>([registroAna]);
      },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar las asistencias de hoy.');

    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(asistenciaService.getAsistenciasAsignatura).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).not.toContain('No se pudieron cargar las asistencias de hoy.');
    expect(texto(fixture)).toContain('Presente · Justificada');
    fixture.destroy();
  });

  it('muestra el detalle de estados no mapeados usando su valor original', async () => {
    const { fixture } = configurar({
      asistencias: () =>
        of<AsistenciaDTO[]>([
          {
            ...registroAna,
            estado: 'ATRASADO',
            justificacion: 'OTRO' as AsistenciaDTO['justificacion'],
          },
        ]),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Atrasado · OTRO');
    fixture.destroy();
  });

  it('sin clases hoy muestra el aviso correspondiente', async () => {
    const { fixture } = configurar({ cursos: [] });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Sin clases hoy');
    expect(texto(fixture)).toContain('no hay asistencia por registrar');
    fixture.destroy();
  });

  it('si falla el horario muestra el error y recarga los datos', async () => {
    const { fixture, academico } = configurar({ estadoHorario: 'error' });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo cargar el horario');
    boton(fixture, 'Reintentar').click();

    expect(academico.recargarDatos).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('cambia de curso desde el selector y recarga sus asistencias', async () => {
    const { fixture, asistenciaService } = configurar({
      asistencias: () => of<AsistenciaDTO[]>([]),
    });
    await esperar(fixture);

    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'siga-select button',
    ) as HTMLButtonElement;
    selector.click();
    fixture.detectChanges();

    const opcion = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('li button'),
    ).find((item) => item.textContent?.includes('7mo B')) as HTMLButtonElement;
    opcion.click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('7mo B · Física');
    expect(texto(fixture)).toContain('Carla Muñoz Vega');
    expect(asistenciaService.getAsistenciasAsignatura).toHaveBeenCalledWith(4, '2026-11-16');
    fixture.destroy();
  });
});
