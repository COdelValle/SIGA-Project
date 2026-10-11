import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { CursoDocente } from '@siga/mocks';
import { of } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { DocenteAcademicoService } from '../state/docente-academico.service';
import { CursoNotas, DocenteNotasService } from '../state/docente-notas.service';
import { DocenteRegistrarNotasComponent } from './registrar-notas.component';

const curso: CursoDocente = {
  id: 3,
  nombre: '8vo A',
  nivel: 8,
  seccion: 'A',
  asignatura: 'Ciencias Naturales',
  docenteId: 1,
  sala: 'S1',
  diasClase: [],
  alumnos: [{ id: 1, nombres: 'Ana', firstSurname: 'Pérez', secondSurname: 'Soto' }],
};

const cursoNotas: CursoNotas = {
  asignaturaId: 3,
  curso: '8vo A',
  asignatura: 'Ciencias Naturales',
  evaluaciones: [
    { id: 1, nombre: 'TRABAJO 1', tipo: 'FORMATIVA', ponderacion: 0 },
    { id: 2, nombre: 'PRUEBA 1', tipo: 'SUMATIVA', ponderacion: 60 },
    { id: 3, nombre: 'DIAGNOSTICO', tipo: 'DIAGNOSTICO', ponderacion: 0 },
  ],
  alumnos: [
    { id: 1, nombres: 'Ana', firstSurname: 'Pérez', secondSurname: 'Soto', notas: [] },
  ],
};

function configurar() {
  const academico = {
    cursos: signal<CursoDocente[]>([curso]),
    estadoCursos: signal<'cargando' | 'listo' | 'error'>('listo'),
  };
  const notasService = {
    getCursoNotas: vi.fn(() => of(cursoNotas)),
    crearNota: vi.fn(() => of({ id: 10, idEvaluacion: 2, score: 6 })),
    editarNota: vi.fn(() => of({ id: 10, idEvaluacion: 2, score: 6 })),
    eliminarNota: vi.fn(() => of(void 0)),
    crearEvaluacion: vi.fn(),
    editarEvaluacion: vi.fn(),
    eliminarEvaluacion: vi.fn(),
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
  return { fixture, notasService };
}

async function esperarCarga(fixture: Awaited<ReturnType<typeof configurar>>['fixture']) {
  await fixture.whenStable();
  fixture.detectChanges();
}

describe('DocenteRegistrarNotasComponent', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('suma solo las ponderaciones SUMATIVA en la ponderación total', async () => {
    const { fixture } = configurar();
    await esperarCarga(fixture);

    expect(fixture.nativeElement.textContent).toContain('Ponderación total:');
    expect(fixture.nativeElement.textContent).toContain('60%');
    fixture.destroy();
  });

  it('deshabilita el % de las evaluaciones no sumativas', async () => {
    const { fixture } = configurar();
    await esperarCarga(fixture);

    const entradas = Array.from(
      fixture.nativeElement.querySelectorAll('th input[type="number"]'),
    ) as HTMLInputElement[];

    expect(entradas.length).toBe(3);
    expect(entradas[0].disabled).toBe(true);
    expect(entradas[1].disabled).toBe(false);
    expect(entradas[2].disabled).toBe(true);
    fixture.destroy();
  });
});
