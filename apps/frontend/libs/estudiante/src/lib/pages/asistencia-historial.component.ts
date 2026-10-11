import { Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import {
  AsistenciaHistorialComponent,
  AsistenciaService,
  PerfilEstudianteService,
  asistenciaRegistrosDePerfil,
  asistenciaResumenDePerfil,
} from '@siga/academico';
import { ESTUDIANTE_ACTUAL_ID, asistenciaRegistrosDe, asistenciaResumenDe } from '@siga/mocks';
import { APP_CONFIG, recursoRemoto } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { switchMap } from 'rxjs';

@Component({
  selector: 'siga-estudiante-asistencia-historial',
  imports: [RouterLink, SeccionCardComponent, AsistenciaHistorialComponent],
  template: `
    <div class="mx-auto flex max-w-5xl flex-col gap-6">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Historial de asistencia</h1>
        <a
          routerLink="/estudiante/asistencias"
          class="rounded-lg border border-gold/60 px-4 py-1.5 text-sm font-semibold text-gold transition hover:bg-gold/10"
        >
          Volver
        </a>
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
        <siga-seccion-card [title]="asignatura()">
          <siga-asistencia-historial [registros]="registros()" />
        </siga-seccion-card>
      }
    </div>
  `,
})
export class EstudianteAsistenciaHistorialComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly route = inject(ActivatedRoute);
  private readonly idParam = toSignal(this.route.paramMap, {
    initialValue: this.route.snapshot.paramMap,
  });

  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);

  private readonly recarga = signal(0);
  private readonly perfil = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.perfilService.getPerfilMe())),
  );
  private readonly asistencias = recursoRemoto(
    toObservable(this.recarga).pipe(switchMap(() => this.asistenciaService.getAsistenciasMe())),
  );

  private readonly id = computed(() => Number(this.idParam().get('id') ?? 0));

  protected readonly hayError = computed(
    () => this.perfil().estado === 'error' || this.asistencias().estado === 'error',
  );
  private readonly resumen = computed(() => {
    if (this.config.useMocks) {
      return asistenciaResumenDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    const asistencias = this.asistencias().dato;
    return perfil && asistencias ? asistenciaResumenDePerfil(perfil, asistencias) : [];
  });
  private readonly todosLosRegistros = computed(() => {
    if (this.config.useMocks) {
      return asistenciaRegistrosDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    const asistencias = this.asistencias().dato;
    return perfil && asistencias ? asistenciaRegistrosDePerfil(perfil, asistencias) : {};
  });

  protected readonly asignatura = computed(
    () => this.resumen().find((item) => item.id === this.id())?.asignatura ?? 'Asignatura',
  );
  protected readonly registros = computed(() => this.todosLosRegistros()[this.id()] ?? []);

  protected reintentar(): void {
    this.perfilService.invalidar();
    this.asistenciaService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
