import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-docente-home',
  imports: [PortalShellComponent],
  template: `
    <siga-portal-shell portal="Portal Docente">
      <h2>Panel docente</h2>
      <ul>
        <li>Crear eventos y actividades</li>
        <li>Agendar evaluaciones</li>
        <li>Registrar asistencias</li>
        <li>Registrar notas</li>
      </ul>
    </siga-portal-shell>
  `,
})
export class DocenteHomeComponent {}

export const DOCENTE_ROUTES: Routes = [{ path: '', component: DocenteHomeComponent }];
