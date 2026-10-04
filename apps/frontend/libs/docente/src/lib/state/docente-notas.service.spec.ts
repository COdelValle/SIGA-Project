import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import { DocenteNotasService } from './docente-notas.service';

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
      idAsignatura: 3,
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
