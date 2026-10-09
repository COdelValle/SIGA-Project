import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG, RefrescoDatosService } from '@siga/core';
import { PerfilEstudianteDTO } from '../models/perfil.model';
import { PerfilEstudianteService } from './perfil-estudiante.service';

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

const perfil: PerfilEstudianteDTO = {
  id: 5,
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

describe('PerfilEstudianteService (modo real)', () => {
  let service: PerfilEstudianteService;
  let refresco: RefrescoDatosService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(false);
    service = TestBed.inject(PerfilEstudianteService);
    refresco = TestBed.inject(RefrescoDatosService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('comparte la peticion entre suscriptores del mismo perfil', () => {
    const recibidos: (PerfilEstudianteDTO | null)[] = [];
    service.getPerfil(5).subscribe((valor) => recibidos.push(valor));
    service.getPerfil(5).subscribe((valor) => recibidos.push(valor));

    http.expectOne('/api/bff/v1/estudiantes/perfil/5').flush(perfil);

    expect(recibidos).toEqual([perfil, perfil]);
  });

  it('reconsulta el perfil activo al pedir refresco (notificacion nueva)', () => {
    let ultimo: PerfilEstudianteDTO | null = null;
    service.getPerfil(5).subscribe((valor) => (ultimo = valor));
    http.expectOne('/api/bff/v1/estudiantes/perfil/5').flush(perfil);

    refresco.solicitarRefresco();
    const recarga = http.expectOne('/api/bff/v1/estudiantes/perfil/5');
    const actualizado: PerfilEstudianteDTO = {
      ...perfil,
      asignaturas: [
        {
          id: 10,
          idAsignatura: 2,
          name: 'Ciencias Naturales',
          description: '',
          area: 'CIENCIAS',
          caracter: 'OBLIGATORIA',
          calificable: true,
          idDocente: 7,
          docente: 'Alejandro Silva',
          horarios: [],
          evaluaciones: [
            { id: 4, nombre: 'PRUEBA 2', tipo: 'FORMATIVA', ponderacion: 30, nota: 6 },
          ],
        },
      ],
    };
    recarga.flush(actualizado);

    expect(ultimo).toEqual(actualizado);
  });

  it('invalida el cache para volver a consultar', () => {
    service.getPerfil(5).subscribe();
    http.expectOne('/api/bff/v1/estudiantes/perfil/5').flush(perfil);

    service.invalidar(5);
    service.getPerfil(5).subscribe();
    http.expectOne('/api/bff/v1/estudiantes/perfil/5').flush(perfil);
  });

  it('propaga el error del BFF sin caer a mocks', () => {
    let fallo = false;
    service.getPerfilMe().subscribe({ error: () => (fallo = true) });

    http
      .expectOne('/api/bff/v1/estudiantes/perfil/me')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(fallo).toBe(true);
  });
});

describe('PerfilEstudianteService (modo demo)', () => {
  it('no llama al BFF y devuelve null', () => {
    configurar(true);
    const service = TestBed.inject(PerfilEstudianteService);
    const http = TestBed.inject(HttpTestingController);

    let perfilDemo: PerfilEstudianteDTO | null = perfil;
    service.getPerfilMe().subscribe((valor) => (perfilDemo = valor));

    expect(perfilDemo).toBeNull();
    http.expectNone('/api/bff/v1/estudiantes/perfil/me');
  });
});
