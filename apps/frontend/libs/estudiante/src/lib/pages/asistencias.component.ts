import { Component } from '@angular/core';
import { ASISTENCIA_RESUMEN_MOCK, AsistenciaTablaComponent } from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-asistencias',
  imports: [SeccionCardComponent, AsistenciaTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asistencias</h1>
      <siga-seccion-card title="Asistencia">
        <siga-asistencia-tabla [items]="asistencia" />
      </siga-seccion-card>
    </div>
  `,
})
export class EstudianteAsistenciasComponent {
  protected readonly asistencia = ASISTENCIA_RESUMEN_MOCK;
}
