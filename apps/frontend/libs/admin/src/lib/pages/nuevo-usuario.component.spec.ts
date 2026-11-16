import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import {
  AREAS_ACADEMICAS,
  ClaseOpcion,
  CredencialTemporal,
  EstudianteOpcion,
  PARENTESCOS,
  RegistroUsuarioEstado,
  RegistroUsuarioPayload,
} from '@siga/mocks';
import { Observable, of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { AdminService } from '../state/admin.service';
import { NuevoUsuarioComponent } from './nuevo-usuario.component';

const CLAVE_PRUEBA = ['clave', 'de', 'prueba'].join('-');

const clases: ClaseOpcion[] = [{ id: 3, nivel: '8vo Básico', letra: 'A', anioAcademico: 2026 }];

const estudiantes: EstudianteOpcion[] = [
  { id: 9, rut: '12345678-9', firstName: 'Ana', firstSurname: 'Pérez' },
];

const credencial: CredencialTemporal = {
  processId: 'proc-1',
  email: 'cuenta-de-prueba',
  userId: 'u-1',
  temporaryPassword: CLAVE_PRUEBA,
  expiresAt: '2026-11-20T00:00:00',
};

function crearEstado(overrides: Partial<RegistroUsuarioEstado> = {}): RegistroUsuarioEstado {
  return {
    processId: 'proc-1',
    state: 'COMPLETADO',
    azureState: 'COMPLETADO',
    domainState: 'COMPLETADO',
    requestedRole: 'ESTUDIANTE',
    email: 'ana.perez@platformsiga.onmicrosoft.com',
    userId: 'u-1',
    errorMessage: null,
    createdAt: '2026-11-13T00:00:00',
    updatedAt: '2026-11-13T00:05:00',
    ...overrides,
  };
}

interface Opciones {
  clases?: Observable<ClaseOpcion[] | null>;
  estudiantes?: Observable<EstudianteOpcion[] | null>;
  iniciar?: Observable<RegistroUsuarioEstado | null>;
  registro?: Observable<RegistroUsuarioEstado>;
  credencial?: Observable<CredencialTemporal>;
}

function configurar(opciones: Opciones = {}) {
  const adminService = {
    getClases: vi.fn(() => opciones.clases ?? of<ClaseOpcion[] | null>(clases)),
    buscarEstudiantes: vi.fn(
      () => opciones.estudiantes ?? of<EstudianteOpcion[] | null>(estudiantes),
    ),
    iniciarRegistro: vi.fn(
      () => opciones.iniciar ?? of<RegistroUsuarioEstado | null>(crearEstado()),
    ),
    getRegistro: vi.fn(() => opciones.registro ?? of(crearEstado())),
    getCredencialTemporal: vi.fn(() => opciones.credencial ?? of(credencial)),
  };

  TestBed.configureTestingModule({
    imports: [NuevoUsuarioComponent],
    providers: [{ provide: AdminService, useValue: adminService }],
  });

  const fixture = TestBed.createComponent(NuevoUsuarioComponent);
  fixture.detectChanges();
  return { fixture, adminService };
}

function raiz(fixture: ComponentFixture<NuevoUsuarioComponent>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

function texto(fixture: ComponentFixture<NuevoUsuarioComponent>): string {
  return raiz(fixture).textContent ?? '';
}

function escribir(elemento: HTMLInputElement, valor: string): void {
  elemento.value = valor;
  elemento.dispatchEvent(new Event('input', { bubbles: true }));
}

function seleccionar(elemento: HTMLSelectElement, valor: string): void {
  elemento.value = valor;
  elemento.dispatchEvent(new Event('change', { bubbles: true }));
}

function boton(
  fixture: ComponentFixture<NuevoUsuarioComponent>,
  etiqueta: string,
): HTMLButtonElement {
  const encontrado = Array.from(raiz(fixture).querySelectorAll('button')).find((item) =>
    item.textContent?.includes(etiqueta),
  ) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

function fieldset(fixture: ComponentFixture<NuevoUsuarioComponent>): HTMLElement {
  const encontrado = raiz(fixture).querySelector('fieldset');
  if (!encontrado) {
    throw new Error('No se encontro el fieldset del rol');
  }
  return encontrado as HTMLElement;
}

function cambiarRol(
  fixture: ComponentFixture<NuevoUsuarioComponent>,
  rol: 'ESTUDIANTE' | 'DOCENTE' | 'APODERADO',
): void {
  const select = Array.from(raiz(fixture).querySelectorAll('select')).find(
    (item) => item.options.length === 3,
  ) as HTMLSelectElement;
  seleccionar(select, rol);
  fixture.detectChanges();
}

function enviar(fixture: ComponentFixture<NuevoUsuarioComponent>): void {
  const form = raiz(fixture).querySelector('form') as HTMLFormElement;
  form.dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
  fixture.detectChanges();
}

function completarEstudiante(fixture: ComponentFixture<NuevoUsuarioComponent>): void {
  const seccion = fieldset(fixture);
  const inputs = Array.from(seccion.querySelectorAll('input')) as HTMLInputElement[];
  const [firstName, middleName, firstSurname, secondSurname, rut, birthDate, allergies] = inputs;
  escribir(firstName, '  ana ');
  escribir(middleName, ' belén ');
  escribir(firstSurname, ' pérez ');
  escribir(secondSurname, ' soto ');
  escribir(rut, '12345678-9');
  escribir(birthDate, '2012-04-01');
  escribir(allergies, 'maní, polen , ');
  seleccionar(seccion.querySelector('select') as HTMLSelectElement, '3');
  fixture.detectChanges();
}

function completarCertificado(
  seccion: HTMLElement,
  indice: number,
  nombre: string,
  institucion: string,
  fecha: string,
): void {
  const nombres = seccion.querySelectorAll<HTMLInputElement>('input[placeholder="Nombre *"]');
  const instituciones = seccion.querySelectorAll<HTMLInputElement>(
    'input[placeholder="Institución *"]',
  );
  const fechas = Array.from(seccion.querySelectorAll<HTMLInputElement>('input[type="date"]')).slice(
    1,
  );
  escribir(nombres[indice], nombre);
  escribir(instituciones[indice], institucion);
  escribir(fechas[indice], fecha);
}

function completarDocente(fixture: ComponentFixture<NuevoUsuarioComponent>): void {
  const seccion = fieldset(fixture);
  const inputs = Array.from(seccion.querySelectorAll('input')) as HTMLInputElement[];
  const [firstName, , firstSurname, , rut, fechaContratacion] = inputs;
  escribir(firstName, ' alejandro ');
  escribir(firstSurname, ' silva ');
  escribir(rut, '12345678-9');
  escribir(fechaContratacion, '2020-03-01');
  seleccionar(seccion.querySelector('select') as HTMLSelectElement, AREAS_ACADEMICAS[1]);
  completarCertificado(seccion, 0, 'Magíster en Educación', 'Universidad de Chile', '2019-12-15');
  fixture.detectChanges();
}

function completarApoderado(fixture: ComponentFixture<NuevoUsuarioComponent>): void {
  const seccion = fieldset(fixture);
  const inputs = (Array.from(seccion.querySelectorAll('input')) as HTMLInputElement[]).filter(
    (input) => input.type !== 'search',
  );
  const [firstName, , firstSurname, , rut, telefonos] = inputs;
  escribir(firstName, ' claudia ');
  escribir(firstSurname, ' hernández ');
  escribir(rut, '11111111-1');
  escribir(telefonos, '+56912345678, +56987654321');
  fixture.detectChanges();
}

async function buscarAlumno(
  fixture: ComponentFixture<NuevoUsuarioComponent>,
  valor: string,
): Promise<void> {
  const input = raiz(fixture).querySelector('input[type="search"]') as HTMLInputElement;
  escribir(input, valor);
  fixture.detectChanges();
  await new Promise((resolve) => setTimeout(resolve, 350));
  fixture.detectChanges();
}

describe('NuevoUsuarioComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('carga las clases del anio y muestra el formulario de estudiante', () => {
    const { fixture, adminService } = configurar();

    expect(adminService.getClases).toHaveBeenCalledWith(new Date().getFullYear());
    expect(texto(fixture)).toContain('Datos del estudiante');
    expect(texto(fixture)).toContain('8vo Básico A · 2026');
    expect(boton(fixture, 'Copiar').disabled).toBe(true);

    fixture.destroy();
  });

  it('valida los campos obligatorios antes de enviar el registro', () => {
    const { fixture, adminService } = configurar();

    enviar(fixture);

    expect(adminService.iniciarRegistro).not.toHaveBeenCalled();
    expect(texto(fixture)).toContain('Completa los campos obligatorios del estudiante');

    fixture.destroy();
  });

  it('registra al estudiante, muestra el proceso y entrega la credencial temporal', () => {
    const { fixture, adminService } = configurar();
    let finalizado = 0;
    fixture.componentInstance.finalizado.subscribe(() => (finalizado += 1));

    completarEstudiante(fixture);

    const previsualizaciones = Array.from(
      raiz(fixture).querySelectorAll('input[readonly]'),
    ) as HTMLInputElement[];
    expect(previsualizaciones[0].value).toBe('Ana Belén Pérez Soto');
    expect(previsualizaciones[1].value).toBe('ana.perez@platformsiga.onmicrosoft.com');

    enviar(fixture);

    const payload: RegistroUsuarioPayload = {
      requestedRole: 'ESTUDIANTE',
      contactEmail: null,
      roleData: {
        estudiante: {
          firstName: 'Ana',
          middleName: 'Belén',
          firstSurname: 'Pérez',
          secondSurname: 'Soto',
          rut: '12.345.678-9',
          birthDate: '2012-04-01',
          allergies: ['maní', 'polen'],
          idClase: 3,
        },
      },
    };
    expect(adminService.iniciarRegistro).toHaveBeenCalledWith(payload);
    expect(finalizado).toBe(1);
    expect(adminService.getRegistro).toHaveBeenCalledWith('proc-1');
    expect(texto(fixture)).toContain('proc-1');
    expect(texto(fixture)).toContain('COMPLETADO');

    const estado = Array.from(raiz(fixture).querySelectorAll('p')).find(
      (item) => item.textContent?.trim() === 'COMPLETADO',
    );
    expect(estado?.className).toContain('text-emerald-600');

    boton(fixture, 'Obtener credencial temporal').click();
    fixture.detectChanges();

    expect(adminService.getCredencialTemporal).toHaveBeenCalledWith('proc-1');
    expect(texto(fixture)).toContain(CLAVE_PRUEBA);
    expect(texto(fixture)).toContain('cuenta-de-prueba');

    boton(fixture, 'Entendido').click();
    fixture.detectChanges();
    expect(texto(fixture)).not.toContain(CLAVE_PRUEBA);

    fixture.destroy();
  });

  it('muestra el error del BFF al registrar usando el detalle de campos', () => {
    const { fixture } = configurar({
      iniciar: throwError(
        () =>
          new HttpErrorResponse({
            error: { errors: { rut: 'El RUT ya esta registrado' } },
            status: 400,
            statusText: 'Bad Request',
          }),
      ),
    });
    completarEstudiante(fixture);

    enviar(fixture);

    expect(texto(fixture)).toContain('El RUT ya esta registrado');

    fixture.destroy();
  });

  it('muestra el aviso de modo demo cuando el registro no esta disponible', () => {
    const { fixture } = configurar({ iniciar: of(null) });
    completarEstudiante(fixture);

    enviar(fixture);

    expect(texto(fixture)).toContain('Modo demo: el registro asíncrono no está disponible.');
    expect(texto(fixture)).toContain('Registrar usuario');

    fixture.destroy();
  });

  it('muestra el error al obtener la credencial temporal', () => {
    const { fixture } = configurar({
      credencial: throwError(
        () =>
          new HttpErrorResponse({
            error: { message: 'La credencial ya expiro' },
            status: 410,
            statusText: 'Gone',
          }),
      ),
    });
    completarEstudiante(fixture);
    enviar(fixture);

    boton(fixture, 'Obtener credencial temporal').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('La credencial ya expiro');

    fixture.destroy();
  });

  it('registra al docente descartando certificados incompletos', () => {
    const { fixture, adminService } = configurar();
    cambiarRol(fixture, 'DOCENTE');
    completarDocente(fixture);

    boton(fixture, 'Agregar certificado').click();
    fixture.detectChanges();
    completarCertificado(fieldset(fixture), 1, 'Diplomado en Ciencias', '', '');

    enviar(fixture);

    const payload: RegistroUsuarioPayload = {
      requestedRole: 'DOCENTE',
      contactEmail: null,
      roleData: {
        docente: {
          firstName: 'Alejandro',
          middleName: null,
          firstSurname: 'Silva',
          secondSurname: null,
          rut: '12.345.678-9',
          fechaContratacion: '2020-03-01',
          area: AREAS_ACADEMICAS[1],
          certificados: [
            {
              nombre: 'Magíster en Educación',
              institucionRealizacion: 'Universidad de Chile',
              fechaTitulacion: '2019-12-15',
            },
          ],
        },
      },
    };
    expect(adminService.iniciarRegistro).toHaveBeenCalledWith(payload);
    expect(texto(fixture)).toContain('COMPLETADO');

    fixture.destroy();
  });

  it('permite agregar y quitar certificados manteniendo al menos uno', () => {
    const { fixture } = configurar();
    cambiarRol(fixture, 'DOCENTE');

    expect(fieldset(fixture).querySelectorAll('input[placeholder="Nombre *"]').length).toBe(1);

    boton(fixture, 'Agregar certificado').click();
    fixture.detectChanges();
    expect(fieldset(fixture).querySelectorAll('input[placeholder="Nombre *"]').length).toBe(2);

    const quitar = Array.from(fieldset(fixture).querySelectorAll('button')).filter(
      (item) => item.textContent?.trim() === '×',
    );
    quitar[quitar.length - 1].click();
    fixture.detectChanges();
    expect(fieldset(fixture).querySelectorAll('input[placeholder="Nombre *"]').length).toBe(1);

    fixture.destroy();
  });

  it('busca, vincula y registra al apoderado con sus estudiantes', async () => {
    const { fixture, adminService } = configurar();
    cambiarRol(fixture, 'APODERADO');
    completarApoderado(fixture);

    await buscarAlumno(fixture, 'ana');
    expect(adminService.buscarEstudiantes).toHaveBeenCalledWith('ana');
    boton(fixture, '12.345.678-9 | Ana Pérez').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('12.345.678-9 | Ana Pérez');
    expect(texto(fixture)).not.toContain('Agrega al menos un estudiante.');

    seleccionar(fieldset(fixture).querySelector('select') as HTMLSelectElement, PARENTESCOS[1]);
    fixture.detectChanges();

    await buscarAlumno(fixture, 'ana ');
    boton(fixture, '12.345.678-9 | Ana Pérez').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Ese estudiante ya está vinculado.');

    enviar(fixture);

    const payload: RegistroUsuarioPayload = {
      requestedRole: 'APODERADO',
      contactEmail: null,
      roleData: {
        apoderado: {
          firstName: 'Claudia',
          middleName: null,
          firstSurname: 'Hernández',
          secondSurname: null,
          rut: '11.111.111-1',
          telefonos: ['+56912345678', '+56987654321'],
          estudiantes: [{ idEstudiante: 9, parentesco: PARENTESCOS[1] }],
        },
      },
    };
    expect(adminService.iniciarRegistro).toHaveBeenCalledWith(payload);

    fixture.destroy();
  });

  it('muestra error si falla la busqueda de alumnos', async () => {
    const { fixture } = configurar({
      estudiantes: throwError(() => new Error('boom')),
    });
    cambiarRol(fixture, 'APODERADO');

    await buscarAlumno(fixture, 'ana');

    expect(texto(fixture)).toContain('No se pudo buscar alumnos.');
    expect(texto(fixture)).not.toContain('Buscando…');

    fixture.destroy();
  });

  it('muestra el error al cargar las clases', () => {
    const { fixture } = configurar({
      clases: throwError(() => new Error('boom')),
    });

    expect(texto(fixture)).toContain('No se pudieron cargar las clases.');

    fixture.destroy();
  });

  it('marca el proceso fallido y permite reintentar desde el formulario', () => {
    const fallido = crearEstado({
      state: 'FALLIDO',
      azureState: 'FALLIDO',
      domainState: 'FALLIDO',
      userId: null,
      errorMessage: 'No se pudo crear la cuenta',
    });
    const { fixture } = configurar({
      iniciar: of<RegistroUsuarioEstado | null>(fallido),
      registro: of(fallido),
    });
    completarEstudiante(fixture);

    enviar(fixture);

    expect(texto(fixture)).toContain('FALLIDO');
    expect(texto(fixture)).toContain('No se pudo crear la cuenta');
    const estado = Array.from(raiz(fixture).querySelectorAll('p')).find(
      (item) => item.textContent?.trim() === 'FALLIDO',
    );
    expect(estado?.className).toContain('text-red-600');

    boton(fixture, 'Nuevo intento').click();
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Datos del estudiante');
    expect(texto(fixture)).not.toContain('Proceso');

    fixture.destroy();
  });
});
