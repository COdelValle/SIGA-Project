import { Component, computed, inject } from '@angular/core';
import { AsistenciaTablaComponent, asistenciaResumenDe } from '@siga/academico';
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

  protected readonly asistencia = computed(() => asistenciaResumenDe(this.state.pupiloId()));
}
