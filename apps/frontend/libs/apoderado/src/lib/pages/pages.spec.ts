import { Type, signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter, Router } from '@angular/router';
import { AsistenciaDTO, PerfilEstudianteDTO } from '@siga/academico';
import { APP_CONFIG, EstadoRemoto, Me, MeService } from '@siga/core';
import { PUPILOS_MOCK, Pupilo } from '@siga/mocks';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { PupiloBarComponent } from '../components/pupilo-bar.component';
import { ApoderadoStateService } from '../state/apoderado-state.service';
import { ApoderadoAsistenciaHistorialComponent } from './asistencia-historial.component';
import { ApoderadoAsistenciasComponent } from './asistencias.component';
import { ApoderadoHorariosComponent } from './horarios.component';
import { ApoderadoInicioComponent } from './inicio.component';
import { ApoderadoNotasComponent } from './notas.component';
import { ApoderadoProgresoComponent } from './progreso.component';
import { ApoderadoPupilosComponent } from './pupilos.component';
import { ApoderadoSolicitudesComponent } from './solicitudes.component';

const ME: Me = {
  id: 'oid-claudia',
  email: 'claudia.soto@siga.cl',
  displayName: 'Claudia Soto',
  roles: ['APODERADO'],
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
    observacion: null,
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

function crearEstado(useMocks: boolean, opciones: Opciones = {}) {
  return {
    estadoPupilos: signal<EstadoRemoto>(opciones.estadoPupilos ?? 'listo'),
    estadoPerfil: signal<EstadoRemoto>(opciones.estadoPerfil ?? 'listo'),
    estadoAsistencias: signal<EstadoRemoto>(opciones.estadoAsistencias ?? 'listo'),
    pupilos: signal<Pupilo[]>(opciones.pupilos ?? PUPILOS_MOCK),
    pupiloId: signal(1),
    perfil: signal<PerfilEstudianteDTO | null>(crearPerfil()),
    asistencias: signal<AsistenciaDTO[] | null>(ASISTENCIAS),
    seleccionar: vi.fn(),
    reintentar: vi.fn(),
  };
}

interface Opciones {
  useMocks?: boolean;
  params?: Parametros;
  query?: Parametros;
  estadoPupilos?: EstadoRemoto;
  estadoPerfil?: EstadoRemoto;
  estadoAsistencias?: EstadoRemoto;
  pupilos?: Pupilo[];
}

function configurar<T>(componente: Type<T>, opciones: Opciones = {}) {
  const state = crearEstado(opciones.useMocks ?? true, opciones);

  TestBed.configureTestingModule({
    imports: [componente],
    providers: [
      provideRouter([]),
      { provide: APP_CONFIG, useValue: crearConfig(opciones.useMocks ?? true) },
      { provide: MeService, useValue: { getMe: () => of(ME) } },
      { provide: ApoderadoStateService, useValue: state },
      { provide: ActivatedRoute, useValue: crearRoute(opciones.params, opciones.query) },
    ],
  });

  const fixture = TestBed.createComponent(componente);
  fixture.detectChanges();
  return { fixture, state };
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

function boton(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.textContent?.includes(etiqueta)) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

describe('paginas del portal apoderado', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('inicio en modo demo saluda, muestra resumenes y cambia de dia', async () => {
    const { fixture } = configurar(ApoderadoInicioComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Bienvenido(a)');
    expect(texto(fixture)).toContain('Claudia Soto');
    expect(texto(fixture)).toContain('Ver horarios');
    expect(texto(fixture)).toContain('Ver asistencias');
    expect(texto(fixture)).toContain('Ver notas');

    boton(fixture, 'Martes').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Tecnología');
    fixture.destroy();
  });

  it('inicio en modo real usa el perfil y las asistencias del pupilo', async () => {
    const { fixture } = configurar(ApoderadoInicioComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Claudia Soto');
    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('67%');
    expect(texto(fixture)).toContain('6,5');
    fixture.destroy();
  });

  it('inicio muestra error de carga y delega el reintento al estado compartido', async () => {
    const { fixture, state } = configurar(ApoderadoInicioComponent, {
      useMocks: false,
      estadoPerfil: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('notas en modo demo muestra el periodo y cambia de semestre', async () => {
    const { fixture } = configurar(ApoderadoNotasComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8° Básico A');
    expect(texto(fixture)).toContain('Cálculo: promedio ponderado');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Semestre 1');
    fixture.destroy();
  });

  it('notas en modo real arma el periodo desde el perfil del pupilo', async () => {
    const { fixture } = configurar(ApoderadoNotasComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8vo Básico A');
    expect(texto(fixture)).toContain('6,5');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Sin notas registradas.');
    fixture.destroy();
  });

  it('notas muestra error y reintenta', async () => {
    const { fixture, state } = configurar(ApoderadoNotasComponent, {
      useMocks: false,
      estadoPupilos: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('asistencias en modo real muestra la tabla del pupilo', async () => {
    const { fixture } = configurar(ApoderadoAsistenciasComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('67%');
    expect(texto(fixture)).toContain('Ver Historial');
    fixture.destroy();
  });

  it('asistencias en modo demo muestra el resumen del pupilo', async () => {
    const { fixture } = configurar(ApoderadoAsistenciasComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('Historia, Geografía y Ciencias Sociales');
    fixture.destroy();
  });

  it('asistencias muestra error cuando fallan las asistencias', async () => {
    const { fixture, state } = configurar(ApoderadoAsistenciasComponent, {
      useMocks: false,
      estadoAsistencias: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('horarios parte con el dia del query param y navega al cambiar', async () => {
    const { fixture } = configurar(ApoderadoHorariosComponent, {
      useMocks: true,
      query: { dia: 'Jueves' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Día: Jueves');
    expect(texto(fixture)).toContain('Artes Visuales');

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    boton(fixture, 'Lunes').click();
    await esperar(fixture);

    expect(navegar).toHaveBeenCalledWith([], {
      relativeTo: expect.anything(),
      queryParams: { dia: 'Lunes' },
      queryParamsHandling: 'merge',
    });
    expect(texto(fixture)).toContain('Día: Lunes');
    expect(texto(fixture)).toContain('Ciencias Naturales');
    fixture.destroy();
  });

  it('horarios en modo real muestra los bloques del perfil', async () => {
    const { fixture } = configurar(ApoderadoHorariosComponent, {
      useMocks: false,
      query: { dia: 'Lunes' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('08:00 - 08:45');
    expect(texto(fixture)).toContain('Ana Pérez');
    fixture.destroy();
  });

  it('horarios muestra error si falla el perfil', async () => {
    const { fixture, state } = configurar(ApoderadoHorariosComponent, {
      useMocks: false,
      estadoPerfil: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('progreso en modo demo cambia de periodo con el selector', async () => {
    const { fixture } = configurar(ApoderadoProgresoComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8° Básico A');
    expect(texto(fixture)).toContain('En curso');

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

  it('progreso en modo real muestra el periodo del pupilo y su semestre vacio', async () => {
    const { fixture } = configurar(ApoderadoProgresoComponent, { useMocks: false });
    await esperar(fixture);

    expect(texto(fixture)).toContain('2026 · 8vo Básico A');
    expect(texto(fixture)).toContain('6,5');

    boton(fixture, 'Semestre 1').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Sin notas registradas.');
    fixture.destroy();
  });

  it('progreso muestra error y reintenta', async () => {
    const { fixture, state } = configurar(ApoderadoProgresoComponent, {
      useMocks: false,
      estadoPerfil: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('asistencia-historial en modo demo pagina los registros del pupilo', async () => {
    const { fixture } = configurar(ApoderadoAsistenciaHistorialComponent, {
      useMocks: true,
      params: { id: '3' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Matemática');
    const filas = () =>
      (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr').length;
    expect(filas()).toBe(10);
    expect(texto(fixture)).toContain('Mostrando 1–10 de 50');

    boton(fixture, 'Siguiente').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Mostrando 11–20 de 50');
    fixture.destroy();
  });

  it('asistencia-historial en modo real muestra fechas y justificaciones', async () => {
    const { fixture } = configurar(ApoderadoAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '10' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('02/03/2026');
    expect(texto(fixture)).toContain('Sí');
    fixture.destroy();
  });

  it('asistencia-historial con id desconocido muestra el estado vacio', async () => {
    const { fixture } = configurar(ApoderadoAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '999' },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Asignatura');
    expect(texto(fixture)).toContain('Sin registros de asistencia.');
    fixture.destroy();
  });

  it('asistencia-historial muestra error y reintenta', async () => {
    const { fixture, state } = configurar(ApoderadoAsistenciaHistorialComponent, {
      useMocks: false,
      params: { id: '10' },
      estadoAsistencias: 'error',
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los datos');
    boton(fixture, 'Reintentar').click();

    expect(state.reintentar).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('pupilos lista las tarjetas y abre la informacion del pupilo elegido', async () => {
    const { fixture, state } = configurar(ApoderadoPupilosComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Mis pupilos');
    expect(texto(fixture)).toContain('Camila Antonieta Soto Hernández');
    expect(texto(fixture)).toContain('Lilith Fernanda Soto Hernández');

    const router = TestBed.inject(Router);
    const navegar = vi.spyOn(router, 'navigate').mockResolvedValue(true);

    const botones = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
    ) as HTMLButtonElement[];
    botones[1].click();

    expect(state.seleccionar).toHaveBeenCalledWith(2);
    expect(navegar).toHaveBeenCalledWith(['/apoderado/inicio']);
    fixture.destroy();
  });

  it('solicitudes muestra las solicitudes registradas con su estado', async () => {
    const { fixture } = configurar(ApoderadoSolicitudesComponent);
    await esperar(fixture);

    expect(texto(fixture)).toContain('Solicitudes');
    expect(texto(fixture)).toContain('Cambio de teléfono de emergencia');
    expect(texto(fixture)).toContain('Pendiente');
    expect(texto(fixture)).toContain('Justificación de inasistencia');
    expect(texto(fixture)).toContain('Aprobada');
    fixture.destroy();
  });

  it('barra de pupilo muestra el seleccionado y cambia al elegir otro', async () => {
    const { fixture, state } = configurar(PupiloBarComponent, { useMocks: true });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Camila Antonieta Soto Hernández · 8° Básico A');

    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'siga-select button',
    ) as HTMLButtonElement;
    selector.click();
    fixture.detectChanges();

    const opcion = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('li button'),
    ).find((item) => item.textContent?.includes('Lilith')) as HTMLButtonElement;
    opcion.click();

    expect(state.seleccionar).toHaveBeenCalledWith(2);
    fixture.destroy();
  });
});
