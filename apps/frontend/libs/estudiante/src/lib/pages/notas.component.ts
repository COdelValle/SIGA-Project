import { Component, computed, signal } from '@angular/core';
import {
  CONFIG_ACADEMICA_MOCK,
  NotasTablaComponent,
  PERIODO_ACTUAL_MOCK,
} from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-notas',
  imports: [SeccionCardComponent, NotasTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Notas</h1>
        <p class="text-sm text-muted">{{ periodo.anio }} · {{ periodo.curso }}</p>
      </div>

      <div class="flex flex-wrap gap-2">
        @for (semestre of periodo.semestres; track semestre.numero) {
          <button
            type="button"
            (click)="seleccionar(semestre.numero)"
            class="rounded-lg border px-4 py-1.5 text-sm font-semibold transition"
            [class.border-gold]="semestre.numero === semestreSeleccionado()"
            [class.bg-gold/15]="semestre.numero === semestreSeleccionado()"
            [class.text-gold]="semestre.numero === semestreSeleccionado()"
            [class.border-transparent]="semestre.numero !== semestreSeleccionado()"
            [class.text-ink]="semestre.numero !== semestreSeleccionado()"
            [class.hover:text-gold]="semestre.numero !== semestreSeleccionado()"
          >
            Semestre {{ semestre.numero }}
          </button>
        }
      </div>

      <siga-seccion-card [title]="'Semestre ' + semestreSeleccionado()">
        <siga-notas-tabla [asignaturas]="asignaturas()" />
        <p class="mt-3 text-sm text-muted">
          Promedio del semestre:
          <span class="font-semibold text-ink">{{ promedioSemestre().toFixed(1) }}</span>
        </p>
      </siga-seccion-card>

      <p class="text-xs text-muted">{{ leyenda() }}</p>
    </div>
  `,
})
export class EstudianteNotasComponent {
  protected readonly periodo = PERIODO_ACTUAL_MOCK;
  protected readonly semestreSeleccionado = signal<1 | 2>(2);
  protected readonly asignaturas = computed(
    () =>
      this.periodo.semestres.find((semestre) => semestre.numero === this.semestreSeleccionado())
        ?.asignaturas ?? [],
  );
  protected readonly promedioSemestre = computed(
    () =>
      this.periodo.semestres.find((semestre) => semestre.numero === this.semestreSeleccionado())
        ?.promedio ?? 0,
  );
  protected readonly leyenda = computed(() =>
    CONFIG_ACADEMICA_MOCK.modoCalculo === 'PONDERADO'
      ? 'Cálculo: promedio ponderado (cada nota vale según su ponderación).'
      : 'Cálculo: promedio simple (suma de notas dividida por la cantidad).',
  );

  protected seleccionar(numero: 1 | 2): void {
    this.semestreSeleccionado.set(numero);
  }
}
