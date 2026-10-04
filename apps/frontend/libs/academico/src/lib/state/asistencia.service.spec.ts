import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import { AsistenciaDTO } from '../models/perfil.model';
import { AsistenciaService } from './asistencia.service';

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

const asistencia: AsistenciaDTO = {
  id: 9,
  idEstudiante: 1,
  idCursoAsignatura: 5,
  fecha: '2026-10-02',
  estado: 'AUSENTE',
  justificacion: 'PENDIENTE',
  observacion: null,
};

describe('AsistenciaService (modo real)', () => {
  let service: AsistenciaService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(false);
    service = TestBed.inject(AsistenciaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('obtiene las asistencias de una asignatura en una fecha', () => {
    let resultado: AsistenciaDTO[] = [];
    service.getAsistenciasAsignatura(5, '2026-10-02').subscribe((valor) => (resultado = valor));

    const req = http.expectOne(
      (peticion) =>
        peticion.url === '/api/bff/v1/asistencias/asignatura/5' &&
        peticion.params.get('fecha') === '2026-10-02',
    );
    expect(req.request.method).toBe('GET');
    req.flush([asistencia]);

    expect(resultado).toEqual([asistencia]);
  });

  it('invalida el cache del estudiante al registrar una asistencia', () => {
    service.getAsistenciasEstudiante(1).subscribe();
    http.expectOne('/api/bff/v1/asistencias/estudiante/1').flush([asistencia]);

    service.getAsistenciasEstudiante(1).subscribe();
    http.expectNone('/api/bff/v1/asistencias/estudiante/1');

    service
      .registrar({
        idEstudiante: 1,
        idCursoAsignatura: 5,
        fecha: '2026-10-02',
        estado: 'AUSENTE',
      })
      .subscribe();
    http.expectOne('/api/bff/v1/asistencias').flush(asistencia);

    let recargado: AsistenciaDTO[] = [];
    service.getAsistenciasEstudiante(1).subscribe((valor) => (recargado = valor ?? []));
    http.expectOne('/api/bff/v1/asistencias/estudiante/1').flush([asistencia]);

    expect(recargado).toEqual([asistencia]);
  });

  it('propaga el error del BFF sin caer a mocks', () => {
    let fallo = false;
    service.getAsistenciasMe().subscribe({ error: () => (fallo = true) });

    http
      .expectOne('/api/bff/v1/asistencias/estudiante/me')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(fallo).toBe(true);
  });
});

describe('AsistenciaService (modo demo)', () => {
  it('no llama al BFF y devuelve null/vacio', () => {
    configurar(true);
    const service = TestBed.inject(AsistenciaService);
    const http = TestBed.inject(HttpTestingController);

    let asistencias: AsistenciaDTO[] | null = [];
    service.getAsistenciasMe().subscribe((valor) => (asistencias = valor));
    expect(asistencias).toBeNull();

    let asignatura: AsistenciaDTO[] = [];
    service.getAsistenciasAsignatura(5, '2026-10-02').subscribe((valor) => (asignatura = valor));
    expect(asignatura).toEqual([]);

    http.expectNone('/api/bff/v1/asistencias/estudiante/me');
  });
});
