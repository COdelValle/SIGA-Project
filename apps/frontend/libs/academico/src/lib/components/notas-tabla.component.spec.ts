import { ComponentFixture, TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it } from 'vitest';
import { AsignaturaNotas } from '../models/academico.model';
import { NotasTablaComponent } from './notas-tabla.component';

const asignaturas: AsignaturaNotas[] = [
  {
    id: 1,
    asignatura: 'Matemática',
    docente: 'Camila Castro',
    promedio: 6.2,
    notas: [
      { numero: 1, nombre: 'PRUEBA 1', valor: 6.2, ponderacion: 30 },
      { numero: 2, nombre: 'TRABAJO 1', valor: 5.8, ponderacion: 0 },
    ],
  },
  {
    id: 2,
    asignatura: 'Historia',
    docente: 'Augusto Figueroa',
    promedio: 5.5,
    notas: [{ numero: 1, nombre: 'CONTROL 1', valor: 5.5, ponderacion: 0 }],
  },
];

function crear(): ComponentFixture<NotasTablaComponent> {
  TestBed.configureTestingModule({ imports: [NotasTablaComponent] });
  const fixture = TestBed.createComponent(NotasTablaComponent);
  fixture.componentRef.setInput('asignaturas', asignaturas);
  fixture.detectChanges();
  return fixture;
}

function raiz(fixture: ComponentFixture<NotasTablaComponent>): HTMLElement {
  return fixture.nativeElement as HTMLElement;
}

function badgeNota(fixture: ComponentFixture<NotasTablaComponent>, texto: string): HTMLElement {
  const spans = Array.from(
    raiz(fixture).querySelectorAll<HTMLElement>('tbody span[tabindex="0"]'),
  );
  const span = spans.find((item) => item.textContent?.trim() === texto);
  if (!span) {
    throw new Error(`No se encontro la celda de nota ${texto}`);
  }
  return span;
}

function tooltip(fixture: ComponentFixture<NotasTablaComponent>): HTMLElement | null {
  return raiz(fixture).querySelector('[role="tooltip"]');
}

describe('NotasTablaComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('muestra el detalle con nombre, nota y ponderación al pasar el mouse', () => {
    const fixture = crear();
    const badge = badgeNota(fixture, '6,2');

    badge.dispatchEvent(new MouseEvent('pointerenter', { bubbles: true }));
    fixture.detectChanges();

    const detalle = tooltip(fixture);
    expect(detalle?.textContent).toContain('PRUEBA 1');
    expect(detalle?.textContent).toContain('6,2');
    expect(detalle?.textContent).toContain('30%');

    badge.dispatchEvent(new MouseEvent('pointerleave', { bubbles: true }));
    fixture.detectChanges();
    expect(tooltip(fixture)).toBeNull();
  });

  it('alterna el detalle con el toque para pantallas táctiles', () => {
    const fixture = crear();
    const badge = badgeNota(fixture, '6,2');

    badge.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(tooltip(fixture)).not.toBeNull();

    badge.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(tooltip(fixture)).toBeNull();
  });

  it('muestra "No pondera" cuando la nota tiene ponderación 0', () => {
    const fixture = crear();
    const badge = badgeNota(fixture, '5,8');

    badge.dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();

    const detalle = tooltip(fixture);
    expect(detalle?.textContent).toContain('TRABAJO 1');
    expect(detalle?.textContent).toContain('No pondera');
  });

  it('cierra el detalle con Escape', () => {
    const fixture = crear();
    badgeNota(fixture, '6,2').dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(tooltip(fixture)).not.toBeNull();

    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    fixture.detectChanges();
    expect(tooltip(fixture)).toBeNull();
  });

  it('no activa el detalle en celdas sin nota', () => {
    const fixture = crear();
    const vacias = Array.from(raiz(fixture).querySelectorAll<HTMLElement>('tbody span')).filter(
      (span) => span.textContent?.trim() === '—',
    );

    expect(vacias.length).toBe(1);
    expect(vacias[0].getAttribute('tabindex')).toBeNull();
    vacias[0].dispatchEvent(new MouseEvent('pointerenter', { bubbles: true }));
    vacias[0].dispatchEvent(new MouseEvent('click', { bubbles: true }));
    fixture.detectChanges();
    expect(tooltip(fixture)).toBeNull();
  });
});
