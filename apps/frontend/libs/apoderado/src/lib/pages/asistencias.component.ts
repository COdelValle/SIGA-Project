import { Component, computed, inject } from '@angular/core';
import { AsistenciaTablaComponent, asistenciaResumenDePerfil } from '@siga/academico';
import { asistenciaResumenDe } from '@siga/mocks';
import { APP_CONFIG } from '@siga/core';
import { SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-asistencias',
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
export class ApoderadoAsistenciasComponent {
  private readonly config = inject(APP_CONFIG);
  private readonly state = inject(ApoderadoStateService);

  protected readonly hayError = computed(
    () =>
      this.state.estadoPupilos() === 'error' ||
      this.state.estadoPerfil() === 'error' ||
      this.state.estadoAsistencias() === 'error',
  );
  protected readonly asistencia = computed(() => {
    if (this.config.useMocks) {
      return asistenciaResumenDe(this.state.pupiloId());
    }
    const perfil = this.state.perfil();
    const asistencias = this.state.asistencias();
    return perfil && asistencias ? asistenciaResumenDePerfil(perfil, asistencias) : [];
  });

  protected reintentar(): void {
    this.state.reintentar();
  }
}
