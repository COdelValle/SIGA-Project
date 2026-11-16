import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CursoDocente } from '@siga/mocks';
import { Observable, Subject, of, throwError } from 'rxjs';
import { signal } from '@angular/core';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DocenteAcademicoService } from '../state/docente-academico.service';
import {
  CursoNotas,
  DocenteNotasService,
  EvaluacionNotas,
  NotaCurso,
} from '../state/docente-notas.service';
import { DocenteRegistrarNotasComponent } from './registrar-notas.component';

const cursoA: CursoDocente = {
  id: 3,
  nombre: '8vo A',
  nivel: 8,
  seccion: 'A',
  asignatura: 'Ciencias Naturales',
  docenteId: 1,
  sala: 'S1',
  diasClase: [],
  alumnos: [
    { id: 1, nombres: 'Ana', firstSurname: 'Pérez', secondSurname: 'Soto' },
    { id: 2, nombres: 'Bruno', firstSurname: 'Díaz', secondSurname: 'Rojas' },
  ],
};

const cursoB: CursoDocente = {
  id: 4,
  nombre: '7mo B',
  nivel: 7,
  seccion: 'B',
  asignatura: 'Física',
  docenteId: 1,
  sala: 'S2',
  diasClase: [],
  alumnos: [{ id: 5, nombres: 'Carla', firstSurname: 'Muñoz', secondSurname: 'Vega' }],
};

function crearCursoNotas(
  curso: CursoDocente,
  evaluaciones: EvaluacionNotas[] = [
    { id: 1, nombre: 'TRABAJO 1', tipo: 'FORMATIVA', ponderacion: 0 },
    { id: 2, nombre: 'PRUEBA 1', tipo: 'SUMATIVA', ponderacion: 60 },
    { id: 3, nombre: 'DIAGNOSTICO', tipo: 'DIAGNOSTICO', ponderacion: 0 },
  ],
): CursoNotas {
  return {
    asignaturaId: curso.id,
    curso: curso.nombre,
    asignatura: curso.asignatura,
    evaluaciones,
    alumnos: curso.alumnos.map((alumno, indice) => ({
      id: alumno.id,
      nombres: alumno.nombres,
      firstSurname: alumno.firstSurname,
      secondSurname: alumno.secondSurname,
      notas:
        indice === 0 && evaluaciones.some((evaluacion) => evaluacion.id === 2)
          ? [{ id: 10, idEvaluacion: 2, score: 5.5 }]
          : [],
    })),
  };
}

const cursoNotasA = crearCursoNotas(cursoA);
const cursoNotasB = crearCursoNotas(cursoB);

interface Opciones {
  cursos?: CursoDocente[];
  estadoCursos?: 'cargando' | 'listo' | 'error';
  getCursoNotas?: (id: number) => Observable<CursoNotas>;
  crearNota?: (idEstudiante: number, idEvaluacion: number, score: number) => Observable<NotaCurso>;
  editarNota?: (id: number, score: number) => Observable<NotaCurso>;
  eliminarNota?: (id: number) => Observable<void>;
  crearEvaluacion?: (
    asignaturaId: number,
    request: { nombre: string; tipo: string; ponderacion: number },
  ) => Observable<EvaluacionNotas>;
  editarEvaluacion?: (
    id: number,
    request: { nombre: string; tipo: string; ponderacion: number },
  ) => Observable<EvaluacionNotas>;
  eliminarEvaluacion?: (id: number) => Observable<void>;
}

