import { Component } from '@angular/core';
import { ASIGNATURAS_MOCK } from '../mocks/admin.mock';

@Component({
  selector: 'siga-admin-asignaturas',
  template: `
    <div class="mx-auto flex max-w-4xl flex-col gap-6">
      <h1 class="text-2xl font-semibold text-ink sm:text-3xl">Asignaturas</h1>

      <section class="rounded-2xl bg-panel p-4 shadow-lg sm:p-5">
        <div class="overflow-x-auto rounded-xl border border-dashed border-line">
          <table class="w-full border-collapse text-left text-sm">
            <thead>
              <tr class="bg-panel text-heading">
                <th class="px-4 py-3 font-semibold">Nombre</th>
                <th class="px-4 py-3 font-semibold">Descripción</th>
                <th class="px-4 py-3 font-semibold">Estado</th>
              </tr>
            </thead>
            <tbody>
              @for (asignatura of asignaturas; track asignatura.id) {
                <tr class="text-ink" [class.bg-surface]="$odd" [class.bg-panel]="!$odd">
                  <td class="px-4 py-3">{{ asignatura.nombre }}</td>
                  <td class="px-4 py-3">{{ asignatura.descripcion }}</td>
                  <td class="px-4 py-3">{{ asignatura.activa ? 'Activa' : 'Inactiva' }}</td>
                </tr>
              }
            </tbody>
          </table>
        </div>
      </section>
    </div>
  `,
})
export class AdminAsignaturasComponent {
  protected readonly asignaturas = ASIGNATURAS_MOCK;
}
