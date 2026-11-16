import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import { afterEach, describe, expect, it } from 'vitest';
import {
  CursoNotas,
  DocenteNotasService,
  EvaluacionNotas,
  NotaCurso,
} from './docente-notas.service';

function configurar(useMocks: boolean): void {
  TestBed.configureTestingModule({
    providers: [
      provideHttpClient(),
      provideHttpClientTesting(),
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
    ],
  });
}

describe('DocenteNotasService (modo real)', () => {
  let service: DocenteNotasService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(false);
    service = TestBed.inject(DocenteNotasService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtiene el curso con evaluaciones y notas del BFF', () => {
    let curso: unknown;
    service.getCursoNotas(3).subscribe((valor) => (curso = valor));

    const req = http.expectOne('/api/bff/v1/docentes/cursos/3/notas');
    expect(req.request.method).toBe('GET');
    req.flush({ asignaturaId: 3, curso: '8vo A', asignatura: 'CIENCIAS', evaluaciones: [], alumnos: [] });

    expect(curso).toEqual({
      asignaturaId: 3,
      curso: '8vo A',
      asignatura: 'CIENCIAS',
      evaluaciones: [],
      alumnos: [],
    });
  });

  it('crea una nota con el body esperado', () => {
    let creada: unknown;
    service.crearNota(1, 9, 6.5).subscribe((valor) => (creada = valor));

    const req = http.expectOne('/api/bff/v1/notas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ idEstudiante: 1, idEvaluacion: 9, score: 6.5 });
    req.flush({ id: 20, idEvaluacion: 9, score: 6.5 });

    expect(creada).toEqual({ id: 20, idEvaluacion: 9, score: 6.5 });
  });

  it('edita y elimina notas por id', () => {
    service.editarNota(20, 7).subscribe();
    const put = http.expectOne('/api/bff/v1/notas/20');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ score: 7 });
    put.flush({ id: 20, idEvaluacion: 9, score: 7 });

    service.eliminarNota(20).subscribe();
    const del = http.expectOne('/api/bff/v1/notas/20');
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('gestiona evaluaciones por el BFF', () => {
    service.crearEvaluacion(3, { nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 40 }).subscribe();
    const post = http.expectOne('/api/bff/v1/evaluaciones');
    expect(post.request.body).toEqual({
      nombre: 'PRUEBA',
      tipo: 'SUMATIVA',
      ponderacion: 40,
      idCursoAsignatura: 3,
    });
    post.flush({ id: 9, nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 40 });

    service.editarEvaluacion(9, { nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 35 }).subscribe();
    const put = http.expectOne('/api/bff/v1/evaluaciones/9');
    expect(put.request.body).toEqual({ nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 35 });
    put.flush({ id: 9, nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 35 });

    service.eliminarEvaluacion(9).subscribe();
    const del = http.expectOne('/api/bff/v1/evaluaciones/9');
    expect(del.request.method).toBe('DELETE');
    del.flush(null, { status: 204, statusText: 'No Content' });
  });

  it('normaliza el tipo cuando el BFF devuelve el texto visible legacy', () => {
    let curso: CursoNotas | undefined;
    service.getCursoNotas(3).subscribe((valor) => (curso = valor));

    http.expectOne('/api/bff/v1/docentes/cursos/3/notas').flush({
      asignaturaId: 3,
      curso: '8vo A',
      asignatura: 'CIENCIAS',
      evaluaciones: [
        { id: 1, nombre: 'PRUEBA 1', tipo: 'Evaluación Sumativa', ponderacion: 30 },
        { id: 2, nombre: 'TRABAJO 1', tipo: 'Evaluación Formativa', ponderacion: 0 },
        { id: 3, nombre: 'INICIAL', tipo: 'Evaluación Diagnóstica', ponderacion: 0 },
      ],
      alumnos: [],
    });

    expect(curso?.evaluaciones.map((evaluacion) => evaluacion.tipo)).toEqual([
      'SUMATIVA',
      'FORMATIVA',
      'DIAGNOSTICO',
    ]);
  });

  it('normaliza el tipo en las respuestas de crear y editar evaluación', () => {
    let creada: EvaluacionNotas | undefined;
    service.crearEvaluacion(3, { nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 40 }).subscribe(
      (valor) => (creada = valor),
    );
    http
      .expectOne('/api/bff/v1/evaluaciones')
      .flush({ id: 9, nombre: 'PRUEBA', tipo: 'Evaluación Sumativa', ponderacion: 40 });
    expect(creada?.tipo).toBe('SUMATIVA');

    let editada: EvaluacionNotas | undefined;
    service.editarEvaluacion(9, { nombre: 'PRUEBA', tipo: 'SUMATIVA', ponderacion: 35 }).subscribe(
      (valor) => (editada = valor),
    );
    http
      .expectOne('/api/bff/v1/evaluaciones/9')
      .flush({ id: 9, nombre: 'PRUEBA', tipo: 'Evaluación Sumativa', ponderacion: 35 });
    expect(editada?.tipo).toBe('SUMATIVA');
  });

  it('propaga el error sin caer a mocks', () => {
    let fallo = false;
    service.getCursoNotas(3).subscribe({ error: () => (fallo = true) });

    http
      .expectOne('/api/bff/v1/docentes/cursos/3/notas')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(fallo).toBe(true);
  });
});

