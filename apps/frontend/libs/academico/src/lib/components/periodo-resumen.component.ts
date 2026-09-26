import { Component, Input } from '@angular/core';
import { PeriodoAcademico, formatearNota } from '../models/academico.model';

/** Resumen de un periodo academico: anio, curso, estado, promedio y asistencia. */
@Component({
  selector: 'siga-periodo-resumen',
  template: `
    <div class="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
      <article class="rounded-2xl bg-panel p-4 shadow-lg">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Año</p>
        <p class="mt-1 text-2xl font-bold text-heading">{{ periodo.anio }}</p>
      </article>
      <article class="rounded-2xl bg-panel p-4 shadow-lg">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Curso</p>
        <p class="mt-1 text-lg font-semibold text-ink">{{ periodo.curso }}</p>
      </article>
      <article class="rounded-2xl bg-panel p-4 shadow-lg">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Estado</p>
        <p class="mt-1 text-lg font-semibold text-ink">
          {{ periodo.estado === 'EN_CURSO' ? 'En curso' : 'Finalizado' }}
        </p>
      </article>
      <article class="rounded-2xl bg-panel p-4 shadow-lg">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Promedio final</p>
        <p [class]="'mt-1 text-2xl font-bold ' + notaClass(periodo.promedioFinal)">
          {{ formatear(periodo.promedioFinal) }}
        </p>
      </article>
      <article class="rounded-2xl bg-panel p-4 shadow-lg">
        <p class="text-xs font-semibold uppercase tracking-wide text-muted">Asistencia general</p>
        <p [class]="'mt-1 text-2xl font-bold ' + asistenciaClass(periodo.asistenciaGeneral)">
          {{ periodo.asistenciaGeneral }}%
        </p>
      </article>
    </div>
  `,
})
export class PeriodoResumenComponent {
  @Input() periodo!: PeriodoAcademico;

  protected formatear(valor: number): string {
    return formatearNota(valor);
  }

  protected notaClass(valor: number): string {
    if (valor >= 6) {
      return 'text-ok';
    }
    if (valor >= 4) {
      return 'text-warn';
    }
    return 'text-bad';
  }

  protected asistenciaClass(porcentaje: number): string {
    if (porcentaje >= 85) {
      return 'text-ok';
    }
    if (porcentaje >= 70) {
      return 'text-warn';
    }
    return 'text-bad';
  }
}
