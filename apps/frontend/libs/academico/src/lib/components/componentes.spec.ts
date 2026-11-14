import { EnvironmentProviders, Provider, Type } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { afterEach, describe, expect, it } from 'vitest';
import {
  AsistenciaRegistro,
  AsistenciaResumen,
  HorarioBloque,
  NotaItem,
  PeriodoAcademico,
} from '../models/academico.model';
import { AsistenciaHistorialComponent } from './asistencia-historial.component';
import { AsistenciaListaComponent } from './asistencia-lista.component';
import { AsistenciaTablaComponent } from './asistencia-tabla.component';
import { HorarioResumenComponent } from './horario-resumen.component';
import { HorarioTablaComponent } from './horario-tabla.component';
import { NotasListaComponent } from './notas-lista.component';
import { PeriodoResumenComponent } from './periodo-resumen.component';

const REGISTROS: AsistenciaRegistro[] = Array.from({ length: 12 }, (_, indice) => ({
  fecha: `2026-03-${String(indice + 1).padStart(2, '0')}`,
  tipo: indice % 3 === 0 ? 'Inasistencia' : 'Presente',
  justificado: indice % 3 === 0 ? 'Sí' : 'No aplica',
}));

const ASISTENCIAS: AsistenciaResumen[] = [
  { id: 1, asignatura: 'Matemática', clasesRegistradas: 20, clasesAsistidas: 18, porcentaje: 90 },
  { id: 2, asignatura: 'Historia', clasesRegistradas: 20, clasesAsistidas: 15, porcentaje: 75 },
  { id: 3, asignatura: 'Música', clasesRegistradas: 10, clasesAsistidas: 5, porcentaje: 50 },
];

const BLOQUES: HorarioBloque[] = [
  { hora: '08:00 - 08:45', asignatura: 'Matemática', profesor: 'Ana Pérez', sala: 'Sala 3' },
  { hora: '08:45 - 09:30', asignatura: 'Historia', profesor: 'Luis Rojas', sala: 'Sala 4' },
];

const NOTAS: NotaItem[] = [
  { asignatura: 'Matemática', nota: 6.5 },
  { asignatura: 'Historia', nota: 5.0 },
  { asignatura: 'Música', nota: 3.5 },
];

const PERIODO: PeriodoAcademico = {
  anio: 2026,
  curso: '8vo Básico A',
  estado: 'EN_CURSO',
  semestres: [
    { numero: 1, asignaturas: [], promedio: 0 },
    { numero: 2, asignaturas: [], promedio: 0 },
  ],
  promedioFinal: 6.5,
  asistenciaGeneral: 90,
};

function crear<T>(
  componente: Type<T>,
  inputs: Record<string, unknown> = {},
  providers: (Provider | EnvironmentProviders)[] = [],
): ComponentFixture<T> {
  TestBed.configureTestingModule({ imports: [componente], providers });
  const fixture = TestBed.createComponent(componente);
  for (const [nombre, valor] of Object.entries(inputs)) {
    fixture.componentRef.setInput(nombre, valor);
  }
  fixture.detectChanges();
  return fixture;
}

