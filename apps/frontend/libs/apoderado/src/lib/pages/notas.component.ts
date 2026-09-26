import { Component, computed, inject, signal } from '@angular/core';
import { CONFIG_ACADEMICA_MOCK, NotasTablaComponent, formatearNota, notasDe } from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-notas',
  imports: [SeccionCardComponent, NotasTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Notas</h1>
        <p class="text-sm text-muted">{{ periodo().anio }} · {{ periodo().curso }}</p>
      </div>

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
export class ApoderadoNotasComponent {
  private readonly state = inject(ApoderadoStateService);

  protected readonly semestreSeleccionado = signal<1 | 2>(2);
  protected readonly periodo = computed(() => {
    const periodos = notasDe(this.state.pupiloId());
    return periodos.find((item) => item.estado === 'EN_CURSO') ?? periodos[0];
  });
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
      ? 'Cálculo: promedio ponderado (cada nota vale según su ponderación).'
      : 'Cálculo: promedio simple (suma de notas dividida por la cantidad).',
  );

  protected seleccionar(numero: 1 | 2): void {
    this.semestreSeleccionado.set(numero);
  }

  protected formatear(valor: number): string {
    return formatearNota(valor);
  }
}
