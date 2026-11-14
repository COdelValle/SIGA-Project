import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import { CursoDocente } from '@siga/mocks';
import { ClaseDocenteDTO, DocenteDatosService } from './docente-datos.service';

const cursos: CursoDocente[] = [
  {
    id: 3,
    nombre: '8° Básico C',
    nivel: 8,
    seccion: 'C',
    asignatura: 'Ciencias Naturales',
    docenteId: 1,
    sala: 'Sala 3',
    diasClase: ['Martes'],
    alumnos: [],
  },
];

const clases: ClaseDocenteDTO[] = [
  {
    franja: 2,
    cursoId: 3,
    curso: '8° Básico C',
    asignatura: 'Ciencias Naturales',
    sala: 'Sala 3',
    dia: 'miércoles',
    horaInicio: '09:50',
    horaFin: '11:20',
  },
  {
    franja: 1,
    cursoId: 1,
    curso: '8° Básico A',
    asignatura: 'Ciencias Naturales',
    sala: 'Sala 1',
    dia: 'MIERCOLES',
    horaInicio: '08:00',
    horaFin: '08:45',
  },
  {
    franja: 2,
    cursoId: 3,
    curso: '8° Básico C',
    asignatura: 'Ciencias Naturales',
    sala: 'Sala 3',
    dia: 'Miércoles',
    horaInicio: '09:50',
    horaFin: '11:20',
  },
  {
    franja: 1,
    cursoId: 9,
    curso: 'Curso inexistente',
    asignatura: 'Otra',
    sala: 'Sala 9',
    dia: 'Sábado',
    horaInicio: '10:00',
    horaFin: '11:00',
  },
];

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

describe('DocenteDatosService (modo real)', () => {
  let service: DocenteDatosService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(false);
    service = TestBed.inject(DocenteDatosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('obtiene los cursos y comparte la peticion entre suscriptores', () => {
    const recibidos: (CursoDocente[] | null)[] = [];
    service.getCursos().subscribe((valor) => recibidos.push(valor));
    service.getCursos().subscribe((valor) => recibidos.push(valor));

    const req = http.expectOne('/api/bff/v1/docentes/cursos');
    expect(req.request.method).toBe('GET');
    req.flush(cursos);

    expect(recibidos).toEqual([cursos, cursos]);
  });

  it('agrupa el horario por dia, deduplica bloques y ordena por franja', () => {
    let horario: unknown;
    service.getHorario().subscribe((valor) => (horario = valor));

    const req = http.expectOne('/api/bff/v1/docentes/horario');
    expect(req.request.method).toBe('GET');
    req.flush(clases);

    expect(horario).toEqual({
      Lunes: [],
      Martes: [],
      Miércoles: [
        {
          franja: 1,
          cursoId: 1,
          curso: '8° Básico A',
          asignatura: 'Ciencias Naturales',
          sala: 'Sala 1',
        },
        {
          franja: 2,
          cursoId: 3,
          curso: '8° Básico C',
          asignatura: 'Ciencias Naturales',
          sala: 'Sala 3',
        },
      ],
      Jueves: [],
      Viernes: [],
    });
  });

  it('invalida el cache para volver a pedir cursos y horario', () => {
    service.getCursos().subscribe();
    http.expectOne('/api/bff/v1/docentes/cursos').flush(cursos);

    service.getHorario().subscribe();
    http.expectOne('/api/bff/v1/docentes/horario').flush(clases);

    service.invalidar();

    service.getCursos().subscribe();
    http.expectOne('/api/bff/v1/docentes/cursos').flush(cursos);

    service.getHorario().subscribe();
    http.expectOne('/api/bff/v1/docentes/horario').flush(clases);
  });

  it('propaga el error del BFF sin caer a mocks', () => {
    let falloCursos = false;
    service.getCursos().subscribe({ error: () => (falloCursos = true) });
    http
      .expectOne('/api/bff/v1/docentes/cursos')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    let falloHorario = false;
    service.getHorario().subscribe({ error: () => (falloHorario = true) });
    http
      .expectOne('/api/bff/v1/docentes/horario')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(falloCursos).toBe(true);
    expect(falloHorario).toBe(true);
  });
});

describe('DocenteDatosService (modo demo)', () => {
  let service: DocenteDatosService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(true);
    service = TestBed.inject(DocenteDatosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('devuelve null sin llamar al BFF', () => {
    const resultados: unknown[] = [];
    service.getCursos().subscribe((valor) => resultados.push(valor));
    service.getHorario().subscribe((valor) => resultados.push(valor));

    expect(resultados).toEqual([null, null]);
    http.expectNone(() => true);
  });
});
