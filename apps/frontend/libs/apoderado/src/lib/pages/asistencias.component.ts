import { Component, computed, inject } from '@angular/core';
import { AsistenciaTablaComponent, asistenciaResumenDePerfil } from '@siga/academico';
import { asistenciaResumenDe } from '@siga/mocks';
import { SeccionCardComponent } from '@siga/shared-ui';
import { ApoderadoStateService } from '../state/apoderado-state.service';

@Component({
  selector: 'siga-apoderado-asistencias',
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
export class ApoderadoAsistenciasComponent {
  private readonly state = inject(ApoderadoStateService);

  protected readonly asistencia = computed(() => {
    const perfil = this.state.perfil();
    const asistencias = this.state.asistencias();
    return perfil && asistencias
      ? asistenciaResumenDePerfil(perfil, asistencias)
      : asistenciaResumenDe(this.state.pupiloId());
  });
}