describe('DocenteNotasService (modo demo)', () => {
  it('sirve datos demo sin llamar al BFF', () => {
    configurar(true);
    const service = TestBed.inject(DocenteNotasService);
    const http = TestBed.inject(HttpTestingController);

    let evaluaciones = 0;
    service.getCursoNotas(3).subscribe((curso) => (evaluaciones = curso.evaluaciones.length));

    expect(evaluaciones).toBe(3);
    http.expectNone('/api/bff/v1/docentes/cursos/3/notas');
  });
});

describe('DocenteNotasService (modo demo, operaciones)', () => {
  afterEach(() => TestBed.resetTestingModule());

  function servicioDemo() {
    configurar(true);
    return TestBed.inject(DocenteNotasService);
  }

  it('crea, edita y elimina notas sobre el set demo', () => {
    const service = servicioDemo();
    let curso: CursoNotas | undefined;
    service.getCursoNotas(3).subscribe((valor) => (curso = valor));
    const alumno = curso!.alumnos[0];
    const evaluacion = curso!.evaluaciones[1];

    let creada: NotaCurso | undefined;
    service.crearNota(alumno.id, evaluacion.id, 6.5).subscribe((valor) => (creada = valor));
    expect(creada?.score).toBe(6.5);
    expect(alumno.notas.some((nota) => nota.id === creada?.id)).toBe(true);

    let editada: NotaCurso | undefined;
    service.editarNota(creada!.id, 5.8).subscribe((valor) => (editada = valor));
    expect(editada?.score).toBe(5.8);

    service.eliminarNota(creada!.id).subscribe();
    expect(alumno.notas.some((nota) => nota.id === creada?.id)).toBe(false);
  });

  it('crea, edita y elimina evaluaciones sobre el set demo', () => {
    const service = servicioDemo();
    let curso: CursoNotas | undefined;
    service.getCursoNotas(3).subscribe((valor) => (curso = valor));

    let creada: EvaluacionNotas | undefined;
    service
      .crearEvaluacion(3, { nombre: 'PRUEBA 2', tipo: 'SUMATIVA', ponderacion: 40 })
      .subscribe((valor) => (creada = valor));
    expect(creada?.id).toBeLessThan(0);
    expect(curso?.evaluaciones.some((item) => item.id === creada?.id)).toBe(true);

    let editada: EvaluacionNotas | undefined;
    service
      .editarEvaluacion(creada!.id, { nombre: 'PRUEBA 2B', tipo: 'FORMATIVA', ponderacion: 0 })
      .subscribe((valor) => (editada = valor));
    expect(editada?.nombre).toBe('PRUEBA 2B');

    const evaluacion = curso!.evaluaciones[0];
    service.eliminarEvaluacion(evaluacion.id).subscribe();
    expect(curso?.evaluaciones.some((item) => item.id === evaluacion.id)).toBe(false);
    expect(curso?.alumnos.every((item) => item.notas.every((nota) => nota.idEvaluacion !== evaluacion.id))).toBe(true);
  });

  it('usa valores por defecto para cursos y entidades desconocidas', () => {
    const service = servicioDemo();
    let curso: CursoNotas | undefined;
    service.getCursoNotas(999).subscribe((valor) => (curso = valor));

    expect(curso?.curso).toBe('Curso 999');
    expect(curso?.alumnos).toEqual([]);

    let nota: NotaCurso | undefined;
    service.crearNota(123456, 99, 4).subscribe((valor) => (nota = valor));
    expect(nota).toEqual({ id: expect.any(Number), idEvaluacion: 99, score: 4 });

    let editada: NotaCurso | undefined;
    service.editarNota(-999, 6).subscribe((valor) => (editada = valor));
    expect(editada).toEqual({ id: -999, idEvaluacion: 0, score: 6 });

    let evaluacion: EvaluacionNotas | undefined;
    service
      .editarEvaluacion(-999, { nombre: 'X', tipo: 'FORMATIVA', ponderacion: 0 })
      .subscribe((valor) => (evaluacion = valor));
    expect(evaluacion).toEqual({ id: -999, nombre: 'X', tipo: 'FORMATIVA', ponderacion: 0 });
  });
});
