import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-docente-home',
  imports: [PortalShellComponent],
  template: `
    <siga-portal-shell portal="Portal Docente">
      <h2 class="text-2xl font-semibold text-slate-900">Panel docente</h2>
      <ul class="mt-4 grid gap-3 sm:grid-cols-2">
        @for (tarea of tareas; track tarea) {
          <li
            class="rounded-lg border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700 shadow-sm"
          >
            {{ tarea }}
          </li>
        }
      </ul>
    </siga-portal-shell>
  `,
})
export class DocenteHomeComponent {
  protected readonly tareas = [
    'Crear eventos y actividades',
    'Agendar evaluaciones',
    'Registrar asistencias',
    'Registrar notas',
  ];
}

export const DOCENTE_ROUTES: Routes = [{ path: '', component: DocenteHomeComponent }];
