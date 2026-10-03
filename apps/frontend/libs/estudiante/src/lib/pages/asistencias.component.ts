import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import {
  AsistenciaService,
  AsistenciaTablaComponent,
  PerfilEstudianteService,
  asistenciaResumenDePerfil,
} from '@siga/academico';
import { ESTUDIANTE_ACTUAL_ID, asistenciaResumenDe } from '@siga/mocks';
import { SeccionCardComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-asistencias',
  imports: [SeccionCardComponent, AsistenciaTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asistencias</h1>
      <siga-seccion-card title="Asistencia">
        <siga-asistencia-tabla [items]="asistencia()" />
      </siga-seccion-card>
    </div>
  `,
})
export class EstudianteAsistenciasComponent {
  private readonly perfilService = inject(PerfilEstudianteService);
  private readonly asistenciaService = inject(AsistenciaService);
  private readonly perfil = toSignal(this.perfilService.getPerfilMe(), { initialValue: null });
  private readonly asistencias = toSignal(this.asistenciaService.getAsistenciasMe(), {
    initialValue: null,
  });

  protected readonly asistencia = computed(() => {
    const perfil = this.perfil();
    const asistencias = this.asistencias();
    return perfil && asistencias
      ? asistenciaResumenDePerfil(perfil, asistencias)
      : asistenciaResumenDe(ESTUDIANTE_ACTUAL_ID);
  });
}