function configurar(opciones: Opciones = {}) {
  const academico = {
    cursos: signal<CursoDocente[]>(opciones.cursos ?? [cursoA, cursoB]),
    estadoCursos: signal<'cargando' | 'listo' | 'error'>(opciones.estadoCursos ?? 'listo'),
    recargarDatos: vi.fn(),
  };
  const notasService = {
    getCursoNotas: vi.fn(
      (id: number) => opciones.getCursoNotas?.(id) ?? of(id === 4 ? cursoNotasB : cursoNotasA),
    ),
    crearNota: vi.fn(
      (idEstudiante: number, idEvaluacion: number, score: number) =>
        opciones.crearNota?.(idEstudiante, idEvaluacion, score) ??
        of<NotaCurso>({ id: 55, idEvaluacion, score }),
    ),
    editarNota: vi.fn(
      (id: number, score: number) =>
        opciones.editarNota?.(id, score) ?? of<NotaCurso>({ id, idEvaluacion: 2, score }),
    ),
    eliminarNota: vi.fn((id: number) => opciones.eliminarNota?.(id) ?? of(void 0)),
    crearEvaluacion: vi.fn(
      (asignaturaId: number, request: { nombre: string; tipo: string; ponderacion: number }) =>
        opciones.crearEvaluacion?.(asignaturaId, request) ??
        of<EvaluacionNotas>({ id: 90, ...request } as EvaluacionNotas),
    ),
    editarEvaluacion: vi.fn(
      (id: number, request: { nombre: string; tipo: string; ponderacion: number }) =>
        opciones.editarEvaluacion?.(id, request) ??
        of<EvaluacionNotas>({ id, ...request } as EvaluacionNotas),
    ),
    eliminarEvaluacion: vi.fn(
      (id: number) => opciones.eliminarEvaluacion?.(id) ?? of(void 0),
    ),
  };

  TestBed.configureTestingModule({
    imports: [DocenteRegistrarNotasComponent],
    providers: [
      { provide: DocenteAcademicoService, useValue: academico },
      { provide: DocenteNotasService, useValue: notasService },
    ],
  });

  const fixture = TestBed.createComponent(DocenteRegistrarNotasComponent);
  fixture.detectChanges();
  return { fixture, academico, notasService };
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

function botonExacto(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('button'),
  ).find((item) => item.textContent?.trim() === etiqueta) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

function celdas(fixture: ComponentFixture<unknown>): HTMLInputElement[] {
  return Array.from(
    (fixture.nativeElement as HTMLElement).querySelectorAll('td input[inputmode="decimal"]'),
  ) as HTMLInputElement[];
}

function encabezados(fixture: ComponentFixture<unknown>): {
  nombres: HTMLInputElement[];
  tipos: HTMLSelectElement[];
  ponderaciones: HTMLInputElement[];
} {
  const ths = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('th'));
  return {
    nombres: ths
      .map((th) => th.querySelector('input[type="text"]'))
      .filter((input): input is HTMLInputElement => !!input),
    tipos: ths
      .map((th) => th.querySelector('select'))
      .filter((select): select is HTMLSelectElement => !!select),
    ponderaciones: ths
      .map((th) => th.querySelector('input[type="number"]'))
      .filter((input): input is HTMLInputElement => !!input),
  };
}

function escribir(input: HTMLInputElement, valor: string): void {
  input.value = valor;
  input.dispatchEvent(new Event('input'));
}

function cambiar(select: HTMLSelectElement, valor: string): void {
  select.value = valor;
  select.dispatchEvent(new Event('change'));
}

function cambiarInput(input: HTMLInputElement, valor: string): void {
  input.value = valor;
  input.dispatchEvent(new Event('change'));
}

function salir(input: HTMLInputElement): void {
  input.dispatchEvent(new Event('blur'));
}

function abrirNuevaEvaluacion(fixture: ComponentFixture<unknown>): {
  nombre: HTMLInputElement;
  tipo: HTMLSelectElement;
  ponderacion: HTMLInputElement;
} {
  boton(fixture, 'Agregar evaluación').click();
  fixture.detectChanges();
  const raiz = fixture.nativeElement as HTMLElement;
  return {
    nombre: raiz.querySelector('input[type="text"]') as HTMLInputElement,
    tipo: raiz.querySelector('select') as HTMLSelectElement,
    ponderacion: raiz.querySelector('input[type="number"]') as HTMLInputElement,
  };
}

