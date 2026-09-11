import { Component } from '@angular/core';
import { Routes } from '@angular/router';
import { ResumenAcademicoComponent } from '@siga/academico';
import { PortalShellComponent } from '@siga/shared-ui';

@Component({
  selector: 'siga-estudiante-home',
  imports: [PortalShellComponent, ResumenAcademicoComponent],
  template: `
    <siga-portal-shell portal="Portal Estudiante">
      <h2>Mi informacion academica</h2>
      <p>Aqui veras tus notas, asignaturas y asistencias.</p>
      <siga-resumen-academico />
    </siga-portal-shell>
  `,
})
export class EstudianteHomeComponent {}

export const ESTUDIANTE_ROUTES: Routes = [{ path: '', component: EstudianteHomeComponent }];
