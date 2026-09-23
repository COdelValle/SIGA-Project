import { Component, signal } from '@angular/core';
import { ASISTENCIA_RESUMEN_MOCK, AsistenciaTablaComponent } from '@siga/academico';
import { SeccionCardComponent } from '@siga/shared-ui';
import { PUPILOS_MOCK } from '../mocks/pupilos.mock';

@Component({
  selector: 'siga-apoderado-asistencias',
  imports: [SeccionCardComponent, AsistenciaTablaComponent],
  template: `
    <div class="mx-auto flex max-w-6xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asistencias</h1>

      <label class="flex w-full max-w-md flex-col gap-1 text-sm text-muted">
        Pupilo
        <select
          [value]="pupiloId()"
          (change)="cambiarPupilo($event)"
          class="rounded-xl border border-line bg-panel px-4 py-3 text-base text-ink focus:outline-none focus-visible:ring-2 focus-visible:ring-brand"
        >
          @for (pupilo of pupilos; track pupilo.id) {
            <option [value]="pupilo.id">{{ pupilo.nombre }} · {{ pupilo.curso }}</option>
          }
        </select>
      </label>

      <siga-seccion-card title="Asistencia">
        <siga-asistencia-tabla [items]="asistencia" />
      </siga-seccion-card>
    </div>
  `,
})
export class ApoderadoAsistenciasComponent {
  protected readonly pupilos = PUPILOS_MOCK;
  protected readonly pupiloId = signal(PUPILOS_MOCK[0]?.id ?? 0);
  protected readonly asistencia = ASISTENCIA_RESUMEN_MOCK;

  protected cambiarPupilo(event: Event): void {
    this.pupiloId.set(Number((event.target as HTMLSelectElement).value));
  }
}
