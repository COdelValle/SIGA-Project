import { TestBed } from '@angular/core/testing';
import { DiaSemana, formatearFecha } from '@siga/academico';
import { APP_CONFIG } from '@siga/core';
import {
  ClaseDocente,
  CursoDocente,
  DOCENTE_ACTUAL_ID,
  cursosDelDocente,
  horarioDelDocente,
} from '@siga/mocks';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DocenteAcademicoService } from './docente-academico.service';
import { DocenteDatosService } from './docente-datos.service';

const curso: CursoDocente = {
  id: 3,
  nombre: '8° Básico C',
  nivel: 8,
  seccion: 'C',
  asignatura: 'Ciencias Naturales',
  docenteId: 1,
  sala: 'Sala 3',
  diasClase: ['Lunes'],
  alumnos: [],
};

const clase: ClaseDocente = {
  franja: 1,
  cursoId: 3,
  curso: '8° Básico C',
  asignatura: 'Ciencias Naturales',
  sala: 'Sala 3',
};

const horario: Record<DiaSemana, ClaseDocente[]> = {
  Lunes: [clase],
  Martes: [],
  Miércoles: [],
  Jueves: [],
  Viernes: [],
};

const horarioTodosLosDias: Record<DiaSemana, ClaseDocente[]> = {
  Lunes: [clase],
  Martes: [clase],
  Miércoles: [clase],
  Jueves: [clase],
  Viernes: [clase],
};

const horarioVacio: Record<DiaSemana, ClaseDocente[]> = {
  Lunes: [],
  Martes: [],
  Miércoles: [],
  Jueves: [],
  Viernes: [],
};

interface Opciones {
  getCursos?: () => Observable<CursoDocente[] | null>;
  getHorario?: () => Observable<Record<DiaSemana, ClaseDocente[]> | null>;
}

function configurar(useMocks: boolean, opciones: Opciones = {}) {
  const datos = {
    getCursos: vi.fn(opciones.getCursos ?? (() => of<CursoDocente[] | null>([curso]))),
    getHorario: vi.fn(
      opciones.getHorario ?? (() => of<Record<DiaSemana, ClaseDocente[]> | null>(horario)),
    ),
    invalidar: vi.fn(),
  };

  TestBed.configureTestingModule({
    providers: [
      {
        provide: APP_CONFIG,
        useValue: {
          bffBaseUrl: '/api',
          useMocks,
          msal: {
            clientId: '',
            authority: '',
            redirectUri: '',
            postLogoutRedirectUri: '',
            scopes: [],
          },
        },
      },
      { provide: DocenteDatosService, useValue: datos },
    ],
  });

  const service = TestBed.inject(DocenteAcademicoService);
  return { service, datos };
}

describe('DocenteAcademicoService', () => {
  afterEach(() => {
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('expone cursos y horario reales con estado listo', () => {
    const { service, datos } = configurar(false);

    expect(service.estadoCursos()).toBe('listo');
    expect(service.estadoHorario()).toBe('listo');
    expect(service.cursos()).toEqual([curso]);
    expect(service.horario()).toEqual(horario);
    expect(datos.getCursos).toHaveBeenCalledTimes(1);
    expect(datos.getHorario).toHaveBeenCalledTimes(1);
  });

  it('deja los estados en error y listas vacias cuando el BFF falla', () => {
    const { service } = configurar(false, {
      getCursos: () => throwError(() => new Error('boom')),
      getHorario: () => throwError(() => new Error('boom')),
    });

    expect(service.estadoCursos()).toBe('error');
    expect(service.estadoHorario()).toBe('error');
    expect(service.cursos()).toEqual([]);
    expect(service.horario()).toEqual(horarioVacio);
  });

  it('recarga los datos invalidando el cache', () => {
    const { service, datos } = configurar(false);
    const actualizado: CursoDocente = { ...curso, id: 4, nombre: '7° Básico A' };

    datos.getCursos.mockReturnValue(of<CursoDocente[] | null>([actualizado]));
    service.recargarDatos();

    expect(datos.invalidar).toHaveBeenCalledTimes(1);
    expect(datos.getCursos).toHaveBeenCalledTimes(2);
    expect(datos.getHorario).toHaveBeenCalledTimes(2);
    expect(service.cursos()).toEqual([actualizado]);
  });

  it('usa los mocks de cursos y horario en modo demo', () => {
    const { service } = configurar(true, {
      getCursos: () => of<CursoDocente[] | null>([{ ...curso, id: 99 }]),
    });

    expect(service.estadoCursos()).toBe('listo');
    expect(service.estadoHorario()).toBe('listo');
    expect(service.cursos()).toEqual(cursosDelDocente(DOCENTE_ACTUAL_ID));
    expect(service.horario()).toEqual(horarioDelDocente(DOCENTE_ACTUAL_ID));
  });

  it('calcula el dia, las clases de hoy y los cursos con clase', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date(2026, 10, 16, 12, 0, 0));

    const { service } = configurar(false, {
      getHorario: () => of<Record<DiaSemana, ClaseDocente[]> | null>(horarioTodosLosDias),
    });

    expect(service.fechaHoy).toBe('2026-11-16');
    expect(service.diaHoy()).toBe('Lunes');
    expect(service.clasesDeHoy()).toEqual(horarioTodosLosDias.Lunes);
    expect(service.cursosConClaseHoy()).toEqual([curso]);
    expect(service.semestreDe(service.fechaHoy)).toBe(2);
  });

  it('calcula el semestre y la fecha legible', () => {
    const { service } = configurar(false);

    expect(service.fechaHoy).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(service.hoyLegible()).toBe(formatearFecha(service.fechaHoy));
    expect(service.semestreDe('2026-06-30')).toBe(1);
    expect(service.semestreDe('2026-07-01')).toBe(2);
    expect(service.semestreDe('2026-12-31')).toBe(2);
  });
});