describe('DocenteRegistrarNotasComponent', () => {
  afterEach(() => {
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('carga el curso, los alumnos, las notas y la ponderación total', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    expect(texto(fixture)).toContain('8vo A · Ciencias Naturales');
    expect(texto(fixture)).toContain('Ana Pérez Soto');
    expect(texto(fixture)).toContain('Bruno Díaz Rojas');
    expect(texto(fixture)).toContain('Ponderación total:');
    expect(texto(fixture)).toContain('60%');
    expect(celdas(fixture).map((celda) => celda.value)).toEqual(['', '5,5', '', '', '', '']);
    fixture.destroy();
  });

  it('deshabilita el % de las evaluaciones no sumativas', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    const { ponderaciones } = encabezados(fixture);
    expect(ponderaciones.length).toBe(3);
    expect(ponderaciones[0].disabled).toBe(true);
    expect(ponderaciones[1].disabled).toBe(false);
    expect(ponderaciones[2].disabled).toBe(true);
    fixture.destroy();
  });

  it('sin cursos muestra el estado vacío', async () => {
    const { fixture } = configurar({ cursos: [] });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Sin cursos');
    expect(texto(fixture)).toContain('No tienes asignaturas asignadas');
    fixture.destroy();
  });

  it('si fallan los cursos muestra el error y reintenta la carga', async () => {
    const { fixture, academico } = configurar({ estadoCursos: 'error' });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar los cursos');
    boton(fixture, 'Reintentar').click();

    expect(academico.recargarDatos).toHaveBeenCalledTimes(1);
    fixture.destroy();
  });

  it('muestra el estado de carga mientras llegan las notas', async () => {
    const pendiente = new Subject<CursoNotas>();
    const { fixture } = configurar({ getCursoNotas: () => pendiente.asObservable() });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Cargando notas…');

    pendiente.next(cursoNotasA);
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Cargando notas…');
    expect(texto(fixture)).toContain('Ana Pérez Soto');
    fixture.destroy();
  });

  it('si fallan las notas muestra el error y recarga con el botón', async () => {
    let intento = 0;
    const { fixture, notasService } = configurar({
      getCursoNotas: () => {
        intento += 1;
        return intento === 1 ? throwError(() => new Error('boom')) : of(cursoNotasA);
      },
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudieron cargar las notas');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(notasService.getCursoNotas).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).toContain('Ana Pérez Soto');
    fixture.destroy();
  });

  it('cambia de curso desde el selector', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    (fixture.nativeElement as HTMLElement)
      .querySelector('siga-select button')
      ?.dispatchEvent(new MouseEvent('click'));
    fixture.detectChanges();

    const opcion = Array.from(
      (fixture.nativeElement as HTMLElement).querySelectorAll('li button'),
    ).find((item) => item.textContent?.includes('7mo B')) as HTMLButtonElement;
    opcion.click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('7mo B · Física');
    expect(texto(fixture)).toContain('Carla Muñoz Vega');
    expect(notasService.getCursoNotas).toHaveBeenCalledWith(4);
    fixture.destroy();
  });

  it('valida una nota fuera de rango y no la guarda', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '9,5');
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Nota entre 1,0 y 7,0');
    expect(celda.className).toContain('border-bad');

    salir(celda);
    await esperar(fixture);

    expect(notasService.editarNota).not.toHaveBeenCalled();
    expect(notasService.crearNota).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('convierte el punto a coma y guarda al salir del campo', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '6.5');
    expect(celda.value).toBe('6,5');

    salir(celda);
    await esperar(fixture);

    expect(notasService.editarNota).toHaveBeenCalledWith(10, 6.5);
    expect(celdas(fixture)[1].value).toBe('6,5');
    fixture.destroy();
  });

  it('guarda la nota tras el debounce sin esperar el blur', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    vi.useFakeTimers();
    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');
    expect(notasService.editarNota).not.toHaveBeenCalled();

    vi.advanceTimersByTime(500);
    fixture.detectChanges();

    expect(notasService.editarNota).toHaveBeenCalledWith(10, 6);
    fixture.destroy();
  });

  it('crea la nota de una celda vacía y elimina la nota al limpiarla', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const celdaVacia = celdas(fixture)[4];
    escribir(celdaVacia, '6,0');
    salir(celdaVacia);
    await esperar(fixture);

    expect(notasService.crearNota).toHaveBeenCalledWith(2, 2, 6);
    expect(celdas(fixture)[4].value).toBe('6,0');

    const celdaConNota = celdas(fixture)[1];
    escribir(celdaConNota, '');
    salir(celdaConNota);
    await esperar(fixture);

    expect(notasService.eliminarNota).toHaveBeenCalledWith(10);
    expect(celdas(fixture)[1].value).toBe('');
    fixture.destroy();
  });

  it('no guarda si la nota no cambió o la celda vacía sigue vacía', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '5,5');
    salir(celda);
    await esperar(fixture);

    escribir(celdas(fixture)[2], '');
    salir(celdas(fixture)[2]);
    await esperar(fixture);

    expect(notasService.editarNota).not.toHaveBeenCalled();
    expect(notasService.crearNota).not.toHaveBeenCalled();
    expect(notasService.eliminarNota).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('muestra el error del BFF al guardar y permite reintentar', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo guardar la nota' },
      status: 500,
      statusText: 'Server Error',
    });
    let intento = 0;
    const { fixture, notasService } = configurar({
      editarNota: (id, score) => {
        intento += 1;
        return intento === 1
          ? throwError(() => error)
          : of<NotaCurso>({ id, idEvaluacion: 2, score });
      },
    });
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');
    salir(celda);
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo guardar la nota');
    boton(fixture, 'Reintentar').click();
    await esperar(fixture);

    expect(notasService.editarNota).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).not.toContain('No se pudo guardar la nota');
    expect(celdas(fixture)[1].value).toBe('6,0');
    fixture.destroy();
  });

  it('ante un conflicto 409 recarga el curso sin mostrar error', async () => {
    const conflicto = new HttpErrorResponse({ status: 409, statusText: 'Conflict' });
    const { fixture, notasService } = configurar({
      editarNota: () => throwError(() => conflicto),
    });
    await esperar(fixture);
    expect(notasService.getCursoNotas).toHaveBeenCalledTimes(1);

    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');
    salir(celda);
    await esperar(fixture);

    expect(notasService.getCursoNotas).toHaveBeenCalledTimes(2);
    expect(texto(fixture)).not.toContain('No se pudo guardar');
    fixture.destroy();
  });

  it('muestra el aviso de guardado y bloquea el selector mientras espera', async () => {
    const pendiente = new Subject<NotaCurso>();
    const { fixture } = configurar({ editarNota: () => pendiente.asObservable() });
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');
    salir(celda);
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Guardando cambios…');
    expect(texto(fixture)).toContain('Guardando…');
    const selector = (fixture.nativeElement as HTMLElement).querySelector(
      'siga-select button',
    ) as HTMLButtonElement;
    expect(selector.disabled).toBe(true);

    (fixture.componentInstance as unknown as { cambiarCurso: (value: number) => void }).cambiarCurso(4);
    expect(texto(fixture)).toContain('8vo A · Ciencias Naturales');

    pendiente.next({ id: 10, idEvaluacion: 2, score: 6 });
    pendiente.complete();
    await esperar(fixture);

    expect(texto(fixture)).not.toContain('Guardando cambios…');
    expect(texto(fixture)).not.toContain('Guardando…');
    fixture.destroy();
  });

  it('marca beforeunload solo mientras hay cambios pendientes', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');

    const conPendientes = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(conPendientes);
    expect(conPendientes.defaultPrevented).toBe(true);

    salir(celda);
    await esperar(fixture);

    const sinPendientes = new Event('beforeunload', { cancelable: true });
    window.dispatchEvent(sinPendientes);
    expect(sinPendientes.defaultPrevented).toBe(false);
    fixture.destroy();
  });

  it('muestra el estado vacío cuando el curso no tiene evaluaciones', async () => {
    const { fixture } = configurar({
      getCursoNotas: () => of(crearCursoNotas(cursoA, [])),
    });
    await esperar(fixture);

    expect(texto(fixture)).toContain('Este curso aún no tiene evaluaciones');
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('table'),
    ).toBeNull();
    fixture.destroy();
  });

  it('valida el nombre y la ponderación al agregar una evaluación', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    expect(form.ponderacion.disabled).toBe(true);

    botonExacto(fixture, 'Agregar').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('El nombre debe tener al menos 2 caracteres.');
    expect(notasService.crearEvaluacion).not.toHaveBeenCalled();

    escribir(form.nombre, 'PRUEBA 2');
    cambiar(form.tipo, 'SUMATIVA');
    fixture.detectChanges();
    expect(form.ponderacion.disabled).toBe(false);

    escribir(form.ponderacion, '0');
    botonExacto(fixture, 'Agregar').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('La ponderación de una SUMATIVA debe ser mayor que 0');

    escribir(form.ponderacion, '150');
    botonExacto(fixture, 'Agregar').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('La ponderación de una SUMATIVA debe ser mayor que 0');

    escribir(form.ponderacion, '50');
    botonExacto(fixture, 'Agregar').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('La suma de ponderaciones no puede superar 100%');
    expect(notasService.crearEvaluacion).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('agrega una evaluación y limpia el formulario', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    escribir(form.nombre, 'PRUEBA 2');
    cambiar(form.tipo, 'SUMATIVA');
    escribir(form.ponderacion, '30');
    botonExacto(fixture, 'Agregar').click();
    await esperar(fixture);

    expect(notasService.crearEvaluacion).toHaveBeenCalledWith(3, {
      nombre: 'PRUEBA 2',
      tipo: 'SUMATIVA',
      ponderacion: 30,
    });
    expect(encabezados(fixture).nombres.map((input) => input.value)).toContain('PRUEBA 2');
    expect(texto(fixture)).toContain('90%');
    expect(texto(fixture)).not.toContain('Cancelar');
    fixture.destroy();
  });

  it('cancela el alta y resetea el formulario', async () => {
    const { fixture } = configurar();
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    escribir(form.nombre, 'BORRADOR');
    cambiar(form.tipo, 'SUMATIVA');
    escribir(form.ponderacion, '20');
    botonExacto(fixture, 'Cancelar').click();
    fixture.detectChanges();

    expect(texto(fixture)).not.toContain('BORRADOR');

    const reabierto = abrirNuevaEvaluacion(fixture);
    expect(reabierto.nombre.value).toBe('');
    expect(reabierto.tipo.value).toBe('FORMATIVA');
    fixture.destroy();
  });

  it('muestra el error del BFF al crear una evaluación', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'La evaluación ya existe' },
      status: 500,
      statusText: 'Server Error',
    });
    const { fixture } = configurar({ crearEvaluacion: () => throwError(() => error) });
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    escribir(form.nombre, 'PRUEBA 2');
    botonExacto(fixture, 'Agregar').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('La evaluación ya existe');
    expect(texto(fixture)).toContain('Cancelar');
    fixture.destroy();
  });

  it('edita el nombre de una evaluación existente', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const { nombres } = encabezados(fixture);
    escribir(nombres[0], 'CONTROL 2');
    salir(nombres[0]);
    await esperar(fixture);

    expect(notasService.editarEvaluacion).toHaveBeenCalledWith(1, {
      nombre: 'CONTROL 2',
      tipo: 'FORMATIVA',
      ponderacion: 0,
    });
    expect(encabezados(fixture).nombres[0].value).toBe('CONTROL 2');
    fixture.destroy();
  });

  it('valida el nombre corto y revierte el valor original', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const { nombres } = encabezados(fixture);
    escribir(nombres[0], 'A');
    salir(nombres[0]);
    await esperar(fixture);

    expect(texto(fixture)).toContain('El nombre debe tener al menos 2 caracteres.');
    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();
    expect(encabezados(fixture).nombres[0].value).toBe('TRABAJO 1');
    fixture.destroy();
  });

  it('revierte el nombre si falla la actualización', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo actualizar la evaluación' },
      status: 500,
      statusText: 'Server Error',
    });
    const { fixture } = configurar({ editarEvaluacion: () => throwError(() => error) });
    await esperar(fixture);

    const { nombres } = encabezados(fixture);
    escribir(nombres[0], 'NUEVO NOMBRE');
    salir(nombres[0]);
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo actualizar la evaluación');
    expect(encabezados(fixture).nombres[0].value).toBe('TRABAJO 1');
    fixture.destroy();
  });

  it('no guarda el nombre si no cambió', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const { nombres } = encabezados(fixture);
    salir(nombres[0]);
    await esperar(fixture);

    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('marca error al cambiar a SUMATIVA sin ponderación y permite asignarla', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const antes = encabezados(fixture);
    cambiar(antes.tipos[0], 'SUMATIVA');
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Asigna una ponderación mayor que 0%');
    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();

    const despues = encabezados(fixture);
    expect(despues.tipos[0].value).toBe('SUMATIVA');
    expect(despues.ponderaciones[0].disabled).toBe(false);

    cambiarInput(despues.ponderaciones[0], '20');
    await esperar(fixture);

    expect(notasService.editarEvaluacion).toHaveBeenCalledWith(1, {
      nombre: 'TRABAJO 1',
      tipo: 'SUMATIVA',
      ponderacion: 20,
    });
    expect(texto(fixture)).toContain('80%');
    fixture.destroy();
  });

  it('cambia una SUMATIVA a FORMATIVA y no repite el mismo tipo', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const cabecera = encabezados(fixture);
    cambiar(cabecera.tipos[1], 'SUMATIVA');
    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();

    cambiar(cabecera.tipos[1], 'FORMATIVA');
    await esperar(fixture);

    expect(notasService.editarEvaluacion).toHaveBeenCalledWith(2, {
      nombre: 'PRUEBA 1',
      tipo: 'FORMATIVA',
      ponderacion: 60,
    });
    expect(texto(fixture)).toContain('0%');
    fixture.destroy();
  });

  it('valida la ponderación al editarla y guarda un valor válido', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const cabecera = encabezados(fixture);
    cambiarInput(cabecera.ponderaciones[1], '0');
    fixture.detectChanges();
    expect(texto(fixture)).toContain('La ponderación de una SUMATIVA debe ser mayor que 0');

    cambiarInput(cabecera.ponderaciones[1], '150');
    fixture.detectChanges();
    expect(texto(fixture)).toContain('La ponderación de una SUMATIVA debe ser mayor que 0');

    cambiarInput(cabecera.ponderaciones[1], '60');
    await esperar(fixture);
    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();

    cambiarInput(cabecera.ponderaciones[1], '70');
    await esperar(fixture);
    expect(notasService.editarEvaluacion).toHaveBeenCalledWith(2, {
      nombre: 'PRUEBA 1',
      tipo: 'SUMATIVA',
      ponderacion: 70,
    });
    expect(texto(fixture)).toContain('70%');
    fixture.destroy();
  });

  it('no permite editar la ponderación de una evaluación no sumativa', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const { ponderaciones } = encabezados(fixture);
    cambiarInput(ponderaciones[0], '25');
    await esperar(fixture);

    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();
    expect(encabezados(fixture).ponderaciones[0].value).toBe('0');
    fixture.destroy();
  });

  it('rechaza una ponderación que haría superar el 100%', async () => {
    const { fixture, notasService } = configurar({
      crearEvaluacion: (_id, request) => of<EvaluacionNotas>({ id: 90, ...request } as EvaluacionNotas),
    });
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    escribir(form.nombre, 'TRABAJO 2');
    cambiar(form.tipo, 'SUMATIVA');
    escribir(form.ponderacion, '40');
    botonExacto(fixture, 'Agregar').click();
    await esperar(fixture);
    expect(texto(fixture)).toContain('100%');

    const { ponderaciones } = encabezados(fixture);
    cambiarInput(ponderaciones[1], '70');
    fixture.detectChanges();

    expect(texto(fixture)).toContain('La suma de ponderaciones no puede superar 100%');
    expect(notasService.editarEvaluacion).not.toHaveBeenCalled();
    fixture.destroy();
  });

  it('elimina una evaluación tras confirmar y descarta sus notas', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const eliminar = (fixture.nativeElement as HTMLElement).querySelector(
      'th button[title="Eliminar evaluación"]',
    ) as HTMLButtonElement;
    eliminar.click();
    await esperar(fixture);

    expect(confirmar).toHaveBeenCalled();
    expect(notasService.eliminarEvaluacion).toHaveBeenCalledWith(1);
    expect(encabezados(fixture).nombres.length).toBe(2);
    confirmar.mockRestore();
    fixture.destroy();
  });

  it('no elimina la evaluación si el usuario cancela', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(false);
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    const eliminar = (fixture.nativeElement as HTMLElement).querySelector(
      'th button[title="Eliminar evaluación"]',
    ) as HTMLButtonElement;
    eliminar.click();
    await esperar(fixture);

    expect(notasService.eliminarEvaluacion).not.toHaveBeenCalled();
    expect(encabezados(fixture).nombres.length).toBe(3);
    confirmar.mockRestore();
    fixture.destroy();
  });

  it('muestra el error del BFF si no se puede eliminar la evaluación', async () => {
    const confirmar = vi.spyOn(window, 'confirm').mockReturnValue(true);
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo eliminar la evaluación.' },
      status: 500,
      statusText: 'Server Error',
    });
    const { fixture } = configurar({ eliminarEvaluacion: () => throwError(() => error) });
    await esperar(fixture);

    const eliminar = (fixture.nativeElement as HTMLElement).querySelector(
      'th button[title="Eliminar evaluación"]',
    ) as HTMLButtonElement;
    eliminar.click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo eliminar la evaluación.');
    expect(encabezados(fixture).nombres.length).toBe(3);
    confirmar.mockRestore();
    fixture.destroy();
  });

  it('reinicia el debounce cuando se escribe otra vez', async () => {
    const { fixture, notasService } = configurar();
    await esperar(fixture);

    vi.useFakeTimers();
    const celda = celdas(fixture)[1];
    escribir(celda, '5,8');
    escribir(celda, '6,0');
    vi.advanceTimersByTime(500);
    fixture.detectChanges();

    expect(notasService.editarNota).toHaveBeenCalledTimes(1);
    expect(notasService.editarNota).toHaveBeenCalledWith(10, 6);
    fixture.destroy();
  });

  it('guarda el valor más reciente cuando llega una respuesta anterior', async () => {
    const primera = new Subject<NotaCurso>();
    let llamadas = 0;
    const { fixture, notasService } = configurar({
      editarNota: (id, score) => {
        llamadas += 1;
        return llamadas === 1
          ? primera.asObservable()
          : of<NotaCurso>({ id, idEvaluacion: 2, score });
      },
    });
    await esperar(fixture);

    const celda = celdas(fixture)[1];
    escribir(celda, '6,0');
    salir(celda);
    fixture.detectChanges();
    escribir(celda, '7,0');
    salir(celda);
    fixture.detectChanges();

    primera.next({ id: 10, idEvaluacion: 2, score: 6 });
    primera.complete();
    await esperar(fixture);

    expect(llamadas).toBe(2);
    expect(notasService.editarNota).toHaveBeenLastCalledWith(10, 7);
    expect(celdas(fixture)[1].value).toBe('7,0');
    fixture.destroy();
  });

  it('usa el mensaje por defecto si el alta falla con un error no HTTP', async () => {
    const { fixture } = configurar({
      crearEvaluacion: () => throwError(() => new Error('boom')),
    });
    await esperar(fixture);

    const form = abrirNuevaEvaluacion(fixture);
    escribir(form.nombre, 'PRUEBA 2');
    botonExacto(fixture, 'Agregar').click();
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo crear la evaluación.');
    fixture.destroy();
  });

  it('revierte el tipo si falla la actualización de la evaluación', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo actualizar la evaluación' },
      status: 500,
      statusText: 'Server Error',
    });
    const { fixture } = configurar({ editarEvaluacion: () => throwError(() => error) });
    await esperar(fixture);

    const cabecera = encabezados(fixture);
    cambiar(cabecera.tipos[1], 'FORMATIVA');
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo actualizar la evaluación');
    expect(encabezados(fixture).tipos[1].value).toBe('SUMATIVA');
    fixture.destroy();
  });

  it('revierte la ponderación si falla la actualización', async () => {
    const error = new HttpErrorResponse({
      error: { message: 'No se pudo actualizar la evaluación' },
      status: 500,
      statusText: 'Server Error',
    });
    const { fixture } = configurar({ editarEvaluacion: () => throwError(() => error) });
    await esperar(fixture);

    cambiarInput(encabezados(fixture).ponderaciones[1], '70');
    await esperar(fixture);

    expect(texto(fixture)).toContain('No se pudo actualizar la evaluación');
    expect(encabezados(fixture).ponderaciones[1].value).toBe('60');
    fixture.destroy();
  });
});
