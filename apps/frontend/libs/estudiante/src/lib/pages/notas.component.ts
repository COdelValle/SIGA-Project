import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  AsistenciaService,
  NotasTablaComponent,
  PerfilEstudianteService,
  PeriodoAcademico,
  formatearNota,
  periodoDePerfil,
} from '@siga/academico';
import { CONFIG_ACADEMICA_MOCK, ESTUDIANTE_ACTUAL_ID, periodoActualDe } from '@siga/mocks';
import { APP_CONFIG, recursoRemoto } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { switchMap } from 'rxjs';

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
  selector: 'siga-estudiante-notas',
  imports: [SeccionCardComponent, NotasTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <div class="flex flex-wrap items-end justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Notas</h1>
        @if (!hayError()) {
          <p class="text-sm text-muted">{{ periodo().anio }} · {{ periodo().curso }}</p>
        }
      </div>

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

        <p class="text-xs text-muted">{{ leyenda }}</p>
      }
    </div>
  `,
})
export class EstudianteNotasComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);

  private readonly recarga = signal(0);
  private readonly perfil = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.perfilService.getPerfilMe())),
  );
  private readonly asistencias = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.asistenciaService.getAsistenciasMe())),
  );

  protected readonly hayError = computed(
    () => this.perfil().estado === 'error' || this.asistencias().estado === 'error',
  );
  protected readonly periodo = computed(() => {
    if (this.config.useMocks) {
      return periodoActualDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    return perfil
      ? periodoDePerfil(perfil, CONFIG_ACADEMICA_MOCK, this.asistencias().dato ?? [])
      : PERIODO_VACIO;
  });
  protected readonly semestreSeleccionado = signal<1 | 2>(2);
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
  protected readonly leyenda =
    CONFIG_ACADEMICA_MOCK.modoCalculo === 'PONDERADO'
      ? 'Cálculo: promedio ponderado (cada nota vale según su ponderación).'
      : 'Cálculo: promedio simple (suma de notas dividida por la cantidad).';

  protected seleccionar(numero: 1 | 2): void {
    this.semestreSeleccionado.set(numero);
  }

  protected formatear(valor: number): string {
    return formatearNota(valor);
  }

  protected reintentar(): void {
    this.perfilService.invalidar();
    this.asistenciaService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
