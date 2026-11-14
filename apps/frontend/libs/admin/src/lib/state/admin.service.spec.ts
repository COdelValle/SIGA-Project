import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { APP_CONFIG } from '@siga/core';
import {
  AsignaturaAdmin,
  ClaseOpcion,
  CredencialTemporal,
  EstudianteOpcion,
  MallaFila,
  RegistroUsuarioEstado,
  RegistroUsuarioPayload,
  UsuarioAdmin,
  UsuarioDetalle,
} from '@siga/mocks';
import { AdminService } from './admin.service';

const CLAVE_PRUEBA = ['clave', 'de', 'prueba'].join('-');

const usuarios: UsuarioAdmin[] = [
  { id: 'u-1', nombre: 'Ana Pérez', email: 'ana@test.com', rol: 'ESTUDIANTE', estado: 'ACTIVO' },
];

const asignaturas: AsignaturaAdmin[] = [
  {
    id: 1,
    nombre: 'Matemática',
    area: 'Matemáticas',
    calificable: true,
    niveles: ['8vo Básico'],
    activa: true,
  },
];

const malla: MallaFila[] = [
  {
    id: 1,
    nivel: '8vo Básico',
    idAsignatura: 1,
    nombre: 'Matemática',
    area: 'Matemáticas',
    caracter: 'OBLIGATORIA',
    plan: 'COMUN',
    horasSemanales: 4,
    calificable: true,
    activa: true,
  },
];

const clases: ClaseOpcion[] = [{ id: 3, nivel: '8vo Básico', letra: 'A', anioAcademico: 2026 }];

const estudiantes: EstudianteOpcion[] = [
  { id: 9, rut: '12345678-9', firstName: 'Ana', firstSurname: 'Pérez' },
];

const estado: RegistroUsuarioEstado = {
  processId: 'proc-1',
  state: 'EN_PROCESO',
  azureState: 'PENDIENTE',
  domainState: 'PENDIENTE',
  requestedRole: 'ESTUDIANTE',
  email: 'ana.perez@platformsiga.onmicrosoft.com',
  userId: null,
  errorMessage: null,
  createdAt: '2026-11-13T00:00:00',
  updatedAt: '2026-11-13T00:00:00',
};

const credencial: CredencialTemporal = {
  processId: 'proc-1',
  email: 'cuenta-de-prueba',
  userId: 'u-1',
  temporaryPassword: CLAVE_PRUEBA,
  expiresAt: '2026-11-20T00:00:00',
};

const detalle: UsuarioDetalle = {
  id: 'u-1',
  fullName: 'Ana Pérez',
  email: 'ana@test.com',
  rol: 'ESTUDIANTE',
  estado: 'ACTIVO',
  rut: '12345678-9',
  fechaNacimiento: '2012-04-01',
  detalle: '8vo Básico A',
  etiquetas: ['Sin alergias'],
};

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