function raiz(fixture: ComponentFixture<unknown>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

function texto(fixture: ComponentFixture<unknown>): string {
  return raiz(fixture).textContent ?? '';
}

function boton(fixture: ComponentFixture<unknown>, etiqueta: string): HTMLButtonElement {
  const encontrado = Array.from(raiz(fixture).querySelectorAll('button')).find((item) =>
    item.textContent?.includes(etiqueta),
  ) as HTMLButtonElement | undefined;
  if (!encontrado) {
    throw new Error(`No se encontro el boton ${etiqueta}`);
  }
  return encontrado;
}

describe('componentes academicos', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('asistencia-historial pagina los registros y formatea las fechas', () => {
    const fixture = crear(AsistenciaHistorialComponent, { registros: REGISTROS });

    expect(raiz(fixture).querySelectorAll('tbody tr').length).toBe(10);
    expect(texto(fixture)).toContain('01/03/2026');
    expect(texto(fixture)).toContain('Mostrando 1–10 de 12');
    expect(raiz(fixture).querySelectorAll('.text-ok').length).toBeGreaterThan(0);
    expect(raiz(fixture).querySelectorAll('.text-bad').length).toBeGreaterThan(0);

    boton(fixture, 'Siguiente').click();
    fixture.detectChanges();

    expect(raiz(fixture).querySelectorAll('tbody tr').length).toBe(2);
    expect(texto(fixture)).toContain('11/03/2026');
    expect(texto(fixture)).toContain('Mostrando 11–12 de 12');
    fixture.destroy();
  });

  it('asistencia-historial vuelve a la primera pagina al cambiar de asignatura', () => {
    const fixture = crear(AsistenciaHistorialComponent, { registros: REGISTROS });

    boton(fixture, 'Siguiente').click();
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Mostrando 11–12 de 12');

    fixture.componentRef.setInput('registros', REGISTROS.slice(0, 3));
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Mostrando 1–3 de 3');
    fixture.destroy();
  });

  it('asistencia-historial muestra el estado vacio sin paginador', () => {
    const fixture = crear(AsistenciaHistorialComponent, { registros: [] });

    expect(texto(fixture)).toContain('Sin registros de asistencia.');
    expect(raiz(fixture).querySelector('siga-paginador')).toBeNull();
    fixture.destroy();
  });

  it('periodo-resumen muestra en curso y colores por rango de notas y asistencia', () => {
    const fixture = crear(PeriodoResumenComponent, { periodo: PERIODO });

    expect(texto(fixture)).toContain('En curso');
    expect(texto(fixture)).toContain('6,5');
    expect(texto(fixture)).toContain('90%');
    expect(raiz(fixture).querySelectorAll('.text-ok').length).toBe(2);

    fixture.componentRef.setInput('periodo', {
      ...PERIODO,
      estado: 'FINALIZADO',
      promedioFinal: 4.5,
      asistenciaGeneral: 75,
    });
    fixture.detectChanges();

    expect(texto(fixture)).toContain('Finalizado');
    expect(texto(fixture)).toContain('4,5');
    expect(texto(fixture)).toContain('75%');
    expect(raiz(fixture).querySelectorAll('.text-warn').length).toBe(2);

    fixture.componentRef.setInput('periodo', {
      ...PERIODO,
      estado: 'FINALIZADO',
      promedioFinal: 3.5,
      asistenciaGeneral: 50,
    });
    fixture.detectChanges();

    expect(raiz(fixture).querySelectorAll('.text-bad').length).toBe(2);
    fixture.destroy();
  });

  it('asistencia-tabla muestra el resumen, los porcentajes y el enlace al historial', () => {
    const fixture = crear(
      AsistenciaTablaComponent,
      { items: ASISTENCIAS },
      [provideRouter([])],
    );

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('90%');
    expect(texto(fixture)).toContain('75%');
    expect(texto(fixture)).toContain('50%');
    expect(raiz(fixture).querySelectorAll('td.text-ok').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('td.text-warn').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('td.text-bad').length).toBe(1);

    const enlaces = raiz(fixture).querySelectorAll<HTMLAnchorElement>('tbody a');
    expect(enlaces.length).toBe(3);
    expect(enlaces[0].getAttribute('href')).toContain('/1');
    fixture.destroy();
  });

  it('asistencia-tabla muestra el estado vacio', () => {
    const fixture = crear(AsistenciaTablaComponent, { items: [] }, [provideRouter([])]);

    expect(texto(fixture)).toContain('Sin datos de asistencia.');
    fixture.destroy();
  });

  it('asistencia-lista colorea la barra segun el porcentaje y muestra el vacio', () => {
    const fixture = crear(AsistenciaListaComponent, { items: ASISTENCIAS });

    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('90%');
    expect(raiz(fixture).querySelectorAll('.bg-ok').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('.bg-warn').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('.bg-bad').length).toBe(1);

    fixture.componentRef.setInput('items', []);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Sin datos de asistencia.');
    fixture.destroy();
  });

  it('horario-resumen muestra los bloques y el estado vacio', () => {
    const fixture = crear(HorarioResumenComponent, { bloques: BLOQUES });

    expect(texto(fixture)).toContain('08:00 - 08:45');
    expect(texto(fixture)).toContain('Matemática');
    expect(texto(fixture)).toContain('Ana Pérez');
    expect(texto(fixture)).toContain('Sala 3');

    fixture.componentRef.setInput('bloques', []);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Sin bloques para este día.');
    fixture.destroy();
  });

  it('horario-tabla muestra los bloques y el estado vacio', () => {
    const fixture = crear(HorarioTablaComponent, { bloques: BLOQUES });

    expect(raiz(fixture).querySelectorAll('tbody tr').length).toBe(2);
    expect(texto(fixture)).toContain('Historia');
    expect(texto(fixture)).toContain('Luis Rojas');
    expect(texto(fixture)).toContain('Sala 4');

    fixture.componentRef.setInput('bloques', []);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Sin bloques para este día.');
    fixture.destroy();
  });

  it('notas-lista formatea y colorea las notas y muestra el vacio', () => {
    const fixture = crear(NotasListaComponent, { items: NOTAS });

    expect(texto(fixture)).toContain('6,5');
    expect(texto(fixture)).toContain('5,0');
    expect(texto(fixture)).toContain('3,5');
    expect(raiz(fixture).querySelectorAll('.text-ok').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('.text-warn').length).toBe(1);
    expect(raiz(fixture).querySelectorAll('.text-bad').length).toBe(1);

    fixture.componentRef.setInput('items', []);
    fixture.detectChanges();
    expect(texto(fixture)).toContain('Sin notas registradas.');
    fixture.destroy();
  });
});
