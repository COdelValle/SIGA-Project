import { AsyncPipe } from '@angular/common';
import { Component, inject } from '@angular/core';
import { Routes } from '@angular/router';
import { ResumenAcademicoComponent } from '@siga/academico';
import { MeService } from '@siga/core';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-apoderado-home',
  imports: [PortalShellComponent, ResumenAcademicoComponent, AsyncPipe],
  template: `
    <siga-portal-shell portal="Portal Apoderado">
      <h2 class="text-2xl font-semibold text-slate-900">Selecciona un estudiante</h2>
      <p class="mt-1 text-sm text-slate-500">Elige un pupilo para ver su informacion academica.</p>

      <select
        class="mt-4 w-full max-w-sm rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm text-slate-800 shadow-sm focus:border-blue-500 focus:ring-2 focus:ring-blue-200 focus:outline-none"
        (change)="seleccionar($event)"
      >
        <option value="">-- Seleccionar --</option>
        @for (e of (me$ | async)?.estudiantesVinculados ?? []; track e.id) {
          <option [value]="e.id">{{ e.nombre }}</option>
        }
      </select>

      <siga-resumen-academico [idEstudiante]="idSeleccionado" />

      <section class="mt-6 border-t border-slate-200 pt-4">
        <h3 class="text-base font-semibold text-slate-900">Solicitudes</h3>
        <p class="mt-1 text-sm text-slate-500">
          Cambio de telefono de contacto de emergencia y otras gestiones.
        </p>
      </section>
    </siga-portal-shell>
  `,
})
export class ApoderadoHomeComponent {
  private readonly meService = inject(MeService);

  readonly me$ = this.meService.getMe();
  idSeleccionado?: number;

  seleccionar(event: Event): void {
    const value = (event.target as HTMLSelectElement).value;
    this.idSeleccionado = value ? Number(value) : undefined;
  }
}

export const APODERADO_ROUTES: Routes = [{ path: '', component: ApoderadoHomeComponent }];
