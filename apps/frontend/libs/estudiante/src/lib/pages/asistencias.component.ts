import { Component, computed, inject, signal } from '@angular/core';
import { toObservable } from '@angular/core/rxjs-interop';
import {
  AsistenciaService,
  AsistenciaTablaComponent,
  PerfilEstudianteService,
  asistenciaResumenDePerfil,
} from '@siga/academico';
import { ESTUDIANTE_ACTUAL_ID, asistenciaResumenDe } from '@siga/mocks';
import { APP_CONFIG, recursoRemoto } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { switchMap } from 'rxjs';

@Component({
  selector: 'siga-estudiante-asistencias',
  imports: [SeccionCardComponent, AsistenciaTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asistencias</h1>

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
        <siga-seccion-card title="Asistencia">
          <siga-asistencia-tabla [items]="asistencia()" />
        </siga-seccion-card>
      }
    </div>
  `,
})
export class EstudianteAsistenciasComponent {
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
  protected readonly asistencia = computed(() => {
    if (this.config.useMocks) {
      return asistenciaResumenDe(ESTUDIANTE_ACTUAL_ID);
    }
    const perfil = this.perfil().dato;
    const asistencias = this.asistencias().dato;
    return perfil && asistencias ? asistenciaResumenDePerfil(perfil, asistencias) : [];
  });

  protected reintentar(): void {
    this.perfilService.invalidar();
    this.asistenciaService.invalidar();
    this.recarga.update((valor) => valor + 1);
  }
}