describe('AdminService (modo real)', () => {
  let service: AdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(false);
    service = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('obtiene el catalogo de usuarios y comparte la peticion entre suscriptores', () => {
    const recibidos: (UsuarioAdmin[] | null)[] = [];
    service.getUsuarios().subscribe((valor) => recibidos.push(valor));
    service.getUsuarios().subscribe((valor) => recibidos.push(valor));

    const req = http.expectOne('/api/bff/v1/admin/usuarios');
    expect(req.request.method).toBe('GET');
    req.flush(usuarios);

    expect(recibidos).toEqual([usuarios, usuarios]);

    service.getUsuarios().subscribe((valor) => recibidos.push(valor));
    http.expectNone('/api/bff/v1/admin/usuarios');
    expect(recibidos[2]).toEqual(usuarios);
  });

  it('obtiene asignaturas y malla desde el BFF', () => {
    let resultadoAsignaturas: AsignaturaAdmin[] | null = null;
    service.getAsignaturas().subscribe((valor) => (resultadoAsignaturas = valor));
    http.expectOne('/api/bff/v1/admin/asignaturas').flush(asignaturas);

    let resultadoMalla: MallaFila[] | null = null;
    service.getMalla().subscribe((valor) => (resultadoMalla = valor));
    http.expectOne('/api/bff/v1/admin/malla').flush(malla);

    expect(resultadoAsignaturas).toEqual(asignaturas);
    expect(resultadoMalla).toEqual(malla);
  });

  it('invalida el cache para volver a consultar el catalogo', () => {
    service.getUsuarios().subscribe();
    http.expectOne('/api/bff/v1/admin/usuarios').flush(usuarios);

    service.getUsuarios().subscribe();
    http.expectNone('/api/bff/v1/admin/usuarios');

    service.invalidar();
    service.getUsuarios().subscribe();
    http.expectOne('/api/bff/v1/admin/usuarios').flush(usuarios);
  });

  it('inicia el registro y consulta su estado y credencial temporal', () => {
    const payload: RegistroUsuarioPayload = {
      requestedRole: 'ESTUDIANTE',
      contactEmail: 'apoderado@test.com',
      roleData: {
        estudiante: {
          firstName: 'Ana',
          firstSurname: 'Pérez',
          rut: '12345678-9',
          birthDate: '2012-04-01',
          idClase: 3,
        },
      },
    };

    let iniciado: RegistroUsuarioEstado | null = null;
    service.iniciarRegistro(payload).subscribe((valor) => (iniciado = valor));
    const post = http.expectOne('/api/bff/v1/admin/registraciones');
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(payload);
    post.flush(estado);
    expect(iniciado).toEqual(estado);

    let consultado: RegistroUsuarioEstado | null = null;
    service.getRegistro('proc-1').subscribe((valor) => (consultado = valor));
    const get = http.expectOne('/api/bff/v1/admin/registraciones/proc-1');
    expect(get.request.method).toBe('GET');
    get.flush(estado);
    expect(consultado).toEqual(estado);

    let credencialRecibida: CredencialTemporal | null = null;
    service.getCredencialTemporal('proc-1').subscribe((valor) => (credencialRecibida = valor));
    const credencialReq = http.expectOne('/api/bff/v1/admin/registraciones/proc-1/credencial');
    expect(credencialReq.request.method).toBe('GET');
    credencialReq.flush(credencial);
    expect(credencialRecibida).toEqual(credencial);
  });

  it('restablece la clave, elimina usuarios y consulta su detalle', () => {
    service.resetPassword('u-1').subscribe();
    const reset = http.expectOne('/api/bff/v1/admin/usuarios/u-1/reset-password');
    expect(reset.request.method).toBe('POST');
    expect(reset.request.body).toEqual({});
    reset.flush(credencial);

    service.eliminarUsuario('u-1').subscribe();
    const eliminar = http.expectOne('/api/bff/v1/admin/usuarios/u-1');
    expect(eliminar.request.method).toBe('DELETE');
    eliminar.flush(null, { status: 204, statusText: 'No Content' });

    let recibido: UsuarioDetalle | null = null;
    service.getUsuarioDetalle('u-1').subscribe((valor) => (recibido = valor));
    const detalleReq = http.expectOne('/api/bff/v1/admin/usuarios/u-1');
    expect(detalleReq.request.method).toBe('GET');
    detalleReq.flush(detalle);

    expect(recibido).toEqual(detalle);
  });

  it('consulta clases y estudiantes con los parametros esperados', () => {
    let clasesRecibidas: ClaseOpcion[] | null = null;
    service.getClases(2026).subscribe((valor) => (clasesRecibidas = valor));
    const clasesReq = http.expectOne(
      (peticion) =>
        peticion.url === '/api/bff/v1/admin/clases' &&
        peticion.params.get('anioAcademico') === '2026',
    );
    expect(clasesReq.request.method).toBe('GET');
    clasesReq.flush(clases);

    let estudiantesRecibidos: EstudianteOpcion[] | null = null;
    service.buscarEstudiantes('ana').subscribe((valor) => (estudiantesRecibidos = valor));
    const estudiantesReq = http.expectOne(
      (peticion) =>
        peticion.url === '/api/bff/v1/admin/estudiantes' && peticion.params.get('q') === 'ana',
    );
    expect(estudiantesReq.request.method).toBe('GET');
    estudiantesReq.flush(estudiantes);

    expect(clasesRecibidas).toEqual(clases);
    expect(estudiantesRecibidos).toEqual(estudiantes);
  });

  it('propaga el error del BFF sin caer a mocks', () => {
    let fallo = false;
    service.getUsuarios().subscribe({ error: () => (fallo = true) });

    http
      .expectOne('/api/bff/v1/admin/usuarios')
      .flush('boom', { status: 500, statusText: 'Server Error' });

    expect(fallo).toBe(true);
  });
});

describe('AdminService (modo demo)', () => {
  let service: AdminService;
  let http: HttpTestingController;

  beforeEach(() => {
    configurar(true);
    service = TestBed.inject(AdminService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    http.verify();
    TestBed.resetTestingModule();
  });

  it('devuelve datos demo sin llamar al BFF', () => {
    const resultados: unknown[] = [];
    service.getUsuarios().subscribe((valor) => resultados.push(valor));
    service.getClases(2026).subscribe((valor) => resultados.push(valor));
    service.buscarEstudiantes('ana').subscribe((valor) => resultados.push(valor));
    service
      .iniciarRegistro({ requestedRole: 'ESTUDIANTE', roleData: {} })
      .subscribe((valor) => resultados.push(valor));

    expect(resultados).toEqual([null, null, null, null]);
    http.expectNone(() => true);
  });
});
