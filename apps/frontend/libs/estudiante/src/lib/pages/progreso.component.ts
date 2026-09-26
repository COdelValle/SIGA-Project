import { Component, computed, signal } from '@angular/core';
import {
  CONFIG_ACADEMICA_MOCK,
  ESTUDIANTE_ACTUAL_ID,
  NotasTablaComponent,
  PeriodoResumenComponent,
  formatearNota,
  notasDe,
} from '@siga/academico';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-progreso',
  imports: [SeccionCardComponent, NotasTablaComponent, PeriodoResumenComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Progreso Académico</h1>

      <div class="w-full max-w-sm">
        <siga-select
          [options]="opcionesPeriodo"
          [value]="anioSeleccionado()"
          ariaLabel="Seleccionar periodo"
          (valueChange)="cambiarPeriodo($event)"
        />
      </div>

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
          <span class="font-semibold text-ink">{{ formatear(promedioSemestre()) }}</span>
        </p>
      </siga-seccion-card>

      <p class="text-xs text-muted">{{ leyenda() }}</p>
    </div>
  `,
})
export class EstudianteProgresoComponent {
  protected readonly periodos = notasDe(ESTUDIANTE_ACTUAL_ID);
  protected readonly opcionesPeriodo: SelectOption[] = this.periodos.map((periodo) => ({
    value: periodo.anio,
    label: `${periodo.anio} · ${periodo.curso}`,
  }));
  protected readonly anioSeleccionado = signal(this.periodos[0]?.anio ?? 0);
  protected readonly semestreSeleccionado = signal<1 | 2>(2);
  protected readonly periodo = computed(
    () =>
      this.periodos.find((periodo) => periodo.anio === this.anioSeleccionado()) ?? this.periodos[0],
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

  protected cambiarPeriodo(value: string | number): void {
    this.anioSeleccionado.set(Number(value));
  }

  protected seleccionar(numero: 1 | 2): void {
    this.semestreSeleccionado.set(numero);
  }

  protected formatear(valor: number): string {
    return formatearNota(valor);
  }
}
