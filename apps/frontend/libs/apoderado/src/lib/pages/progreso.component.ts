import { Component, computed, effect, inject, signal } from '@angular/core';
import {
  NotasTablaComponent,
  PeriodoAcademico,
  PeriodoResumenComponent,
  formatearNota,
  periodoDePerfil,
} from '@siga/academico';
import { CONFIG_ACADEMICA_MOCK, notasDe } from '@siga/mocks';
import { APP_CONFIG } from '@siga/core';
import { SeccionCardComponent, SelectComponent, SelectOption } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

const PERIODO_VACIO: PeriodoAcademico = {
  anio: new Date().getFullYear(),
  curso: '',
  estado: 'EN_CURSO',
  semestres: [
    { numero: 1, asignaturas: [], promedio: 0 },
    { numero: 2, asignaturas: [], promedio: 0 },
  ],
  promedioFinal: 0,
  asistenciaGeneral: 0,
};

@Component({
  selector: 'siga-apoderado-progreso',
  imports: [SeccionCardComponent, NotasTablaComponent, PeriodoResumenComponent, SelectComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Progreso Académico</h1>

      @if (hayError()) {
        <siga-seccion-card title="No se pudieron cargar los datos">
          <div class="flex flex-col items-center gap-3 py-4 text-center">
            <p class="text-sm text-muted">Revisa la conexión con el BFF e inténtalo nuevamente.</p>
            <button
              type="button"
              (click)="reintentar()"
              class="rounded-lg border border-brand px-4 py-2 text-sm font-semibold text-brand transition hover:bg-brand/20"
            >
              Reintentar
            </button>
          </div>
        </siga-seccion-card>
      } @else {
        <div class="w-full max-w-sm">
          <siga-select
            [options]="opcionesPeriodo()"
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
      }
    </div>
  `,
})
export class ApoderadoProgresoComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly state = inject(ApoderadoStateService);

  protected readonly hayError = computed(
    () =>
      this.state.estadoPupilos() === 'error' ||
      this.state.estadoPerfil() === 'error' ||
      this.state.estadoAsistencias() === 'error',
  );
  protected readonly periodos = computed(() => {
    if (this.config.useMocks) {
      return notasDe(this.state.pupiloId());
    }
    const perfil = this.state.perfil();
    return perfil
      ? [periodoDePerfil(perfil, CONFIG_ACADEMICA_MOCK, this.state.asistencias() ?? [])]
      : [];
  });
  protected readonly opcionesPeriodo = computed<SelectOption[]>(() =>
    this.periodos().map((periodo) => ({
      value: periodo.anio,
      label: `${periodo.anio} · ${periodo.curso}`,
    })),
  );
  protected readonly anioSeleccionado = signal(2026);
  protected readonly semestreSeleccionado = signal<1 | 2>(2);
  protected readonly periodo = computed(
    () =>
      this.periodos().find((periodo) => periodo.anio === this.anioSeleccionado()) ??
      this.periodos()[0] ??
      PERIODO_VACIO,
  );

  constructor() {
    effect(() => {
      const periodos = this.periodos();
      if (periodos.length > 0 && !periodos.some((periodo) => periodo.anio === this.anioSeleccionado())) {
        this.anioSeleccionado.set(periodos[0].anio);
      }
    });
  }
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

  protected reintentar(): void {
    this.state.reintentar();
  }
}
