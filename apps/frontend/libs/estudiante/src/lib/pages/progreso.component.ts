import { Component, computed, signal } from '@angular/core';
import {
  CONFIG_ACADEMICA_MOCK,
  HISTORIAL_NOTAS_MOCK,
  NotasTablaComponent,
  PeriodoResumenComponent,
} from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-progreso',
  imports: [SeccionCardComponent, NotasTablaComponent, PeriodoResumenComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Progreso Académico</h1>

      <label class="flex w-full max-w-xs flex-col gap-1 text-sm text-muted">
        Periodo
        <select
          [value]="anioSeleccionado()"
          (change)="cambiarPeriodo($event)"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          @for (periodo of periodos; track periodo.anio) {
            <option [value]="periodo.anio">{{ periodo.anio }} · {{ periodo.curso }}</option>
          }
        </select>
      </label>

      <siga-periodo-resumen [periodo]="periodo()" />

      <div class="flex flex-wrap gap-2">
        @for (semestre of periodo().semestres; track semestre.numero) {
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
export class EstudianteProgresoComponent {
  protected readonly periodos = HISTORIAL_NOTAS_MOCK;
  protected readonly anioSeleccionado = signal(this.periodos[0]?.anio ?? 0);
  protected readonly semestreSeleccionado = signal<1 | 2>(2);
  protected readonly periodo = computed(
    () => this.periodos.find((periodo) => periodo.anio === this.anioSeleccionado()) ?? this.periodos[0],
  );
  protected readonly asignaturas = computed(
    () =>
      this.periodo().semestres.find((semestre) => semestre.numero === this.semestreSeleccionado())
        ?.asignaturas ?? [],
  );
  protected readonly promedioSemestre = computed(
    () =>
      this.periodo().semestres.find((semestre) => semestre.numero === this.semestreSeleccionado())
        ?.promedio ?? 0,
  );
  protected readonly leyenda = computed(() =>
    CONFIG_ACADEMICA_MOCK.modoCalculo === 'PONDERADO'
      ? 'Cálculo: promedio ponderado por la ponderación de cada nota.'
      : 'Cálculo: promedio simple (suma de notas / cantidad).',
  );

  protected cambiarPeriodo(event: Event): void {
    this.anioSeleccionado.set(Number((event.target as HTMLSelectElement).value));
  }

  protected seleccionar(numero: 1 | 2): void {
    this.semestreSeleccionado.set(numero);
  }
}
