import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { afterEach, describe, expect, it, vi } from 'vitest';
import {
  CursoReporte,
  EstudianteReporte,
  IndicadorAsistencia,
  IndicadorRendimiento,
} from '../models/reporte.model';
import { ReporteService } from '../state/reporte.service';
import { ReportesDashboardComponent } from './reportes-dashboard.component';

const curso: CursoReporte = {
  id: 8,
  nombre: 'Matemática',
  curso: '8vo Básico A',
  idClase: 4,
};

const estudiante: EstudianteReporte = {
  id: 41,
  nombre: 'Ana Pérez',
  rut: '11111111-1',
};

const asistencia: IndicadorAsistencia = {
  idEstudiante: 41,
  idCursoAsignatura: 8,
  fecha: '2026-11-09',
  estado: 'AUSENTE',
  porcentajeInasistencia: 45,
  totalRegistros: 20,
  totalInasistencias: 9,
  nivelRiesgo: 'CRITICO',
  actualizadoEn: '2026-11-09T22:00:00',
};

const rendimiento: IndicadorRendimiento = {
  idEstudiante: 41,
  idEvaluacion: 12,
  idCursoAsignatura: 8,
  puntaje: 3.5,
  promedio: 3.5,
  evaluacionesInsuficientes: 2,
  nivelRiesgo: 'CRITICO',
  actualizadoEn: '2026-11-09T22:00:00',
};

function configurar(opciones?: {
  asistencia?: IndicadorAsistencia[];
  rendimiento?: IndicadorRendimiento[];
  error?: boolean;
}) {
  const servicio = {
    getFiltros: vi.fn(() => of({ cursos: [curso] })),
    getEstudiantes: vi.fn(() => of([estudiante])),
    getAsistencia: vi.fn(() =>
      opciones?.error
        ? throwError(() => new Error('sin BFF'))
        : of(opciones?.asistencia ?? [asistencia]),
    ),
    getRendimiento: vi.fn(() =>
      opciones?.error
        ? throwError(() => new Error('sin BFF'))
        : of(opciones?.rendimiento ?? [rendimiento]),
    ),
  };

  TestBed.configureTestingModule({
    imports: [ReportesDashboardComponent],
    providers: [{ provide: ReporteService, useValue: servicio }],
  });

  return { servicio, fixture: TestBed.createComponent(ReportesDashboardComponent) };
}

async function asentar(fixture: ComponentFixture<ReportesDashboardComponent>): Promise<void> {
  await new Promise((resolve) => setTimeout(resolve, 10));
  fixture.detectChanges();
}

describe('ReportesDashboardComponent', () => {
  afterEach(() => {
    vi.useRealTimers();
    TestBed.resetTestingModule();
  });

  it('muestra los indicadores del curso y la última actualización', async () => {
    const { fixture, servicio } = configurar();
    fixture.detectChanges();
    await asentar(fixture);

    expect(servicio.getFiltros).toHaveBeenCalled();
    expect(servicio.getEstudiantes).toHaveBeenCalledWith(8);
    expect(servicio.getAsistencia).toHaveBeenCalled();

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Reportes académicos');
    expect(texto).toContain('8vo Básico A · Matemática');
    expect(texto).toContain('Ana Pérez');
    expect(texto).toContain('CRITICO');
    expect(texto).toContain('45');
    expect(texto).toContain('Última actualización');
  });

  it('muestra estado vacío cuando no hay indicadores', async () => {
    const { fixture } = configurar({ asistencia: [], rendimiento: [] });
    fixture.detectChanges();
    await asentar(fixture);

    expect(fixture.nativeElement.textContent).toContain('No hay indicadores');
  });

  it('muestra error y permite reintentar cuando falla el BFF', async () => {
    const { fixture, servicio } = configurar({ error: true });
    fixture.detectChanges();
    await asentar(fixture);

    expect(fixture.nativeElement.textContent).toContain('No se pudieron cargar los indicadores');
    const boton = Array.from(fixture.nativeElement.querySelectorAll('button')).find((item) =>
      (item as HTMLButtonElement).textContent?.includes('Reintentar'),
    ) as HTMLButtonElement | undefined;
    expect(boton).toBeTruthy();
    boton?.click();
    expect(servicio.getAsistencia).toHaveBeenCalledTimes(2);
  });

  it('vuelve a consultar los indicadores cada 15 segundos', async () => {
    vi.useFakeTimers();
    const { fixture, servicio } = configurar();
    fixture.detectChanges();
    await vi.advanceTimersByTimeAsync(0);
    fixture.detectChanges();

    expect(servicio.getAsistencia).toHaveBeenCalledTimes(1);
    await vi.advanceTimersByTimeAsync(15_000);
    fixture.detectChanges();

    expect(servicio.getAsistencia).toHaveBeenCalledTimes(2);
  });

  it('al cambiar de curso vuelve a cargar estudiantes e indicadores', async () => {
    const { fixture, servicio } = configurar();
    fixture.detectChanges();
    await asentar(fixture);

    const selects = fixture.nativeElement.querySelectorAll('siga-select button');
    (selects[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    const opciones = fixture.nativeElement.querySelectorAll('siga-select li button');
    (opciones[0] as HTMLButtonElement).click();
    fixture.detectChanges();
    await asentar(fixture);

    expect(servicio.getEstudiantes).toHaveBeenCalledWith(8);
    expect(servicio.getAsistencia).toHaveBeenCalled();
  });
});
