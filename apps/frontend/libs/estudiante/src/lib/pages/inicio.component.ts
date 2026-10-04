import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import {
  AsistenciaListaComponent,
  AsistenciaService,
  DIAS_SEMANA,
  DiaSemana,
  HorarioResumenComponent,
  NotasListaComponent,
  PerfilEstudianteService,
  asistenciaResumenDePerfil,
  diaActual,
  horarioDePerfil,
  notasResumenDePerfil,
  resumirBloques,
} from '@siga/academico';
import {
  CONFIG_ACADEMICA_MOCK,
  ESTUDIANTE_ACTUAL_ID,
  asistenciaResumenDe,
  horarioDe,
  notasResumenDe,
} from '@siga/mocks';
import { APP_CONFIG, MeService, recursoRemoto } from '@siga/core';
import { DayTabsComponent, SeccionCardComponent } from '@siga/shared-ui';
import { switchMap } from 'rxjs';

@Component({
  selector: 'siga-estudiante-inicio',
  imports: [
    DayTabsComponent,
    SeccionCardComponent,
    HorarioResumenComponent,
    AsistenciaListaComponent,
    NotasListaComponent,
  ],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">
        Bienvenido(a), <span class="text-heading">{{ nombre() }}</span>
      </h1>

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
        <siga-seccion-card
          title="Horarios"
          actionLabel="Ver horarios"
          actionRoute="/estudiante/horarios"
          [actionQueryParams]="{ dia: dia() }"
        >
          <siga-day-tabs [dias]="dias" [selected]="dia()" (selectedChange)="seleccionarDia($event)" />
          <div class="mt-3">
            <siga-horario-resumen [bloques]="resumen()" />
          </div>
        </siga-seccion-card>

        <div class="grid gap-6 lg:grid-cols-2">
          <siga-seccion-card
            title="Asistencia"
            actionLabel="Ver asistencias"
            actionRoute="/estudiante/asistencias"
          >
            <siga-asistencia-lista [items]="asistencia()" />
          </siga-seccion-card>

          <siga-seccion-card title="Notas" actionLabel="Ver notas" actionRoute="/estudiante/notas">
            <siga-notas-lista [items]="notas()" />
          </siga-seccion-card>
        </div>
      }
    </div>
  `,
})
export class EstudianteInicioComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly meService = inject(MeService);
  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);

  private readonly recarga = signal(0);
  private readonly me = toSignal(this.meService.getMe(), { initialValue: null });
  private readonly perfil = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.perfilService.getPerfilMe())),
  );
  private readonly asistencias = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.asistenciaService.getAsistenciasMe())),
  );

  protected readonly hayError = computed(
    () => this.perfil().estado === 'error' || this.asistencias().estado === 'error',
  );
  protected readonly nombre = computed(() => this.me()?.displayName ?? '');
  protected readonly dias = DIAS_SEMANA;
  protected readonly dia = signal<DiaSemana>(diaActual());
  protected readonly resumen = computed(() => {
    if (this.config.useMocks) {
      return resumirBloques(horarioDe(ESTUDIANTE_ACTUAL_ID)[this.dia()]);
    }
    const perfil = this.perfil().dato;
    return perfil ? resumirBloques(horarioDePerfil(perfil)[this.dia()]) : [];
  });
  protected readonly asistencia = computed(() => {
    if (this.config.useMocks) {
      return asistenciaResumenDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    const asistencias = this.asistencias().dato;
    return perfil && asistencias ? asistenciaResumenDePerfil(perfil, asistencias) : [];
  });
  protected readonly notas = computed(() => {
    if (this.config.useMocks) {
      return notasResumenDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    return perfil ? notasResumenDePerfil(perfil, CONFIG_ACADEMICA_MOCK) : [];
  });

  protected seleccionarDia(dia: string): void {
    this.dia.set(dia as DiaSemana);
  }

  protected reintentar(): void {
    this.perfilService.invalidar();
    this.asistenciaService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
