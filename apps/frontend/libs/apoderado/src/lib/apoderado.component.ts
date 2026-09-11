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
      <h2>Selecciona un estudiante</h2>
      <select (change)="seleccionar($event)">
        <option value="">-- Seleccionar --</option>
        @for (e of (me$ | async)?.estudiantesVinculados ?? []; track e.id) {
          <option [value]="e.id">{{ e.nombre }}</option>
        }
      </select>

      <siga-resumen-academico [idEstudiante]="idSeleccionado" />

      <section class="solicitudes">
        <h3>Solicitudes</h3>
        <p>Cambio de telefono de contacto de emergencia y otras gestiones.</p>
      </section>
    </siga-portal-shell>
  `,
  styles: [
    `
      .solicitudes {
        margin-top: 1.5rem;
        border-top: 1px solid #e3e8ef;
        padding-top: 1rem;
      }
    `,
  ],
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
