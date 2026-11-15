import { TestBed } from '@angular/core/testing';
import {
  AsistenciaDTO,
  AsistenciaService,
  PerfilEstudianteDTO,
  PerfilEstudianteService,
} from '@siga/academico';
import { APP_CONFIG } from '@siga/core';
import { PUPILOS_MOCK, Pupilo } from '@siga/mocks';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApoderadoStateService } from './apoderado-state.service';
import { ApoderadoService } from './apoderado.service';

const pupilos: Pupilo[] = [
  { id: 1, nombre: 'Camila Soto', relacion: 'Hija', curso: '8° Básico A' },
  { id: 2, nombre: 'Lilith Soto', relacion: 'Hija', curso: '4° Básico B' },
];

const perfil: PerfilEstudianteDTO = {
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
  asignaturas: [],
};

const asistencia: AsistenciaDTO = {
  id: 9,
  idEstudiante: 1,
  idCursoAsignatura: 10,
  fecha: '2026-10-02',
  estado: 'PRESENTE',
  justificacion: 'SI',
  observacion: null,
};

interface Opciones {
  getPupilos?: () => Observable<Pupilo[] | null>;
  getPerfil?: () => Observable<PerfilEstudianteDTO | null>;
  getAsistencias?: () => Observable<AsistenciaDTO[] | null>;
}

function configurar(useMocks: boolean, opciones: Opciones = {}) {
  const apoderadoService = {
    getPupilos: vi.fn(opciones.getPupilos ?? (() => of<Pupilo[] | null>(pupilos))),
    invalidar: vi.fn(),
  };
  const perfilService = {
    getPerfil: vi.fn(
      opciones.getPerfil ?? ((id: number) => of<PerfilEstudianteDTO | null>({ ...perfil, id })),
    ),
    invalidar: vi.fn(),
  };
  const asistenciaService = {
    getAsistenciasEstudiante: vi.fn(
      opciones.getAsistencias ??
        ((id: number) => of<AsistenciaDTO[] | null>([{ ...asistencia, idEstudiante: id }])),
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
      { provide: ApoderadoService, useValue: apoderadoService },
      { provide: PerfilEstudianteService, useValue: perfilService },
      { provide: AsistenciaService, useValue: asistenciaService },
    ],
  });

  const service = TestBed.inject(ApoderadoStateService);
  return { service, apoderadoService, perfilService, asistenciaService };
}

function flushEfectos(): void {
  TestBed.tick();
  TestBed.tick();
  TestBed.tick();
}

describe('ApoderadoStateService', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('carga pupilos y selecciona el primero para consultar perfil y asistencias', () => {
    const { service, perfilService, asistenciaService } = configurar(false);
    flushEfectos();

    expect(service.estadoPupilos()).toBe('listo');
    expect(service.pupilos()).toEqual(pupilos);
    expect(service.pupiloId()).toBe(1);
    expect(perfilService.getPerfil).toHaveBeenLastCalledWith(1);
    expect(service.estadoPerfil()).toBe('listo');
    expect(service.perfil()?.id).toBe(1);
    expect(asistenciaService.getAsistenciasEstudiante).toHaveBeenLastCalledWith(1);
    expect(service.estadoAsistencias()).toBe('listo');
    expect(service.asistencias()).toEqual([{ ...asistencia, idEstudiante: 1 }]);
  });

  it('consulta el perfil y las asistencias del pupilo seleccionado', () => {
    const { service, perfilService, asistenciaService } = configurar(false);
    flushEfectos();

    service.seleccionar(2);
    flushEfectos();

    expect(service.pupiloId()).toBe(2);
    expect(perfilService.getPerfil).toHaveBeenLastCalledWith(2);
    expect(service.perfil()?.id).toBe(2);
    expect(asistenciaService.getAsistenciasEstudiante).toHaveBeenLastCalledWith(2);
    expect(service.asistencias()).toEqual([{ ...asistencia, idEstudiante: 2 }]);
  });

  it('deja los pupilos en estado error y una lista vacia si el BFF falla', () => {
    const { service } = configurar(false, {
      getPupilos: () => throwError(() => new Error('boom')),
    });
    flushEfectos();

    expect(service.estadoPupilos()).toBe('error');
    expect(service.pupilos()).toEqual([]);
    expect(service.pupiloId()).toBe(0);
    expect(service.perfil()).toBeNull();
  });

  it('mantiene el estado listo con una lista vacia cuando no hay pupilos', () => {
    const { service } = configurar(false, { getPupilos: () => of<Pupilo[] | null>([]) });
    flushEfectos();

    expect(service.estadoPupilos()).toBe('listo');
    expect(service.pupilos()).toEqual([]);
    expect(service.pupiloId()).toBe(0);
  });

  it('reintenta pupilos, perfil y asistencias invalidando los caches', () => {
    const { service, apoderadoService, perfilService, asistenciaService } = configurar(false);
    flushEfectos();

    service.reintentar();
    flushEfectos();

    expect(apoderadoService.invalidar).toHaveBeenCalledTimes(1);
    expect(perfilService.invalidar).toHaveBeenCalledTimes(1);
    expect(asistenciaService.invalidar).toHaveBeenCalledTimes(1);
    expect(apoderadoService.getPupilos).toHaveBeenCalledTimes(2);
    expect(perfilService.getPerfil).toHaveBeenCalledWith(1);
    expect(asistenciaService.getAsistenciasEstudiante).toHaveBeenCalledWith(1);
  });

  it('usa los pupilos demo y selecciona el primero en modo demo', () => {
    const { service } = configurar(true);
    flushEfectos();

    expect(service.estadoPupilos()).toBe('listo');
    expect(service.pupilos()).toEqual(PUPILOS_MOCK);
    expect(service.pupiloId()).toBe(PUPILOS_MOCK[0].id);
  });
});
